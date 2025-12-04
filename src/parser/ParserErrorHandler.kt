package parser

import lexer.*

/**
 * Represents parsing errors encountered during the parsing process.
 */
data class ParserError(
    val token: Token,
    val errorMessage: String,
    val hints: List<String> = emptyList()
) : RuntimeException(errorMessage)

/**
 * Manages and tracks parsing errors during the parsing process.
 * Collects errors for batch reporting and provides utilities for error management.
 */
class ParserErrorHandler {
    private val errors = mutableListOf<ParserError>()

    /**
     * Creates and records a parser error with formatted message.
     * Builds a user-friendly error message with line number and token location.
     */
    fun error(token: Token, message: String): ParserError {
        val location = if (token.type == TokenType.EOF) "end" else "'${token.lexeme}'"
        val errorMsg = "[line ${token.lineNumber}] Parse error: $message at $location"
        val parserError = ParserError(token, errorMsg, emptyList())
        errors.add(parserError)
        return parserError
    }

    /**
     * Appends an additional hint to the most recent error matching the given token.
     * If no matching error exists, the hint is silently ignored.
     */
    fun appendHintToLast(token: Token, hint: String) {
        val idx = errors.indexOfLast {
            it.token.lineNumber == token.lineNumber &&
                    it.token.lexeme == token.lexeme
        }

        if (idx >= 0) {
            val old = errors[idx]
            val newHints = old.hints + hint
            errors[idx] = ParserError(old.token, old.errorMessage, newHints)
        }
        // Hints without matching errors are ignored
    }

    /**
     * Analyzes parser error and adds contextual hints based on error type.
     * Separates hint generation logic from parsing logic.
     */
    fun addContextualHints(error: ParserError) {
        try {
            val msg = error.errorMessage

            when {
                // Missing closing parenthesis
                msg.contains("Expected ')'", ignoreCase = true) ||
                        msg.contains("Unclosed '('", ignoreCase = true) -> {
                    appendHintToLast(error.token, "Missing ')' to close the expression.")
                    appendHintToLast(error.token, "Missing ';' at the end of the statement.")
                }

                // Missing right brace
                msg.contains("Expected '}'", ignoreCase = true) ||
                        msg.contains("Unclosed '{'", ignoreCase = true) -> {
                    appendHintToLast(error.token, "Missing '}' to close the block.")
                }

                // Missing semicolon
                msg.contains("Expected ';'", ignoreCase = true) -> {
                    appendHintToLast(error.token, "Try adding a ';' at the end of the previous expression or statement.")
                }
            }
        } catch (_: Exception) {
            // Don't let hint generation crash the parser
        }
    }

    /**
     * Checks if any errors have been recorded.
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * Prints all recorded errors with their hints to the console.
     */
    fun reportErrors() {
        errors.forEach { err ->
            println(err.errorMessage)
            if (err.hints.isNotEmpty()) {
                err.hints.forEach { hint ->
                    println("  Note: $hint")
                }
            }
        }
    }
}
