package evaluator

import lexer.*
import parser.*

/**
 * Handles evaluation of expressions (literals, variables, operators, assignments).
 */
class ExpressionEvaluator(private val evaluator: Evaluator) {

    fun visitLiteralExpr(expr: LiteralExpr): Any? = expr.value

    fun visitVariableExpr(expr: VariableExpr): Any? =
        evaluator.getEnvironment().get(expr.identifier)

    fun visitUnaryExpr(expr: UnaryExpr): Any? {
        val operand = expr. right.accept(evaluator)
        return evaluateUnaryOperation(expr.operator, operand)
    }

    private fun evaluateUnaryOperation(operator: Token, operand: Any?): Any?  {
        return when (operator. type) {
            TokenType. MINUS -> negateNumber(operator, operand)
            TokenType.NOT -> !evaluator.isTruthy(operand)
            else -> throw evaluator.getErrorHandler().error(
                operator,
                "Unknown unary operator '${operator.lexeme}'",
                ErrorPhase.RUNTIME
            )
        }
    }

    private fun negateNumber(operator: Token, operand: Any?): Int {
        if (operand !is Int) {
            throw evaluator.getErrorHandler().typeError(operator, "Operand must be a number")
        }
        return -operand
    }

    fun visitBinaryExpr(expr: BinaryExpr): Any? {
        if (expr.operator.type == TokenType.AND) {
            return evaluateAndOperator(expr)
        }
        if (expr.operator.type == TokenType.OR) {
            return evaluateOrOperator(expr)
        }

        val left = expr.left. accept(evaluator)
        val right = expr.right.accept(evaluator)
        return evaluator.getArithmeticEvaluator().evaluate(left, expr.operator, right)
    }

    private fun evaluateAndOperator(expr: BinaryExpr): Boolean {
        val left = expr. left.accept(evaluator)
        if (!evaluator.isTruthy(left)) return false
        return evaluator.isTruthy(expr.right. accept(evaluator))
    }

    private fun evaluateOrOperator(expr: BinaryExpr): Boolean {
        val left = expr.left.accept(evaluator)
        if (evaluator. isTruthy(left)) return true
        return evaluator.isTruthy(expr.right. accept(evaluator))
    }

    fun visitAssignExpr(expr: AssignExpr): Any? {
        val value = expr.value.accept(evaluator)
        performAssignment(expr. target, value, expr.equals)
        return value
    }

    private fun performAssignment(target: Expr, value: Any?, token: Token) {
        when (target) {
            is VariableExpr -> evaluator.getEnvironment().assign(target.identifier, value)
            is PropertyAccessExpr -> assignToProperty(target, value, token)
            is ArrayAccessExpr -> assignToArrayElement(target, value, token)
            else -> throw evaluator.getErrorHandler().error(token, "Invalid assignment target")
        }
    }

    private fun assignToProperty(target: PropertyAccessExpr, value: Any?, token: Token) {
        val obj = target.primaryWithSuffixes.accept(evaluator)
        if (obj !is SafariZoneObjectInterface) {
            throw evaluator.getErrorHandler().typeError(token, "Cannot set property on non-object type")
        }
        obj.setProperty(target.identifier.lexeme, value, evaluator.getErrorHandler(), target.identifier)
    }

    private fun assignToArrayElement(target: ArrayAccessExpr, value: Any?, token: Token) {
        val array = target.array. accept(evaluator)
        val index = target.index.accept(evaluator)
        val arrayStringEval = ArrayStringEvaluator(evaluator)
        arrayStringEval.performArrayAssignment(array, index, value, token)
    }

    fun visitPropertyAccessExpr(expr: PropertyAccessExpr): Any? {
        val obj = expr.primaryWithSuffixes.accept(evaluator)

        if (obj !is SafariZoneObjectInterface) {
            throw evaluator.getErrorHandler(). typeError(
                expr.identifier,
                "Cannot access property '${expr.identifier.lexeme}' on non-object type"
            )
        }

        return obj.getProperty(expr.identifier.lexeme, evaluator.getErrorHandler(), expr.identifier)
    }
}