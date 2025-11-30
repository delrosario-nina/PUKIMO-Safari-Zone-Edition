package evaluator

import lexer.Token
import lexer.TokenType

/**
 * Handles arithmetic and comparison operations.
 * Centralizes type checking and coercion logic for binary operations.
 */
class ArithmeticEvaluator(private val errorHandler: EvaluatorErrorHandler) {

    /**
     * Evaluates a binary expression based on operator type.
     */
    fun evaluate(left: Any?, operator: Token, right: Any?): Any? {
        return when (operator.type) {
            // Arithmetic
            TokenType.PLUS -> evaluatePlus(left, right, operator)
            TokenType.MINUS -> evaluateMinus(left, right, operator)
            TokenType.MULTIPLY -> evaluateMultiply(left, right, operator)
            TokenType.DIVIDE -> evaluateDivide(left, right, operator)
            TokenType.MODULO -> evaluateModulo(left, right, operator)

            // Comparison
            TokenType.LESS_THAN -> evaluateLessThan(left, right, operator)
            TokenType.LESS_EQUAL -> evaluateLessEqual(left, right, operator)
            TokenType.GREATER_THAN -> evaluateGreaterThan(left, right, operator)
            TokenType.GREATER_EQUAL -> evaluateGreaterEqual(left, right, operator)
            TokenType.EQUAL_EQUAL -> evaluateEqual(left, right)
            TokenType.NOT_EQUAL -> !evaluateEqual(left, right)

            // Logical
            TokenType.AND -> {
                if (!isTruthy(left)) return false  // Short-circuit: don't evaluate right
                return isTruthy(right)
            }

            TokenType.OR -> {
                if (isTruthy(left)) return true    // Short-circuit: don't evaluate right
                return isTruthy(right)
            }

            else -> throw errorHandler.error(operator, "Unknown binary operator: ${operator.lexeme}")
        }
    }

    // ==================== Arithmetic Operations ====================

    private fun evaluatePlus(left: Any?, right: Any?, operator: Token): Any? {
        return when {
            // Numeric addition
            left is Int && right is Int -> left + right

            // String concatenation
            left is String -> left + stringify(right)
            right is String -> stringify(left) + right

            else -> throw errorHandler.typeError(
                operator,
                "Operands must be two numbers or at least one string. Got ${typeName(left)} + ${typeName(right)}"
            )
        }
    }

    private fun evaluateMinus(left: Any?, right: Any?, operator: Token): Any? {
        return when {
            left is Int && right is Int -> left - right
            else -> throw errorHandler.typeError(
                operator,
                "Operands must be numbers. Got ${typeName(left)} - ${typeName(right)}"
            )
        }
    }

    private fun evaluateMultiply(left: Any?, right: Any?, operator: Token): Any? {
        return when {
            left is Int && right is Int -> left * right
            else -> throw errorHandler.typeError(
                operator,
                "Operands must be numbers. Got ${typeName(left)} * ${typeName(right)}"
            )
        }
    }

    private fun evaluateDivide(left: Any?, right: Any?, operator: Token): Any? {
        return when {
            left is Int && right is Int -> {
                if (right == 0) {
                    throw errorHandler.error(operator, "Division by zero")
                }
                left / right
            }
            else -> throw errorHandler.typeError(
                operator,
                "Operands must be numbers. Got ${typeName(left)} // ${typeName(right)}"
            )
        }
    }

    private fun evaluateModulo(left: Any?, right: Any?, operator: Token): Any? {
        return when {
            left is Int && right is Int -> {
                if (right == 0) {
                    throw errorHandler.error(operator, "Modulo by zero")
                }
                left % right
            }
            else -> throw errorHandler.typeError(
                operator,
                "Operands must be numbers. Got ${typeName(left)} % ${typeName(right)}"
            )
        }
    }

    // ==================== Comparison Operations ====================

    private fun evaluateLessThan(left: Any?, right: Any?, operator: Token): Boolean {
        return when {
            left is Int && right is Int -> left < right
            else -> throw errorHandler.typeError(
                operator,
                "Operands must be numbers. Got ${typeName(left)} < ${typeName(right)}"
            )
        }
    }

    private fun evaluateLessEqual(left: Any?, right: Any?, operator: Token): Boolean {
        return when {
            left is Int && right is Int -> left <= right
            else -> throw errorHandler.typeError(
                operator,
                "Operands must be numbers. Got ${typeName(left)} <= ${typeName(right)}"
            )
        }
    }

    private fun evaluateGreaterThan(left: Any?, right: Any?, operator: Token): Boolean {
        return when {
            left is Int && right is Int -> left > right
            else -> throw errorHandler.typeError(
                operator,
                "Operands must be numbers. Got ${typeName(left)} > ${typeName(right)}"
            )
        }
    }

    private fun evaluateGreaterEqual(left: Any?, right: Any?, operator: Token): Boolean {
        return when {
            left is Int && right is Int -> left >= right
            else -> throw errorHandler.typeError(
                operator,
                "Operands must be numbers. Got ${typeName(left)} >= ${typeName(right)}"
            )
        }
    }

    private fun evaluateEqual(left: Any?, right: Any?): Boolean {
        return left == right
    }

    // ==================== Logical Operations ====================

    private fun evaluateAnd(left: Any?, right: Any?, operator: Token): Boolean {
        if (left !is Boolean) {
            throw errorHandler.typeError(operator, "Left operand of && must be boolean")
        }
        if (right !is Boolean) {
            throw errorHandler.typeError(operator, "Right operand of && must be boolean")
        }
        return left && right
    }

    private fun evaluateOr(left: Any?, right: Any?, operator: Token): Boolean {
        if (left !is Boolean) {
            throw errorHandler.typeError(operator, "Left operand of || must be boolean")
        }
        if (right !is Boolean) {
            throw errorHandler.typeError(operator, "Right operand of || must be boolean")
        }
        return left || right
    }

    // ==================== Helper Methods ====================

    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }

    private fun stringify(value: Any?): String {
        return when (value) {
            null -> "null"
            is Boolean -> if (value) "true" else "false"
            is String -> value
            is Int -> value.toString()
            else -> value.toString()
        }
    }

    private fun typeName(value: Any?): String {
        return when (value) {
            null -> "null"
            is Boolean -> "Boolean"
            is String -> "String"
            is Int -> "Int"
            is SafariZoneObject -> "SafariZoneObject"
            is TeamObject -> "TeamObject"
            is PokemonCollection -> "PokemonCollection"
            else -> value::class.simpleName ?: "Unknown"
        }
    }
}
