package evaluator

/**
 * Control flow exceptions used for non-local returns.
 */
class BreakException : RuntimeException()
class ContinueException : RuntimeException()
class ReturnException(val value: Any?) : RuntimeException()
class RunException : RuntimeException()