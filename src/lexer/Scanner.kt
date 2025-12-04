package lexer

class Scanner(private val errorHandler: LexerErrorHandler = LexerErrorHandler()) {

    private var lineNumber: Int = 1

    fun getErrorHandler(): LexerErrorHandler = errorHandler

    fun classifyWord(word: String): Pair<TokenType, Any? > {
        return when (word) {
            "var" -> TokenType.VAR_KEYWORD to null
            "if" -> TokenType.IF_KEYWORD to null
            "else" -> TokenType.ELSE_KEYWORD to null
            "explore" -> TokenType.EXPLORE_KEYWORD to null
            "define" -> TokenType.DEFINE_KEYWORD to null
            "return" -> TokenType.RETURN_KEYWORD to null
            "print" -> TokenType.PRINT_KEYWORD to null
            "const" -> TokenType.CONST_KEYWORD to null
            "SafariZone" -> TokenType.SAFARI_ZONE to null
            "Team" -> TokenType.TEAM to null
            "while" -> TokenType.WHILE_KEYWORD to null
            "for" -> TokenType.FOR_KEYWORD to null
            "in" -> TokenType.IN_KEYWORD to null
            "to" -> TokenType. TO_KEYWORD to null
            "break" -> TokenType. BREAK_KEYWORD to null
            "continue" -> TokenType. CONTINUE_KEYWORD to null

            "true" -> TokenType. BOOLEAN_LITERAL to true
            "false" -> TokenType. BOOLEAN_LITERAL to false
            "null" -> TokenType.NULL_LITERAL to null

            else -> {
                if (word. matches(Regex("""\d+"""))) {
                    TokenType.NUMERIC_LITERAL to word.toInt()
                } else {
                    TokenType. IDENTIFIER to null
                }
            }
        }
    }

    fun scanIdentifierOrKeyword(source: String, start: Int): Pair<Token?, Int> {
        var index = start
        while (index < source. length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
        val lexeme = source. substring(start, index)
        val (type, literal) = classifyWord(lexeme)
        return Token(type, lexeme, literal, lineNumber) to index
    }

    fun scanNumber(source: String, start: Int): Pair<Token?, Int> {
        var index = start
        while (index < source.length && source[index].isDigit()) index++
        val lexeme = source.substring(start, index)

        val literal = lexeme.toIntOrNull()
        if (literal == null) {
            errorHandler. error(lineNumber, "Number too large: $lexeme")
            return null to index
        }

        return Token(TokenType.NUMERIC_LITERAL, lexeme, literal, lineNumber) to index
    }

    fun scanString(source: String, start: Int): Pair<Token?, Int> {
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

        if (index >= source.length) {
            errorHandler.error(lineNumber, "Unterminated string")
            return null to index
        }

        index++
        val value = sb.toString()
        return Token(TokenType. STRING_LITERAL, value, value, lineNumber) to index
    }

    fun scanOperator(source: String, start: Int): Pair<Token?, Int> {
        val remaining = source.substring(start)

        val twoCharOps = mapOf(
            "==" to TokenType. EQUAL_EQUAL,
            "!=" to TokenType.NOT_EQUAL,
            "<=" to TokenType. LESS_EQUAL,
            ">=" to TokenType. GREATER_EQUAL,
            "&&" to TokenType.AND,
            "||" to TokenType. OR,
            "->" to TokenType.ARROW
        )

        for ((symbol, type) in twoCharOps) {
            if (remaining.startsWith(symbol)) {
                return Token(type, symbol, null, lineNumber) to (start + symbol.length)
            }
        }

        val oneChar = remaining. first()
        val type = when (oneChar) {
            '+' -> TokenType.PLUS
            '-' -> TokenType. MINUS
            '*' -> TokenType.MULTIPLY
            '/' -> TokenType.DIVIDE
            '%' -> TokenType.MODULO
            '=' -> TokenType. ASSIGN
            '<' -> TokenType.LESS_THAN
            '>' -> TokenType. GREATER_THAN
            '!' -> TokenType.NOT
            '[' -> TokenType.LEFT_BRACKET
            ']' -> TokenType.RIGHT_BRACKET
            '(' -> TokenType.LEFT_PAREN
            ')' -> TokenType.RIGHT_PAREN
            '{' -> TokenType. LEFT_BRACE
            '}' -> TokenType.RIGHT_BRACE
            ',' -> TokenType.COMMA
            '.' -> TokenType.DOT
            ';' -> TokenType. SEMICOLON

            else -> {
                errorHandler.error(lineNumber, "Unexpected character", oneChar)
                return null to (start + 1)
            }
        }

        return Token(type, oneChar. toString(), null, lineNumber) to (start + 1)
    }

    private fun scanToken(source: String, start: Int): Pair<Token?, Int> {
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
        lineNumber = 1
        var index = 0

        while (index < source.length) {
            val char = source[index]

            if (char == '\n') {
                lineNumber++
                index++
                continue
            }

            if (char.isWhitespace()) {
                index++
                continue
            }

            // Single-line comment: :> ...
            if (char == ':' && index + 1 < source. length && source[index + 1] == '>') {
                while (index < source.length && source[index] != '\n') {
                    index++
                }
                continue
            }

            // Multi-line comment: /* ... */
            if (char == '/' && index + 1 < source.length && source[index + 1] == '*') {
                index += 2

                while (index < source.length &&
                    !(source[index] == '*' && index + 1 < source.length && source[index + 1] == '/')) {
                    if (source[index] == '\n') lineNumber++
                    index++
                }

                if (index + 1 >= source.length) {
                    errorHandler.error(lineNumber, "Unterminated multi-line comment")
                    break
                }

                index += 2
                continue
            }

            val (token, nextIndex) = scanToken(source, index)

            if (token != null) {
                tokens.add(token)
            }

            index = nextIndex
        }

        tokens.add(Token(TokenType.EOF, "", null, lineNumber))

        if (errorHandler.hasErrors()) {
            errorHandler.reportErrors()
            // Uncomment to stop compilation on lexical errors:
            // throw LexerError(lineNumber, "Scanning failed with ${errorHandler.errorCount()} error(s)")
        }

        return tokens
    }
}