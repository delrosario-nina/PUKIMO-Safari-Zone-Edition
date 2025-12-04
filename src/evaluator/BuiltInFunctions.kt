package evaluator

import lexer.*
import parser.*

/**
 * Handles evaluation of built-in functions.
 */
class BuiltinFunctions(private val errorHandler: EvaluatorErrorHandler) {

    private val functions: Map<String, BuiltinFunction> = mapOf(
        "readString" to BuiltinFunction(
            name = "readString",
            arity = 0,
            implementation = ::evaluateReadString
        ),
        "readInt" to BuiltinFunction(
            name = "readInt",
            arity = 0,
            implementation = ::evaluateReadInt
        ),
        "length" to BuiltinFunction(
            name = "length",
            arity = 1,
            implementation = ::evaluateLength
        ),
        "push" to BuiltinFunction(
            name = "push",
            arity = 2,
            implementation = ::evaluatePush
        ),
        "contains" to BuiltinFunction(
            name = "contains",
            arity = 2,
            implementation = ::evaluateContains
        ),
        "concat" to BuiltinFunction(
            name = "concat",
            arity = 2,
            implementation = ::evaluateConcat
        )
    )

    /**
     * Attempts to evaluate a built-in function call.
     * Returns the result if it's a built-in function, null otherwise.
     */
    fun evaluate(expr: CallExpr, evaluator: Evaluator): Any? {
        if (expr. callee !is VariableExpr) return null

        val name = (expr.callee as VariableExpr).identifier.lexeme
        val token = (expr.callee as VariableExpr).identifier

        val function = functions[name] ?: return null

        if (expr.args.size != function.arity) {
            throw errorHandler.error(
                token,
                "${function.name}() expects ${function.arity} argument(s), got ${expr.args.size}"
            )
        }

        return function.implementation(expr, token, evaluator)
    }

    private fun evaluateReadString(expr: CallExpr, token: Token, evaluator: Evaluator): String {
        print("> ")
        val input = readlnOrNull()
        if (input. isNullOrEmpty()) {
            throw errorHandler.error(token, "Input for readString() cannot be empty", ErrorPhase.RUNTIME)
        }
        return input
    }

    private fun evaluateReadInt(expr: CallExpr, token: Token, evaluator: Evaluator): Int {
        print("> ")
        val input = readlnOrNull()
        if (input.isNullOrEmpty()) {
            throw errorHandler.error(token, "Input for readInt() cannot be empty", ErrorPhase.RUNTIME)
        }
        val intVal = input.toIntOrNull()
        if (intVal == null) {
            throw errorHandler.error(token, "Input '$input' is not a valid integer for readInt()", ErrorPhase. RUNTIME)
        }
        return intVal
    }

    private fun evaluateLength(expr: CallExpr, token: Token, evaluator: Evaluator): Int {
        val arg = expr.args[0].accept(evaluator)
        return when (arg) {
            is String -> arg.length
            is SafariZoneObjectInterface -> arg.getProperty("pokemonCount", errorHandler, token) as Int
            is Collection<*> -> arg.size
            is Array<*> -> arg.size
            else -> throw errorHandler.typeError(
                token,
                "length() only works for strings, arrays, or collections - not ${arg?. javaClass?.simpleName ?: "null"}"
            )
        }
    }

    private fun evaluatePush(expr: CallExpr, token: Token, evaluator: Evaluator): Any? {
        val array = expr.args[0].accept(evaluator)
        val item = expr.args[1].accept(evaluator)

        if (array !is MutableList<*>) {
            throw errorHandler.typeError(token, "First argument to push() must be an array")
        }

        val mutableArray = array as MutableList<Any?>
        mutableArray.add(item)
        return null
    }

    private fun evaluateContains(expr: CallExpr, token: Token, evaluator: Evaluator): Boolean {
        val array = expr.args[0]. accept(evaluator)
        val item = expr.args[1]. accept(evaluator)

        if (array !is List<*>) {
            throw errorHandler.typeError(token, "First argument to contains() must be an array")
        }

        return array.contains(item)
    }

    private fun evaluateConcat(expr: CallExpr, token: Token, evaluator: Evaluator): MutableList<Any?> {
        val array1 = expr.args[0].accept(evaluator)
        val array2 = expr.args[1]. accept(evaluator)

        if (array1 !is List<*> || array2 !is List<*>) {
            throw errorHandler.typeError(token, "concat() requires two arrays")
        }

        return (array1 + array2).toMutableList()
    }

    /**
     * Represents a built-in function definition.
     */
    private data class BuiltinFunction(
        val name: String,
        val arity: Int,
        val implementation: (CallExpr, Token, Evaluator) -> Any?
    )
}