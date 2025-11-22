package parser

import lexer.*

//Represents parsing errors encountered during the parsing process.
data class ParserError(
    val token: Token,
    val errorMessage: String
) : RuntimeException(errorMessage)


/*
 * Manages and tracks parsing errors during the parsing process.
 * Collects errors for batch reporting and provides utilities for error management.
*/
class ErrorHandler {
    private val errors = mutableListOf<ParserError>()

   /* Creates and records a parser error with formatted message.
    Builds a user-friendly error message with line number and token location.*/
    fun error(token: Token, message: String): ParserError {
        val location = if (token.type == TokenType.EOF) "end" else "'${token.lexeme}'"
        val errorMsg = "[line ${token.lineNumber}] Parse error: $message at $location"
        val parserError = ParserError(token, errorMsg)
        errors.add(parserError)
        return parserError
    }

    @Suppress("unused")
    fun hasErrors(): Boolean = errors.isNotEmpty()

    @Suppress("unused")
    fun getErrors(): List<ParserError> = errors.toList()

    @Suppress("unused")
    fun clearErrors() = errors.clear()

    @Suppress("unused")
    fun reportErrors() {
        errors.forEach { println(it.errorMessage) }
    }
}
