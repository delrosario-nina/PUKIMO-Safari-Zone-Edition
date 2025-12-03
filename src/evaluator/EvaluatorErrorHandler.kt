package evaluator

import lexer.Token

/*
 * Represents runtime errors encountered during evaluation.
 * Includes token location for better error messages.
 */
data class RuntimeError(
    val token: Token,
    override val message: String,
    val hints: List<String> = emptyList(),
    val phase: ErrorPhase = ErrorPhase.RUNTIME,
    val pattern: String? = null
) : RuntimeException(message)

/*
 * Categories of runtime errors for better organization
 */
enum class ErrorPhase(val label: String) {
    RUNTIME("Runtime error"),
    TYPE("Type error"),
    NAME("Name error"),
    ARGUMENT("Argument error"),
    PROPERTY("Property error")
}

/*
 * Manages and tracks runtime errors during program evaluation.
 * Provides utilities for error collection, reporting, and recovery.
 */
class EvaluatorErrorHandler {
    private val errors = mutableListOf<RuntimeError>()
    private var hasEncounteredFatalError = false

    /*
     * Creates and records a runtime error with formatted message.
     * Throws the error for immediate evaluation halt.
     */
    fun error(
        token: Token,
        message: String,
        phase: ErrorPhase = ErrorPhase.RUNTIME,
        hints: List<String> = emptyList(),
        pattern: String? = null
    ): RuntimeError {
        val runtimeError = RuntimeError(token, message, hints, phase, pattern)
        errors.add(runtimeError)
        reportSingleError(runtimeError)
        throw runtimeError
    }

    /*
     * Creates a type error (e.g., "Expected number, got string")
     */
    fun typeError(token: Token, message: String, hints: List<String> = emptyList(), pattern: String? = null): RuntimeError {
        return error(token, message, ErrorPhase.TYPE, hints, pattern)
    }

    /*
     * Creates a name error (e.g., "Undefined variable 'x'")
     */
    fun nameError(token: Token, message: String, hints: List<String> = emptyList(), pattern: String? = null): RuntimeError {
        return error(token, message, ErrorPhase.NAME, hints, pattern)
    }

    /*
     * Creates an argument error (e.g., "Expected 2 arguments, got 3")
     */
    fun argumentError(token: Token, message: String, hints: List<String> = emptyList(), pattern: String? = null): RuntimeError {
        return error(token, message, ErrorPhase.ARGUMENT, hints, pattern)
    }

    /*
     * Creates a property error (e.g., "Unknown property 'xyz'")
     */
    fun propertyError(token: Token, message: String, hints: List<String> = emptyList(), pattern: String? = null): RuntimeError {
        return error(token, message, ErrorPhase.PROPERTY, hints, pattern)
    }

    /*
     * Append a hint to the most recent error that matches the token (by line and lexeme if available).
     * If no matching error exists, create a new runtime error with the hint as a note (and optional pattern).
     */
    fun appendHintToLast(token: Token, hint: String, pattern: String? = null) {
        val idx = errors.indexOfLast { it.token.lineNumber == token.lineNumber && it.token.lexeme == token.lexeme }
        if (idx >= 0) {
            val old = errors[idx]
            val newHints = old.hints + hint
            errors[idx] = RuntimeError(old.token, old.message, newHints, old.phase, old.pattern ?: pattern)
        } else {
            val primary = "Note at '${token.lexeme}'"
            val noteError = RuntimeError(token, primary, listOf(hint), ErrorPhase.RUNTIME, pattern)
            errors.add(noteError)
            reportSingleError(noteError)
        }
    }

    /*
     * Clear all errors (useful for REPL sessions)
     */
    fun clearErrors() {
        errors.clear()
        hasEncounteredFatalError = false
    }


    fun reportSingleError(err: RuntimeError) {
        println("[line ${err.token.lineNumber}] Runtime error: ${err.message}")
    }

}