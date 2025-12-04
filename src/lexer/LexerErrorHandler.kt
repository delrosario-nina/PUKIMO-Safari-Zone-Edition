package lexer

/**
 * Represents lexical errors encountered during scanning.
 */
class LexerError(
    val lineNumber: Int,
    message: String,
    val character: Char? = null
) : RuntimeException(message)

/**
 * Manages and tracks lexical errors during the scanning process.
 */
class LexerErrorHandler {
    private val errors = mutableListOf<LexerError>()

    /**
     * Records a lexical error with line number and optional character context.
     */
    fun error(lineNumber: Int, message: String, char: Char? = null): LexerError {
        val charInfo = if (char != null) " ('$char')" else ""
        val errorMsg = "[line $lineNumber] Lexical error: $message$charInfo"
        val error = LexerError(lineNumber, errorMsg, char)
        errors.add(error)
        return error
    }

    /**
     * Checks if any errors have been recorded.
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * Returns the count of recorded errors.
     */
    fun errorCount(): Int = errors. size

    /**
     * Prints all recorded errors to the console.
     */
    fun reportErrors() {
        if (errors. isEmpty()) return

        errors.forEach { err ->
            println(err.message)
        }
    }
}