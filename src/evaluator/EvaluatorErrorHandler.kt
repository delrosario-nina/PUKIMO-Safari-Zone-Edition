package evaluator

import lexer.Token

/*
 * Represents runtime errors encountered during evaluation.
 * Includes token location for better error messages.
 */
data class RuntimeError(
    val token: Token,
    val errorMessage: String,
    val phase: ErrorPhase = ErrorPhase.RUNTIME
) : RuntimeException("[line ${token.lineNumber}] ${phase.label}: $errorMessage")

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
     * Creates and records a runtime error with formatted message
     */
    fun error(
        token: Token,
        message: String,
        phase: ErrorPhase = ErrorPhase.RUNTIME
    ): RuntimeError {
        val runtimeError = RuntimeError(token, message, phase)
        errors.add(runtimeError)
        return runtimeError
    }

    /*
     * Creates a type error (e.g., "Expected number, got string")
     */
    fun typeError(token: Token, message: String): RuntimeError {
        return error(token, message, ErrorPhase.TYPE)
    }

    /*
     * Creates a name error (e.g., "Undefined variable 'x'")
     */
    fun nameError(token: Token, message: String): RuntimeError {
        return error(token, message, ErrorPhase.NAME)
    }

    /*
     * Creates an argument error (e.g., "Expected 2 arguments, got 3")
     */
    fun argumentError(token: Token, message: String): RuntimeError {
        return error(token, message, ErrorPhase.ARGUMENT)
    }

    /*
     * Creates a property error (e.g., "Unknown property 'xyz'")
     */
    fun propertyError(token: Token, message: String): RuntimeError {
        return error(token, message, ErrorPhase.PROPERTY)
    }

    /*
     * Marks that a fatal error occurred (should stop evaluation)
     */
    fun markFatal() {
        hasEncounteredFatalError = true
    }

    /*
     * Check if there are any errors
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()



    /*
     * Clear all errors (useful for REPL sessions)
     */
    fun clearErrors() {
        errors.clear()
        hasEncounteredFatalError = false
    }

    /*
     * Print all errors to console
     */
    fun reportErrors() {
        errors.forEach { println(it.message) }
    }

    /*
     * Get formatted error report
     */
    fun getErrorReport(): String {
        return buildString {
            appendLine("=== Runtime Errors (${errors.size}) ===")
            errors.forEachIndexed { index, error ->
                appendLine("${index + 1}. ${error.message}")
            }
        }
    }
}

