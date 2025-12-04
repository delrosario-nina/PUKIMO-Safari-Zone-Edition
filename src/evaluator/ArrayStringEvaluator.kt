package evaluator

import lexer.*
import parser.*

/**
 * Handles array and string operations (indexing, assignment, literals).
 */
class ArrayStringEvaluator(private val evaluator: Evaluator) {

    fun visitArrayLiteralExpr(expr: ArrayLiteralExpr): Any? {
        return expr.elements.map { it.accept(evaluator) }. toMutableList()
    }

    fun visitArrayAccessExpr(expr: ArrayAccessExpr): Any? {
        val array = expr.array.accept(evaluator)
        val index = expr.index.accept(evaluator)
        return performArrayAccess(array, index, expr.leftBracket)
    }

    fun visitArrayAssignExpr(expr: ArrayAssignExpr): Any? {
        val array = expr.array.accept(evaluator)
        val index = expr.index.accept(evaluator)
        val value = expr.value.accept(evaluator)
        return performArrayAssignment(array, index, value, expr.leftBracket)
    }

    fun performArrayAccess(array: Any?, index: Any?, token: Token): Any? {
        return when (array) {
            is MutableList<*> -> {
                val validIndex = validateArrayIndex(array, index, token)
                array[validIndex]
            }
            is String -> {
                val validIndex = validateStringIndex(array, index, token)
                array[validIndex]. toString()
            }
            else -> throw evaluator.getErrorHandler().typeError(
                token,
                "Can only index arrays and strings, got ${array?. javaClass?.simpleName ?: "null"}"
            )
        }
    }

    private fun validateStringIndex(str: String, index: Any?, token: Token): Int {
        if (index !is Int) {
            throw evaluator.getErrorHandler().typeError(token, "String index must be an integer")
        }
        if (index < 0 || index >= str.length) {
            throw evaluator.getErrorHandler().error(
                token,
                "String index $index out of bounds (length ${str.length})"
            )
        }
        return index
    }

    fun performArrayAssignment(array: Any?, index: Any?, value: Any?, token: Token): Any? {
        if (array is String) {
            throw evaluator.getErrorHandler().error(
                token,
                "Strings are immutable and cannot be modified.   Use string concatenation instead."
            )
        }

        validateArrayType(array, token)
        val validIndex = validateArrayIndex(array as MutableList<*>, index, token)

        (array as MutableList<Any?>)[validIndex] = value
        return value
    }

    private fun validateArrayType(array: Any?, token: Token) {
        if (array !is MutableList<*>) {
            throw evaluator.getErrorHandler().typeError(
                token,
                "Can only assign to arrays, not ${array?.javaClass?.simpleName ?: "null"}"
            )
        }
    }

    private fun validateArrayIndex(array: MutableList<*>, index: Any?, token: Token): Int {
        if (index !is Int) {
            throw evaluator. getErrorHandler().typeError(token, "Array index must be an integer")
        }
        if (index < 0 || index >= array.size) {
            throw evaluator.getErrorHandler().error(
                token,
                "Array index $index out of bounds (size ${array.size})"
            )
        }
        return index
    }
}