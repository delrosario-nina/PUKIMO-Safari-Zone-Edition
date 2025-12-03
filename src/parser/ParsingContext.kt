package parser

import lexer.Token

/**
 * Tracks parsing context state during the parsing process.
 * Manages control block depth and explore state to enforce language-specific rules
 * (e.g., 'run' statements only allowed inside control blocks,
 *  'encounter' only allowed/declared within explore blocks).
 */
class ParsingContext {
    private var controlBlockDepth = 0
    private var exploreBlockDepth = 0

    fun enterControlBlock() {
        controlBlockDepth++
    }

    fun exitControlBlock() {
        controlBlockDepth--
    }

    fun isInControlBlock(): Boolean = controlBlockDepth > 0

    // ----- Explore block depth tracking -----

    fun enterExploreBlock() {
        exploreBlockDepth++
    }

    fun exitExploreBlock() {
        exploreBlockDepth--
    }

    fun isInExploreBlock(): Boolean = exploreBlockDepth > 0

    /**
     * Validates that a 'run' statement appears in a valid context.
     * Throws an exception if 'run' is used outside of control blocks.
     */
    fun validateRunStatement(token: Token) {
        if (!isInControlBlock()) {
            throw ParserError(token, "'run' statement is only allowed inside control blocks")
        }
    }

    /**
     * Validates that 'encounter' is used only inside explore blocks.
     * Throws an exception if used elsewhere.
     */
    fun validateEncounterUsage(token: Token) {
        if (!isInExploreBlock()) {
            throw ParserError(token, "'encounter' is only available inside explore blocks")
        }
    }
}