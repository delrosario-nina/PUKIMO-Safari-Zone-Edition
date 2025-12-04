package evaluator

import lexer.*
import parser.*

/**
 * Handles function definitions, calls, and returns.
 */
class FunctionEvaluator(private val evaluator: Evaluator) {
    fun visitDefineStmt(stmt: DefineStmt): Any? {
        val function = createFunctionObject(stmt)
        evaluator.getEnvironment().define(stmt.name, function)
        return null
    }

    private fun createFunctionObject(stmt: DefineStmt): FunctionObject {
        return FunctionObject(
            name = stmt.name,
            parameters = stmt.paramList,
            body = stmt.block,
            closure = evaluator.getEnvironment()
        )
    }

    fun visitReturnStmt(stmt: ReturnStmt): Any? {
        validateReturnContext(stmt. keyword)
        val value = stmt. value?.accept(evaluator)
        throw ReturnException(value)
    }

    private fun validateReturnContext(token: Token) {
        if (! evaluator.isInUserFunction()) {
            throw evaluator.getErrorHandler().error(
                token,
                "Return statement not allowed outside function",
                ErrorPhase.RUNTIME
            )
        }
    }

    fun visitCallExpr(expr: CallExpr): Any? {
        // Check built-in functions FIRST (before evaluating arguments)
        evaluator. getBuiltinFunctions().evaluate(expr, evaluator)?.let { return it }

        return when (val callee = expr.callee) {
            is PropertyAccessExpr -> evaluateMethodCall(expr)
            is VariableExpr -> {
                // Evaluate arguments
                val arguments = expr.args. map { it.accept(evaluator) }

                // Try constructor
                evaluator.getSafariZoneObjects(). tryCreate(
                    callee. identifier.lexeme,
                    arguments,
                    expr.namedArgs,
                    callee.identifier
                ) ?: run {
                    // Try user function - need to look up in environment
                    try {
                        val function = evaluator.getEnvironment().get(callee.identifier)
                        if (function is FunctionObject) {
                            callUserFunction(function, arguments)
                        } else {
                            throw evaluator.getErrorHandler().error(
                                callee.identifier,
                                "'${callee.identifier.lexeme}' is not a function"
                            )
                        }
                    } catch (e: RuntimeError) {
                        // If not found in environment, re-throw with better message
                        throw evaluator.getErrorHandler().error(
                            callee.identifier,
                            "Undefined function '${callee.identifier.lexeme}'"
                        )
                    }
                }
            }
            else -> {
                val function = callee.accept(evaluator)
                if (function is FunctionObject) {
                    callUserFunction(function, expr.args. map { it.accept(evaluator) })
                } else {
                    throw evaluator.getErrorHandler().error(
                        Evaluator.EOF_TOKEN,
                        "Not a callable function"
                    )
                }
            }
        }
    }
    private fun evaluateMethodCall(expr: CallExpr): Any? {
        val propertyExpr = expr.callee as PropertyAccessExpr
        val obj = propertyExpr.primaryWithSuffixes.accept(evaluator)
        val methodName = propertyExpr.identifier.lexeme
        val arguments = expr.args.map { it.accept(evaluator) }

        if (obj !is SafariZoneObjectInterface) {
            throw evaluator.getErrorHandler().typeError(propertyExpr.identifier, "Cannot call method on non-object type")
        }

        return obj.callMethod(methodName, arguments, evaluator.getErrorHandler(), propertyExpr.identifier)
    }

    private fun callUserFunction(function: FunctionObject, arguments: List<Any?>): Any? {
        validateArgumentCount(function, arguments)
        return executeFunction(function, arguments)
    }

    private fun validateArgumentCount(function: FunctionObject, arguments: List<Any? >) {
        if (arguments. size != function.parameters.size) {
            throw evaluator.getErrorHandler().error(
                function.name,
                "Expected ${function.parameters.size} arguments but got ${arguments.size}"
            )
        }
    }

    private fun executeFunction(function: FunctionObject, arguments: List<Any?>): Any? {
        val functionEnvironment = Environment(
            enclosing = function.closure,
            errorHandler = evaluator.getErrorHandler()
        )
        bindParameters(function. parameters, arguments, functionEnvironment)

        val previous = evaluator.getEnvironment()
        val wasInFunction = evaluator.isInUserFunction()

        try {
            evaluator. setEnvironment(functionEnvironment)
            evaluator.setInUserFunction(true)

            function.body.accept(evaluator)
            return null
        } catch (e: ReturnException) {
            return e.value
        } finally {
            evaluator. setInUserFunction(wasInFunction)
            evaluator. setEnvironment(previous)
        }
    }

    private fun bindParameters(parameters: List<Token>, arguments: List<Any?>, env: Environment) {
        for ((param, arg) in parameters.zip(arguments)) {
            env.define(param, arg)
        }
    }
}