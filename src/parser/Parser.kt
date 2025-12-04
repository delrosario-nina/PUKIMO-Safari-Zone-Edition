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
    private val errorHandler: ParserErrorHandler = ParserErrorHandler(),
    private val context: ParsingContext = ParsingContext(errorHandler),
) {

    fun getErrorHandler(): ParserErrorHandler = errorHandler

    fun parse(): Program {
        val stmtList = parseStatements()

        if (errorHandler.hasErrors()) {
            errorHandler.reportErrors()
        }

        return Program(stmtList)
    }

    private fun parseStatements(): MutableList<Stmt> {
        val stmtList = mutableListOf<Stmt>()

        while (!tokenBuffer.isAtEnd()) {
            try {
                stmtList.add(parseStatement())
            } catch (e: ParserError) {
                errorHandler.addContextualHints(e)
                synchronize()
            }
        }

        return stmtList
    }

    private fun parseStatement(): Stmt {
        return if (tokenBuffer.check(TokenType.IF_KEYWORD)) {
            parseIfStmt()
        } else {
            parseNonIfStmt()
        }
    }

    private fun parseNonIfStmt(): Stmt {
        return when {
            tokenBuffer.match(TokenType.VAR_KEYWORD) -> parseVarDeclStmt()
            tokenBuffer.match(TokenType.PRINT_KEYWORD) -> parsePrintStmt()
            tokenBuffer. match(TokenType.WHILE_KEYWORD) -> parseWhileStmt()
            tokenBuffer. match(TokenType.FOR_KEYWORD) -> parseForStmt()
            tokenBuffer.match(TokenType.BREAK_KEYWORD) -> parseBreakStmt()
            tokenBuffer. match(TokenType.CONTINUE_KEYWORD) -> parseContinueStmt()
            tokenBuffer. match(TokenType. EXPLORE_KEYWORD) -> parseExploreStmt()
            tokenBuffer.match(TokenType.RETURN_KEYWORD) -> parseReturnStmt()
            tokenBuffer. match(TokenType.DEFINE_KEYWORD) -> parseDefineStmt()
            tokenBuffer.check(TokenType.LEFT_BRACE) -> parseBlock()
            else -> parseExprStmt()
        }
    }

    // ========== Statement Parsers ==========

    private fun parseWhileStmt(): WhileStmt {
        val keyword = tokenBuffer.previous()
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'while'")
        val condition = parseExpression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after condition")

        context.enterControlBlock()
        val body = parseBlock()
        context.exitControlBlock()

        return WhileStmt(keyword, condition, body)
    }

    private fun parseForStmt(): ForStmt {
        val keyword = tokenBuffer.previous()

        // Optionally consume parentheses
        val hasParens = tokenBuffer.match(TokenType.LEFT_PAREN)

        val variable = consume(TokenType.IDENTIFIER, "Expected variable name in for loop")
        consume(TokenType.IN_KEYWORD, "Expected 'in' after variable")

        val start = parseExpression()

        // Make 'to' clause optional
        val end = if (tokenBuffer.match(TokenType.TO_KEYWORD)) {
            parseExpression()
        } else {
            null  // No 'to' means iterating over collection/string
        }

        // Only require closing paren if opening paren was present
        if (hasParens) {
            consume(TokenType.RIGHT_PAREN, "Expected ')' after for clause")
        }

        context.enterControlBlock()
        val body = parseBlock()
        context.exitControlBlock()

        return ForStmt(keyword, variable, start, end, body)
    }
    private fun parseBreakStmt(): BreakStmt {
        val keyword = tokenBuffer.previous()
        context.validateBreakStatement(keyword)
        consume(TokenType.SEMICOLON, "Expected ';' after 'break'")
        return BreakStmt(keyword)
    }

    private fun parseContinueStmt(): ContinueStmt {
        val keyword = tokenBuffer.previous()
        context.validateContinueStatement(keyword)
        consume(TokenType. SEMICOLON, "Expected ';' after 'continue'")
        return ContinueStmt(keyword)
    }

    private fun parseIfStmt(): Stmt {
        consume(TokenType.IF_KEYWORD, "Expected 'if' keyword")
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'")
        val condition = parseExpression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition")

        context.enterControlBlock()
        val thenBlock = parseBlockOrError("Expected '{' to start 'if' block")
        val elseBlock = parseElseBlock()
        context.exitControlBlock()

        return IfStmt(condition, thenBlock, elseBlock)
    }

    private fun parseElseBlock(): Block?  {
        if (!tokenBuffer.match(TokenType. ELSE_KEYWORD)) return null
        return parseBlockOrError("Expected '{' to start 'else' block")
    }

    private fun parseBlockOrError(errorMessage: String): Block {
        if (tokenBuffer.check(TokenType. LEFT_BRACE)) {
            return parseBlock()
        } else {
            throw errorHandler.error(tokenBuffer. peek(), errorMessage)
        }
    }

    private fun parseVarDeclStmt(): Stmt {
        val identifier = consume(TokenType.IDENTIFIER, "Expected variable name")
        context.validateNotReserved(identifier, "encounter")

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
        val params = parseParameterList()
        val body = parseBlock()
        return DefineStmt(name, params, body)
    }

    private fun parseParameterList(): MutableList<Token> {
        consume(TokenType.LEFT_PAREN, "Expected '(' after function name")
        val params = mutableListOf<Token>()

        if (!tokenBuffer.check(TokenType.RIGHT_PAREN)) {
            do {
                val paramName = consume(TokenType.IDENTIFIER, "Expected parameter name")
                params.add(paramName)
            } while (tokenBuffer.match(TokenType.COMMA))
        }

        consume(TokenType.RIGHT_PAREN, "Expected ')' after function parameters")
        return params
    }

    private fun parseReturnStmt(): Stmt {
        val keyword = tokenBuffer.previous()
        val value = if (tokenBuffer.check(TokenType.SEMICOLON)) null else parseExpression()
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
        consume(TokenType.SEMICOLON, "Expected ';' after expression. Semicolons are required in PukiMO.")
        return ExprStmt(expr)
    }

    private fun parseBlock(): Block {
        consume(TokenType.LEFT_BRACE, "Expected '{' at start of block")
        val stmts = parseBlockStatements()
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block")
        return Block(stmts)
    }

    private fun parseBlockStatements(): MutableList<Stmt> {
        val stmts = mutableListOf<Stmt>()
        var hadError = false

        while (!tokenBuffer.check(TokenType.RIGHT_BRACE) && !tokenBuffer.isAtEnd()) {
            try {
                stmts.add(parseStatement())
            } catch (e: ParserError) {
                hadError = true
                synchronize()
                if (tokenBuffer.check(TokenType.RIGHT_BRACE)) break
            }
        }

        if (! tokenBuffer.check(TokenType.RIGHT_BRACE) && !hadError) {
            throw errorHandler.error(tokenBuffer.peek(), "Expected '}' after block")
        }

        return stmts
    }

    private fun parseExploreStmt(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'explore'")
        val safariZoneIdent = consume(TokenType. IDENTIFIER, "Expected SafariZone variable name in 'explore'")
        consume(TokenType.RIGHT_PAREN, "Expected ')' after SafariZone variable name in 'explore'")

        context.enterControlBlock()
        val block = parseBlock()
        context.exitControlBlock()

        return ExploreStmt(safariZoneIdent, block)
    }

    // ========== Expression Parsers ==========

    private fun parseExpression(): Expr = parseAssignExpr()

    private fun parseAssignExpr(): Expr {
        val expr = parseOr()

        if (tokenBuffer.match(TokenType.ASSIGN)) {
            val equals = tokenBuffer.previous()
            val value = parseAssignExpr()
            return createAssignment(expr, equals, value)
        }

        return expr
    }

    private fun createAssignment(target: Expr, equals: Token, value: Expr): Expr {
        return when (target) {
            is VariableExpr -> {
                context.validateNotReadOnly(target. identifier, "encounter")
                AssignExpr(target, equals, value)
            }
            is PropertyAccessExpr -> AssignExpr(target, equals, value)
            is ArrayAccessExpr -> ArrayAssignExpr(target. array, target.leftBracket, target.index, value)
            else -> throw errorHandler.error(equals, "Invalid assignment target")
        }
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
        while (tokenBuffer. match(
                TokenType.LESS_THAN, TokenType.LESS_EQUAL,
                TokenType. GREATER_THAN, TokenType. GREATER_EQUAL
            )) {
            val operator = tokenBuffer.previous()
            val right = parseAdditive()
            expr = BinaryExpr(expr, operator, right)
        }
        return expr
    }

    private fun parseAdditive(): Expr {
        var expr = parseMultiplicative()
        while (tokenBuffer.match(TokenType.PLUS, TokenType. MINUS)) {
            val operator = tokenBuffer.previous()
            val right = parseMultiplicative()
            expr = BinaryExpr(expr, operator, right)
        }
        return expr
    }

    private fun parseMultiplicative(): Expr {
        var expr = parseUnary()
        while (tokenBuffer.match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType. MODULO)) {
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
                tokenBuffer.match(TokenType.DOT) -> parsePropertyAccess(expr)
                tokenBuffer.match(TokenType. ARROW) -> parseMethodCall(expr)
                tokenBuffer.match(TokenType.LEFT_BRACKET) -> parseArrayAccess(expr)
                tokenBuffer.match(TokenType.LEFT_PAREN) -> parseFunctionCall(expr)
                else -> break
            }
        }

        return expr
    }

    private fun parsePropertyAccess(expr: Expr): Expr {
        val member = consume(TokenType.IDENTIFIER, "Expected property name after '.'")

        if (tokenBuffer.check(TokenType.LEFT_PAREN)) {
            throw errorHandler.error(member, "Cannot call method with '.' operator - use '->' for methods")
        }

        return PropertyAccessExpr(expr, member)
    }

    private fun parseMethodCall(expr: Expr): Expr {
        val member = consume(TokenType.IDENTIFIER, "Expected method name after '->'")

        if (! tokenBuffer.check(TokenType. LEFT_PAREN)) {
            throw errorHandler.error(member, "Arrow operator '->' is for method calls only - use '.' for properties")
        }

        tokenBuffer.match(TokenType.LEFT_PAREN)
        val args = parseArgList()
        return CallExpr(PropertyAccessExpr(expr, member), args. positional, args.named)
    }

    private fun parseArrayAccess(expr: Expr): Expr {
        val leftBracket = tokenBuffer.previous()
        val index = parseExpression()
        consume(TokenType.RIGHT_BRACKET, "Expected ']' after array index")
        return ArrayAccessExpr(expr, leftBracket, index)
    }

    private fun parseFunctionCall(expr: Expr): Expr {
        val args = parseArgList()
        return CallExpr(expr, args. positional, args.named)
    }

    private fun parsePrimary(): Expr {
        return when {
            tokenBuffer.match(TokenType. NUMERIC_LITERAL, TokenType.STRING_LITERAL,
                TokenType.BOOLEAN_LITERAL, TokenType.NULL_LITERAL) -> {
                LiteralExpr(tokenBuffer. previous().literal)
            }
            tokenBuffer.match(TokenType.IDENTIFIER) -> VariableExpr(tokenBuffer. previous())
            tokenBuffer.match(TokenType. SAFARI_ZONE, TokenType.TEAM) -> parseConstructorCall()
            tokenBuffer.match(TokenType.LEFT_BRACKET) -> parseArrayLiteral()
            tokenBuffer.match(TokenType.LEFT_PAREN) -> parseGroupedExpression()
            else -> throw errorHandler.error(tokenBuffer.peek(), "Unexpected token")
        }
    }

    private fun parseConstructorCall(): Expr {
        val token = tokenBuffer.previous()
        consume(TokenType.LEFT_PAREN, "Expected '(' after ${token.lexeme}")
        val args = parseArgList()
        return CallExpr(VariableExpr(token), args.positional, args.named)
    }

    private fun parseArrayLiteral(): Expr {
        val leftBracket = tokenBuffer.previous()
        val elements = mutableListOf<Expr>()

        if (!tokenBuffer.check(TokenType.RIGHT_BRACKET)) {
            do {
                elements.add(parseExpression())
            } while (tokenBuffer.match(TokenType.COMMA))
        }

        consume(TokenType.RIGHT_BRACKET, "Expected ']' after array elements")
        return ArrayLiteralExpr(leftBracket, elements)
    }

    private fun parseGroupedExpression(): Expr {
        val expr = parseExpression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after expression")
        return expr
    }

    private data class ArgumentList(val positional: List<Expr>, val named: List<NamedArg>)

    private fun parseArgList(): ArgumentList {
        val positional = mutableListOf<Expr>()
        val named = mutableListOf<NamedArg>()

        if (!tokenBuffer.check(TokenType.RIGHT_PAREN)) {
            do {
                parseArgument(positional, named)
            } while (tokenBuffer.match(TokenType.COMMA))
        }

        consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments")
        return ArgumentList(positional, named)
    }

    private fun parseArgument(positional: MutableList<Expr>, named: MutableList<NamedArg>) {
        if (isNamedArgument()) {
            named.add(parseNamedArgument())
        } else {
            positional.add(parseExpression())
        }
    }


    private fun parseNamedArgument(): NamedArg {
        val name = tokenBuffer. advance()
        consume(TokenType.ASSIGN, "Expected '=' after argument name")
        val value = parseExpression()
        return NamedArg(name, value)
    }

    private fun consume(type: TokenType, message: String): Token {
        if (tokenBuffer.check(type)) return tokenBuffer.advance()
        throw errorHandler.error(tokenBuffer.peek(), message)
    }


    private fun synchronize() {
        addDelimiterHints()
        skipToNextStatement()
    }

    private fun addDelimiterHints() {
        try {
            val errorToken = tokenBuffer.peek()
            val previousTokenType = tokenBuffer.previous().type

            when (previousTokenType) {
                TokenType.LEFT_PAREN ->
                    errorHandler.appendHintToLast(errorToken, "Unclosed '(' - missing ')'")
                TokenType.LEFT_BRACE ->
                    errorHandler. appendHintToLast(errorToken, "Unclosed '{' - missing '}'")
                else -> { }
            }
        } catch (_: Exception) { }
    }

    private fun skipToNextStatement() {
        tokenBuffer.advance()

        while (! tokenBuffer.isAtEnd()) {
            if (isStatementBoundary()) return
            if (isStatementKeyword()) return
            tokenBuffer.advance()
        }
    }

    private fun isStatementBoundary(): Boolean {
        return tokenBuffer.previous().type == TokenType.SEMICOLON
    }

    private fun isStatementKeyword(): Boolean {
        return when (tokenBuffer.peek(). type) {
            TokenType.VAR_KEYWORD, TokenType.DEFINE_KEYWORD, TokenType.IF_KEYWORD,
            TokenType.WHILE_KEYWORD, TokenType.FOR_KEYWORD, TokenType.PRINT_KEYWORD,
            TokenType. BREAK_KEYWORD, TokenType.CONTINUE_KEYWORD, TokenType.EXPLORE_KEYWORD -> true
            else -> false
        }
    }

    private fun isNamedArgument(): Boolean {
        return tokenBuffer.check(TokenType. IDENTIFIER) && tokenBuffer.peekNext().type == TokenType.ASSIGN
    }

}