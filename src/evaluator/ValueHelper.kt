package evaluator

/**
 * Helper utilities for value operations (truthiness, stringification).
 */
class ValueHelper {
    fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }

    fun stringify(value: Any?): String {
        return when (value) {
            null -> "null"
            is SafariZoneObjectInterface -> value.toString()
            is List<*> -> stringifyArray(value)
            is Boolean -> value.toString().lowercase()
            else -> value.toString()
        }
    }

    private fun stringifyArray(list: List<*>): String {
        return "[" + list.joinToString(", ") { stringify(it) } + "]"
    }
}