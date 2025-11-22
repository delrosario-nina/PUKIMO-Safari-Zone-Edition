package parser

import lexer.Token

/**
 * Tracks parsing context state during the parsing process.
 * Manages control block depth to enforce language-specific rules
 * (e.g., 'run' statements only allowed inside control blocks).
 */
class ParsingContext {
    private var controlBlockDepth = 0

    fun enterControlBlock() {
        controlBlockDepth++
    }

    fun exitControlBlock() {
        controlBlockDepth--
    }

    fun isInControlBlock(): Boolean = controlBlockDepth > 0

    /**
     * Validates that a 'run' statement appears in a valid context.
     * Throws an exception if 'run' is used outside of control blocks.
     */
    fun validateRunStatement(token: Token) {
        if (!isInControlBlock()) {
            throw ParserError(token, "'run' statement is only allowed inside control blocks")
        }
    }
}
