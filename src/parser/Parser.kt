package parser

import lexer.*

/*
 * Recursive descent parser for the PukiMO language.
 * Implements a precedence-climbing algorithm for expression parsing.
 * Follows the language grammar specified in README.md.
 */
class Parser(
    private val tokens: List<Token>,
    private val tokenBuffer: TokenBuffer = TokenBuffer(tokens),
    private val context: ParsingContext = ParsingContext(),
    private val errorHandler: ErrorHandler = ErrorHandler(),
    private val replMode: Boolean = false  // REPL mode makes semicolons optional
) {

    fun parse(): Program {
        val stmtList = mutableListOf<Stmt>()
        while (!tokenBuffer.isAtEnd()) {
            stmtList.add(
                if (tokenBuffer.check(TokenType.IF_KEYWORD)) parseIfStmt()
                else parseNonIfStmt()
            )
        }
        return Program(stmtList)
    }

    private fun parseNonIfStmt(): Stmt {
        return when {
            tokenBuffer.match(TokenType.VAR_KEYWORD) -> parseVarDeclStmt()
            tokenBuffer.match(TokenType.PRINT_KEYWORD) -> parsePrintStmt()
            tokenBuffer.match(TokenType.RUN_KEYWORD) -> parseRunStmt()
            tokenBuffer.match(TokenType.EXPLORE_KEYWORD) -> parseExploreStmt()
            tokenBuffer.match(TokenType.THROWBALL_KEYWORD) -> parseThrowBallStmt()
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
        val identifier = consume(TokenType.IDENTIFIER, "Expected variable name.")

        val expr = if (tokenBuffer.match(TokenType.ASSIGN)) {
            parseExpression()
        } else {
            LiteralExpr(null)
        }

        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.")
        return VarDeclStmt(identifier, expr)
    }

    private fun parseDefineStmt(): Stmt {
        // DEFINE_KEYWORD already consumed by match() in parseNonIfStmt
        val name = consume(TokenType.IDENTIFIER, "Expected function name after 'define'")

        val params = mutableListOf<Token>()
        if (tokenBuffer.match(TokenType.LEFT_PAREN)) {
            if (!tokenBuffer.check(TokenType.RIGHT_PAREN)) {
                do {
                    params.add(consume(TokenType.IDENTIFIER, "Expected parameter name"))
                } while (tokenBuffer.match(TokenType.COMMA))
            }
            consume(TokenType.RIGHT_PAREN, "Expected ')' after function parameters")
        }

        val body = parseBlock()
        return DefineStmt(name, params, body)
    }

    private fun parseThrowBallStmt(): Stmt {
        // THROWBALL_KEYWORD already consumed by match() in parseNonIfStmt
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'throwBall'")
        val target = parseExpression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after throwBall target")
        consume(TokenType.SEMICOLON, "Expected ';' after throwBall statement.")
        return ThrowBallStmt(target)
    }

    private fun parseReturnStmt(): Stmt {
        val keyword = tokenBuffer.previous()  // Get the already-consumed RETURN_KEYWORD token

        // Check if there's a value to return
        val value = if (tokenBuffer.check(TokenType.SEMICOLON)) {
            null  // return with no value
        } else {
            parseExpression()
        }
        
        consume(TokenType.SEMICOLON, "Expected ';' after return statement.")
        return ReturnStmt(keyword, value)
    }

    private fun parsePrintStmt(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'print'")
        val expr = parseExpression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after print expression")
        consume(TokenType.SEMICOLON, "Expected ';' after print statement")
        return PrintStmt(expr)
    }

    private fun parseExprStmt(optionalSemicolon: Boolean = false): Stmt {
        val expr = parseExpression()

        // In REPL mode, semicolons are optional for expression statements
        val requireSemicolon = !replMode && !optionalSemicolon

        if (requireSemicolon || tokenBuffer.check(TokenType.SEMICOLON)) {
            consume(TokenType.SEMICOLON, "Expected ';' after expression.")
        }
        if (expr is VariableExpr) {
            throw errorHandler.error(expr.identifier, "Unexpected standalone identifier '${expr.identifier.lexeme}'")
        }
        return ExprStmt(expr)
    }

    private fun parseBlock(): Block {
        consume(TokenType.LEFT_BRACE, "Expected '{' at start of block")
        val stmts = mutableListOf<Stmt>()

        while (!tokenBuffer.check(TokenType.RIGHT_BRACE) && !tokenBuffer.isAtEnd()) {
            stmts.add(
                if (tokenBuffer.check(TokenType.IF_KEYWORD)) parseIfStmt()
                else parseNonIfStmt()
            )
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' after block")
        return Block(stmts)
    }

    private fun parseRunStmt(): Stmt {
        val runToken = tokenBuffer.previous()  // Get the already-consumed RUN_KEYWORD token
        context.validateRunStatement(runToken)
        consume(TokenType.SEMICOLON, "Expected ';' after 'run' statement.")
        return RunStmt(runToken)
    }

    private fun parseExploreStmt(): Stmt {
        val target = parseExpression()
        context.enterControlBlock()
        val block = parseBlock()
        context.exitControlBlock()
        return ExploreStmt(target, block)
    }

    private fun parseExpression(): Expr = parseAssignExpr()

    private fun parseAssignExpr(): Expr {
        val expr = parseOr()

        if (tokenBuffer.match(TokenType.ASSIGN)) {
            val equals = tokenBuffer.previous()
            val value = parseAssignExpr()

            if (expr is VariableExpr || expr is PropertyAccessExpr) {
                return AssignExpr(expr, equals, value)
            } else {
                throw errorHandler.error(equals, "Invalid assignment target")
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
        val expr: Expr
        if (tokenBuffer.match(TokenType.NOT, TokenType.MINUS)) {
            val operator = tokenBuffer.previous()
            val right = parseUnary()
            expr = UnaryExpr(operator, right)
        } else {
            expr = parsePrimaryWithSuffixes()
        }
        return expr
    }


    private fun parsePrimaryWithSuffixes(): Expr {
        var expr = parsePrimary()
        while (true) {
            expr = when {
                tokenBuffer.match(TokenType.DOT) -> {
                    val member = consume(TokenType.IDENTIFIER, "Expected property name after '.'")
                    // Dot operator is for properties only - no parentheses allowed
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
                    // Arrow operator must be followed by parentheses for method calls
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

            else -> throw errorHandler.error(tokenBuffer.peek(), "Expected primary expression")
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
