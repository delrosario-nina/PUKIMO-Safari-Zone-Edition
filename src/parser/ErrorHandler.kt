package parser

import lexer.*

data class ParserError(
    val token: Token,
    val errorMessage: String
) : RuntimeException(errorMessage)

class ErrorHandler {
    private val errors = mutableListOf<ParserError>()

    fun error(token: Token, message: String): ParserError {
        val location = if (token.type == TokenType.EOF) {
            "end"
        } else {
            "'${token.lexeme}'"
        }

        val errorMsg = "[line ${token.lineNumber}] Parse error: $message at $location"
        val parserError = ParserError(token, errorMsg)
        errors.add(parserError)
        return parserError
    }

    fun getErrors(): List<ParserError> = errors
    fun hasErrors(): Boolean = errors.isNotEmpty()
    fun clearErrors() { errors.clear() }
}
