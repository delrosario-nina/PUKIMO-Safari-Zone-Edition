package parser

import lexer.Token

class ParsingContext (private val errorHandler: ParserErrorHandler) {
    private var controlBlockDepth = 0

    fun enterControlBlock() {
        controlBlockDepth++
    }

    fun exitControlBlock() {
        controlBlockDepth--
    }

    fun isInControlBlock(): Boolean = controlBlockDepth > 0

    fun validateBreakStatement(token: Token) {
        if (! isInControlBlock()) {
            throw errorHandler.error(token, "'break' statement is only allowed inside loops")
        }
    }

    fun validateContinueStatement(token: Token) {
        if (!isInControlBlock()) {
            throw errorHandler.error(token, "'continue' statement is only allowed inside loops")
        }
    }

    fun validateNotReserved(token: Token, keyword: String) {
        if (token.lexeme == keyword) {
            throw errorHandler.error(token, "'$keyword' is a reserved keyword and cannot be declared as a variable")
        }
    }

    fun validateNotReadOnly(token: Token, keyword: String) {
        if (token.lexeme == keyword) {
            throw errorHandler.error(token, "'$keyword' is read-only and cannot be assigned")
        }
    }

    fun validateRunStatement(token: Token) {
        if (!isInControlBlock()) {
            throw errorHandler.error(token, "'run' statement is only allowed inside control blocks")
        }
    }
}