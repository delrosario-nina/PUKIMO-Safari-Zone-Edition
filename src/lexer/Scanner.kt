package lexer

class Scanner {

    private var lineNumber: Int = 1

    // helper: check if a symbol belongs to a TokenType
    fun TokenType.contains(symbol: String): Boolean {
        return symbols?.contains(symbol) ?: false
    }

    // classifyWord handles keywords, literals, and identifiers
    fun classifyWord(word: String): Pair<TokenType, Any?> {
        return when (word) {
            // --------------------
            // Keywords
            // --------------------
            "if" -> TokenType.IF_KEYWORD to null
            "else" -> TokenType.ELSE_KEYWORD to null
            "explore" -> TokenType.EXPLORE_KEYWORD to null
            "run" -> TokenType.RUN_KEYWORD to null
            "define" -> TokenType.DEFINE_KEYWORD to null
            "print" -> TokenType.PRINT_KEYWORD to null
            "throwBall" -> TokenType.THROWBALL_KEYWORD to null
            "const" -> TokenType.CONST_KEYWORD to null
            "SafariZone" -> TokenType.SAFARI_ZONE to null
            "Team" -> TokenType.TEAM to null

            // --------------------
            // Boolean & null literals
            // --------------------
            "true" -> TokenType.BOOLEAN_LITERAL to true
            "false" -> TokenType.BOOLEAN_LITERAL to false
            "null" -> TokenType.NULL_LITERAL to null

            // --------------------
            // Default cases
            // --------------------
            else -> {
                when {
                    // Numeric literal detection
                    word.matches(Regex("""\d+""")) ->
                        TokenType.NUMERIC_LITERAL to word.toInt()


                    // String literal detection
                    word.matches(Regex(""""([^"\\]|\\.)*"""")) ||
                            word.matches(Regex("""'([^'\\]|\\.)*'""")) ->
                        TokenType.STRING_LITERAL to word.substring(1, word.length - 1)

                    // Otherwise, identifier
                    else -> TokenType.IDENTIFIER to null
                }
            }
        }
    }

    // --------------------
    // SCANNERS
    // --------------------

    fun scanIdentifierOrKeyword(source: String, start: Int): Pair<Token, Int> {
        var index = start
        while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
        val lexeme = source.substring(start, index)
        val (type, literal) = classifyWord(lexeme)
        return Token(type, lexeme, literal, this.lineNumber) to index
    }

    fun scanNumber(source: String, start: Int): Pair<Token, Int> {
        var index = start
        while (index < source.length && (source[index].isDigit() || source[index] == '.')) index++
        val lexeme = source.substring(start, index)
        val literal = lexeme.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid number '$lexeme' at line $lineNumber")
        return Token(TokenType.NUMERIC_LITERAL, lexeme, literal, this.lineNumber) to index
    }

    fun scanString(source: String, start: Int): Pair<Token, Int> {
        var index = start + 1
        val sb = StringBuilder()

        while (index < source.length && source[index] != '"') {
            val char = source[index]
            if (char == '\n') lineNumber++

            if (char == '\\' && index + 1 < source.length) {
                val escaped = when (val nextChar = source[index + 1]) {
                    'n' -> '\n'
                    't' -> '\t'
                    '\\' -> '\\'
                    '"' -> '"'
                    else -> nextChar
                }
                sb.append(escaped)
                index += 2
            } else {
                sb.append(char)
                index++
            }
        }

        if (index >= source.length) throw IllegalArgumentException("Unterminated string at line $lineNumber")
        index++ // skip closing quote
        val value = sb.toString()
        return Token(TokenType.STRING_LITERAL, value, value, lineNumber) to index
    }

    fun scanOperator(source: String, start: Int): Pair<Token, Int> {
        val remaining = source.substring(start)

        // Two-character operators
        val twoCharOps = mapOf(
            "==" to TokenType.EQUAL_EQUAL,
            "!=" to TokenType.NOT_EQUAL,
            "<=" to TokenType.LESS_EQUAL,
            ">=" to TokenType.GREATER_EQUAL,
            "&&" to TokenType.AND,
            "||" to TokenType.OR,
            "->" to TokenType.ARROW
        )
        for ((symbol, type) in twoCharOps) {
            if (remaining.startsWith(symbol)) {
                return Token(type, symbol, null, lineNumber) to (start + symbol.length)
            }
        }

        // Single-character operators or delimiters
        val oneChar = remaining.first().toString()
        val type = when (oneChar) {
            "+" -> TokenType.PLUS
            "-" -> TokenType.MINUS
            "*" -> TokenType.MULTIPLY
            "/" -> TokenType.DIVIDE
            "%" -> TokenType.MODULO
            "=" -> TokenType.ASSIGN
            "<" -> TokenType.LESS_THAN
            ">" -> TokenType.GREATER_THAN
            "!" -> TokenType.NOT

            "(" -> TokenType.LEFT_PAREN
            ")" -> TokenType.RIGHT_PAREN
            "{" -> TokenType.LEFT_BRACE
            "}" -> TokenType.RIGHT_BRACE
            "[" -> TokenType.LEFT_BRACKET
            "]" -> TokenType.RIGHT_BRACKET
            "," -> TokenType.COMMA
            "." -> TokenType.DOT
            ";" -> TokenType.SEMICOLON

            else -> throw IllegalArgumentException("Unexpected character '$oneChar' at line $lineNumber")
        }

        return Token(type, oneChar, null, lineNumber) to (start + 1)
    }

    // --------------------
    // Main token scanning logic
    // --------------------
    fun scanToken(source: String, start: Int): Pair<Token, Int> {
        val char = source[start]
        return when {
            char.isLetter() || char == '_' -> scanIdentifierOrKeyword(source, start)
            char.isDigit() -> scanNumber(source, start)
            char == '"' -> scanString(source, start)
            else -> scanOperator(source, start)
        }
    }

    fun scanAll(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0

        while (index < source.length) {
            val char = source[index]

            // Handle newlines
            if (char == '\n') {
                lineNumber++
                index++
                continue
            }

            // Skip whitespace
            if (char.isWhitespace()) { index++; continue }

            // Single-line comment :>
            if (char == ':' && index + 1 < source.length && source[index + 1] == '>') {
                while (index < source.length && source[index] != '\n') index++
                continue
            }

            // Multi-line comment /* ... */
            if (char == '/' && index + 1 < source.length && source[index + 1] == '*') {
                index += 2
                while (index < source.length &&
                    !(source[index] == '*' && index + 1 < source.length && source[index + 1] == '/')) {
                    if (source[index] == '\n') lineNumber++
                    index++
                }
                if (index + 1 >= source.length)
                    throw IllegalArgumentException("Unterminated multi-line comment at line $lineNumber")
                index += 2
                continue
            }

            // Scan token
            val (token, nextIndex) = scanToken(source, index)
            tokens.add(token)
            index = nextIndex
        }

        // Always add EOF token
        tokens.add(Token(TokenType.EOF, "", null, lineNumber))
        return tokens
    }

}
