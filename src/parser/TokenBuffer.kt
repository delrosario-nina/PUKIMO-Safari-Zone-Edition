package parser

import lexer.*

/*
 * Manages token stream navigation during parsing.
 * Provides lookahead, backtracking, and token matching capabilities.
*/
class TokenBuffer(private val tokens: List<Token>) {
    private var current = 0

    // Returns the current token without advancing.
    fun peek(): Token = tokens.getOrElse(current) { tokens.last() }

    // Returns the next token without advancing.
    fun peekNext(): Token = tokens.getOrElse(current + 1) { tokens.last() }

    // Consumes the current token and moves to the next one.
    fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    // Returns the most recently consumed token.
    fun previous(): Token = tokens.getOrElse(current - 1) { tokens.first() }

    // Checks if we've reached the end of the token stream.
    fun isAtEnd(): Boolean = peek().type == TokenType.EOF

     // Checks if the current token matches the given type.
    fun check(type: TokenType): Boolean = !isAtEnd() && peek().type == type

     /*
     * Checks if current token matches any of the given types.
     * If matched, consumes the token and returns true.
     */
    fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }
}
