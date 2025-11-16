package parser

import lexer.*

class TokenBuffer(private val tokens: List<Token>) {
    private var current = 0

    fun peek(): Token = tokens.getOrElse(current) { tokens.last() }
    fun peekNext(): Token = tokens.getOrElse(current + 1) { tokens.last() }
    fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }
    fun previous(): Token = tokens[current - 1]
    fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    fun check(type: TokenType): Boolean = !isAtEnd() && peek().type == type

    fun match(vararg types: TokenType): Boolean {
        for (type in types) if (check(type)) { advance(); return true }
        return false
    }
}
