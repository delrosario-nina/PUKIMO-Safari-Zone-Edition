package parser
import lexer.Token

class ParsingContext {
    private var controlBlockDepth = 0

    fun enterControlBlock() { controlBlockDepth++ }
    fun exitControlBlock() { controlBlockDepth-- }
    fun isInControlBlock(): Boolean = controlBlockDepth > 0
    fun validateRunStatement(token: Token): Boolean {
        if (!isInControlBlock()) throw Exception("'run' only allowed in control blocks")
        return true
    }
}
