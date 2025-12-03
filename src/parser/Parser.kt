package parser

import lexer.*

/**
 * Recursive descent parser for the PukiMO language.
 * Implements TypeScript-style error recovery: collects multiple errors
 * and provides helpful context without stopping at the first error.
 */
class Parser(
    private val tokens: List<Token>,
    private val tokenBuffer: TokenBuffer = TokenBuffer(tokens),
    private val context: ParsingContext = ParsingContext(),
    private val errorHandler: ErrorHandler = ErrorHandler(),
) {

    fun parse(): Program {
        val stmtList = mutableListOf<Stmt>()

        while (!tokenBuffer.isAtEnd()) {
            try {
                stmtList.add(
                    if (tokenBuffer.check(TokenType.IF_KEYWORD)) parseIfStmt()
                    else parseNonIfStmt()
                )
            } catch (e: ParserError) {
                errorHandler.addContextualHints(e)
                synchronize()
            }
        }

        if (errorHandler.hasErrors()) {
            errorHandler.reportErrors()
        }

        return Program(stmtList)
    }

    private fun synchronize() {
        addDelimiterHints()
        skipToNextStatement()
    }

    /**
     * Adds hints for unclosed delimiters if the error occurred after ( or {
     */
    private fun addDelimiterHints() {
        try {
            val errorToken = tokenBuffer.peek()
            val previousTokenType = tokenBuffer.previous().type

            when (previousTokenType) {
                TokenType.LEFT_PAREN ->
                    errorHandler.appendHintToLast(errorToken, "Unclosed '(' - missing ')'")

                TokenType.LEFT_BRACE ->
                    errorHandler.appendHintToLast(errorToken, "Unclosed '{' - missing '}'")

                else -> { }
            }

        } catch (_: Exception) {
            // Ignore if previous() fails
        }
    }

    /**
     * Skips tokens until a statement boundary or keyword is found
     */
    private fun skipToNextStatement() {
        tokenBuffer.advance()

        while (!tokenBuffer.isAtEnd()) {
            if (isStatementBoundary()) return
            if (isStatementKeyword()) return
            tokenBuffer.advance()
        }
    }

    /**
     * Checks if we're at a statement boundary (semicolon)
     */
    private fun isStatementBoundary(): Boolean {
        return tokenBuffer.previous().type == TokenType.SEMICOLON
    }

    /**
     * Checks if we're at a statement-starting keyword
     */
    private fun isStatementKeyword(): Boolean {
        return when (tokenBuffer.peek().type) {
            TokenType.VAR_KEYWORD,
            TokenType.DEFINE_KEYWORD,
            TokenType.IF_KEYWORD,
            TokenType.PRINT_KEYWORD,
            TokenType.RUN_KEYWORD,
            TokenType.EXPLORE_KEYWORD,
            TokenType.RETURN_KEYWORD -> true
            else -> false
        }
    }

    private fun parseNonIfStmt(): Stmt {
        return when {
            tokenBuffer.match(TokenType.VAR_KEYWORD) -> parseVarDeclStmt()
            tokenBuffer.match(TokenType.PRINT_KEYWORD) -> parsePrintStmt()
            tokenBuffer.match(TokenType.RUN_KEYWORD) -> parseRunStmt()
            tokenBuffer.match(TokenType.EXPLORE_KEYWORD) -> parseExploreStmt()
            tokenBuffer.match(TokenType.RETURN_KEYWORD) -> parseReturnStmt()
            tokenBuffer.match(TokenType.DEFINE_KEYWORD) -> parseDefineStmt()
            tokenBuffer.check(TokenType.LEFT_BRACE) -> parseBlock()
            else -> parseExprStmt()
        }
    }

    private fun parseIfStmt(): Stmt {
        consume(TokenType.IF_KEYWORD, "Expected 'if' keyword")
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'")
        val condition = parseExpression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition")

        context.enterControlBlock()

        val thenBlock = if (tokenBuffer.check(TokenType.LEFT_BRACE)) {
            parseBlock()
        } else {
            throw errorHandler.error(tokenBuffer.peek(), "Expected '{' to start 'if' block")
        }

        val elseBlock = if (tokenBuffer.match(TokenType.ELSE_KEYWORD)) {
            if (tokenBuffer.check(TokenType.LEFT_BRACE)) {
                parseBlock()
            } else {
                throw errorHandler.error(tokenBuffer.peek(), "Expected '{' to start 'else' block")
            }
        } else null

        context.exitControlBlock()

        return IfStmt(condition, thenBlock, elseBlock)
    }

    private fun parseVarDeclStmt(): Stmt {
        val identifier = consume(TokenType.IDENTIFIER, "Expected variable name")
        if (identifier.lexeme == "encounter") {
            throw errorHandler.error(identifier, "'encounter' is a reserved keyword and cannot be declared as a variable.")
        }
        val expr = if (tokenBuffer.match(TokenType.ASSIGN)) {
            parseExpression()
        } else {
            LiteralExpr(null)
        }

        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration")
        return VarDeclStmt(identifier, expr)
    }

    private fun parseDefineStmt(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expected function name after 'define'")

        consume(TokenType.LEFT_PAREN, "Expected '(' after function name")
        val params = mutableListOf<Parameter>()
        if (!tokenBuffer.check(TokenType.RIGHT_PAREN)) {
            do {
                val paramName = consume(TokenType.IDENTIFIER, "Expected parameter name")

                // Parse colon and type (e.g. a: int)
                consume(TokenType.COLON, "Expected ':' after parameter name")
                val typeToken = consume(TokenType.IDENTIFIER, "Expected type name after ':'")

                // Convert type string to Type enum
                val type = parseType(typeToken)

                params.add(Parameter(paramName, type))
            } while (tokenBuffer.match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after function parameters")

        val body = parseBlock()
        return DefineStmt(name, params, body)
    }

    // Utility to map type name string to Type enum
    private fun parseType(typeToken: Token): Type {
        return when (typeToken.lexeme.lowercase()) {
            "int"    -> Type.INT
            "double" -> Type.DOUBLE
            "string" -> Type.STRING
            "bool"   -> Type.BOOL
            "safarizone" -> Type.SAFARIZONE
            "team"   -> Type.TEAM
            "object" -> Type.OBJECT
            "pokemon"-> Type.POKEMON
            else     -> throw errorHandler.error(typeToken, "Unknown parameter type: ${typeToken.lexeme}")
        }
    }

    private fun parseReturnStmt(): Stmt {
        val keyword = tokenBuffer.previous()

        val value = if (tokenBuffer.check(TokenType.SEMICOLON)) {
            null
        } else {
            parseExpression()
        }

        consume(TokenType.SEMICOLON, "Expected ';' after return statement")
        return ReturnStmt(keyword, value)
    }

    private fun parsePrintStmt(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'print'")
        val expr = parseExpression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after print expression")
        consume(TokenType.SEMICOLON, "Expected ';' after print statement")
        return PrintStmt(expr)
    }

    private fun parseExprStmt(): Stmt {
        val expr = parseExpression()

        if (tokenBuffer.check(TokenType.SEMICOLON)) {
            consume(TokenType.SEMICOLON, "Expected ';' after expression")
        }

        return ExprStmt(expr)
    }

    private fun parseBlock(): Block {
        consume(TokenType.LEFT_BRACE, "Expected '{' at start of block")
        val stmts = mutableListOf<Stmt>()
        var hadError = false

        while (!tokenBuffer.check(TokenType.RIGHT_BRACE) && !tokenBuffer.isAtEnd()) {
            try {
                stmts.add(
                    if (tokenBuffer.check(TokenType.IF_KEYWORD)) parseIfStmt()
                    else parseNonIfStmt()
                )
            } catch (e: ParserError) {
                hadError = true
                synchronize()
                if (tokenBuffer.check(TokenType.RIGHT_BRACE)) break
            }
        }

        if (tokenBuffer.check(TokenType.RIGHT_BRACE)) {
            consume(TokenType.RIGHT_BRACE, "Expected '}' after block")
        } else if (!hadError) {
            throw errorHandler.error(tokenBuffer.peek(), "Expected '}' after block")
        }

        return Block(stmts)
    }


    private fun parseRunStmt(): Stmt {
        val runToken = tokenBuffer.previous()
        context.validateRunStatement(runToken)
        consume(TokenType.SEMICOLON, "Expected ';' after 'run' statement")
        return RunStmt(runToken)
    }

    private fun parseExploreStmt(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'explore'")

        // Expect an identifier for the SafariZone variable
        val safariZoneIdent = consume(TokenType.IDENTIFIER, "Expected SafariZone variable name in 'explore'")

        consume(TokenType.RIGHT_PAREN, "Expected ')' after SafariZone variable name in 'explore'")

        context.enterControlBlock()
        val block = parseBlock()
        context.exitControlBlock()

        // Adjust your AST class: ExploreStmt(Token, Block) or similar
        return ExploreStmt(safariZoneIdent, block)
    }


    private fun parseExpression(): Expr = parseAssignExpr()

    private fun parseAssignExpr(): Expr {
        val expr = parseOr()

        if (tokenBuffer.match(TokenType.ASSIGN)) {
            val equals = tokenBuffer.previous()
            val value = parseAssignExpr()

            if (expr is VariableExpr) {
                if (expr.identifier.lexeme == "encounter") {
                    throw errorHandler.error(expr.identifier, "'encounter' is read-only and cannot be assigned.")
                }
                return AssignExpr(expr, equals, value)
            }
        }
        return expr
    }

    private fun parseOr(): Expr {
        var expr = parseAnd()
        while (tokenBuffer.match(TokenType.OR)) {
            val operator = tokenBuffer.previous()
            val right = parseAnd()
            expr = BinaryExpr(expr, operator, right)
        }
        return expr
    }

    private fun parseAnd(): Expr {
        var expr = parseEquality()
        while (tokenBuffer.match(TokenType.AND)) {
            val operator = tokenBuffer.previous()
            val right = parseEquality()
            expr = BinaryExpr(expr, operator, right)
        }
        return expr
    }

    private fun parseEquality(): Expr {
        var expr = parseRelational()
        while (tokenBuffer.match(TokenType.EQUAL_EQUAL, TokenType.NOT_EQUAL)) {
            val operator = tokenBuffer.previous()
            val right = parseRelational()
            expr = BinaryExpr(expr, operator, right)
        }
        return expr
    }

    private fun parseRelational(): Expr {
        var expr = parseAdditive()
        while (tokenBuffer.match(
                TokenType.LESS_THAN,
                TokenType.LESS_EQUAL,
                TokenType.GREATER_THAN,
                TokenType.GREATER_EQUAL
            )) {
            val operator = tokenBuffer.previous()
            val right = parseAdditive()
            expr = BinaryExpr(expr, operator, right)
        }
        return expr
    }

    private fun parseAdditive(): Expr {
        var expr = parseMultiplicative()
        while (tokenBuffer.match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = tokenBuffer.previous()
            val right = parseMultiplicative()
            expr = BinaryExpr(expr, operator, right)
        }
        return expr
    }

    private fun parseMultiplicative(): Expr {
        var expr = parseUnary()
        while (tokenBuffer.match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO)) {
            val operator = tokenBuffer.previous()
            val right = parseUnary()
            expr = BinaryExpr(expr, operator, right)
        }
        return expr
    }

    private fun parseUnary(): Expr {
        return if (tokenBuffer.match(TokenType.NOT, TokenType.MINUS)) {
            val operator = tokenBuffer.previous()
            val right = parseUnary()
            UnaryExpr(operator, right)
        } else {
            parsePrimaryWithSuffixes()
        }
    }

    private fun parsePrimaryWithSuffixes(): Expr {
        var expr = parsePrimary()
        while (true) {
            expr = when {
                tokenBuffer.match(TokenType.DOT) -> {
                    val member = consume(TokenType.IDENTIFIER, "Expected property name after '.'")
                    if (tokenBuffer.check(TokenType.LEFT_PAREN)) {
                        throw errorHandler.error(
                            member,
                            "Cannot call method with '.' operator. Use '->' for methods: ${member.lexeme}()"
                        )
                    }
                    PropertyAccessExpr(expr, member)
                }

                tokenBuffer.match(TokenType.ARROW) -> {
                    val member = consume(TokenType.IDENTIFIER, "Expected method name after '->'")
                    if (!tokenBuffer.check(TokenType.LEFT_PAREN)) {
                        throw errorHandler.error(
                            member,
                            "Arrow operator '->' is for method calls only. Use '.' for properties: .${member.lexeme}"
                        )
                    }
                    tokenBuffer.match(TokenType.LEFT_PAREN)
                    val args = parseArgList()
                    CallExpr(PropertyAccessExpr(expr, member), args.positional, args.named)
                }

                tokenBuffer.match(TokenType.LEFT_PAREN) -> {
                    val args = parseArgList()
                    CallExpr(expr, args.positional, args.named)
                }

                else -> break
            }
        }
        return expr
    }

    private fun parsePrimary(): Expr {
        return when {
            tokenBuffer.match(
                TokenType.NUMERIC_LITERAL,
                TokenType.STRING_LITERAL,
                TokenType.BOOLEAN_LITERAL,
                TokenType.NULL_LITERAL
            ) -> {
                LiteralExpr(tokenBuffer.previous().literal)
            }

            tokenBuffer.match(TokenType.IDENTIFIER) -> {
                VariableExpr(tokenBuffer.previous())
            }

            tokenBuffer.match(TokenType.SAFARI_ZONE, TokenType.TEAM) -> {
                val token = tokenBuffer.previous()
                consume(TokenType.LEFT_PAREN, "Expected '(' after ${token.lexeme}")
                val args = parseArgList()
                CallExpr(VariableExpr(token), args.positional, args.named)
            }

            tokenBuffer.match(TokenType.LEFT_PAREN) -> {
                val expr = parseExpression()
                consume(TokenType.RIGHT_PAREN, "Expected ')' after expression")
                expr
            }

            else -> throw errorHandler.error(tokenBuffer.peek(), "Unexpected token")
        }
    }

    private data class ArgumentList(val positional: List<Expr>, val named: List<NamedArg>)

    private fun parseArgList(): ArgumentList {
        val positional = mutableListOf<Expr>()
        val named = mutableListOf<NamedArg>()

        if (!tokenBuffer.check(TokenType.RIGHT_PAREN)) {
            do {
                if (tokenBuffer.check(TokenType.IDENTIFIER)) {
                    val nextToken = tokenBuffer.peekNext()

                    if (nextToken.type == TokenType.ASSIGN) {
                        val name = tokenBuffer.advance()
                        consume(TokenType.ASSIGN, "Expected '=' after argument name")
                        val value = parseExpression()
                        named.add(NamedArg(name, value))
                    } else {
                        positional.add(parseExpression())
                    }
                } else {
                    positional.add(parseExpression())
                }
            } while (tokenBuffer.match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments")
        return ArgumentList(positional, named)
    }

    private fun consume(type: TokenType, message: String): Token {
        return tokenBuffer.consume(type, message, errorHandler)
    }
}
