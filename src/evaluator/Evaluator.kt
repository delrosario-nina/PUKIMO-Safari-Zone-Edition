package evaluator

import lexer.*
import parser.*


class RuntimeError(
    val token: Token,
    errorMessage: String
) : RuntimeException("[line ${token.lineNumber}] Runtime error: $errorMessage")

class Evaluator : AstVisitor<Any?> {
    fun evaluate(node: AstNode): Any? {
        return node.accept(this)
}
    override fun visitProgram(program: Program): Any? {
        var lastValue: Any? = null
        for (stmt in program.stmtList) {
            lastValue = stmt.accept(this)
        }
        return lastValue
    }

    override fun visitExprStmt(stmt: ExprStmt): Any? {
        return stmt.expression.accept(this)
    }

    override fun visitPrintStmt(stmt: PrintStmt): Any? {
        val value = stmt.expression.accept(this)
        println(stringify(value))
        return null
    }

    override fun visitVarDeclStmt(stmt: VarDeclStmt): Any? {
        val value = stmt.expression.accept(this)
        // In a real interpreter, you'd store this in an environment/symbol table
        // For now, just return the value
        return value
    }

    override fun visitBlock(block: Block): Any? {
        var lastValue: Any? = null
        for (stmt in block.stmtList) {
            lastValue = stmt.accept(this)
        }
        return lastValue
    }

    override fun visitIfStmt(stmt: IfStmt): Any? {
        val condition = stmt.expression.accept(this)

        return if (isTruthy(condition)) {
            stmt.thenBlock.accept(this)
        } else if (stmt.elseBlock != null) {
            stmt.elseBlock.accept(this)
        } else {
            null
        }
    }

    override fun visitDefineStmt(stmt: DefineStmt): Any? {
        // In a real interpreter, you'd create a function object and store it
        // For now, just acknowledge it
        return null
    }

    override fun visitExploreStmt(stmt: ExploreStmt): Any? {
        // Evaluate the target
        stmt.target.accept(this)
        // Execute the block
        return stmt.block.accept(this)
    }

    override fun visitThrowBallStmt(stmt: ThrowBallStmt): Any? {
        return stmt.expression.accept(this)
    }

    override fun visitRunStmt(stmt: RunStmt): Any? {
        // In a real interpreter, this would continue a loop
        return null
    }

    override fun visitLiteralExpr(expr: LiteralExpr): Any? {
        return expr.value
    }

    override fun visitVariableExpr(expr: VariableExpr): Any? {
        // In a real interpreter, look up the variable in the environment
        // For now, throw an error
        throw RuntimeError(
            expr.identifier,
            "Undefined variable '${expr.identifier.lexeme}'."
        )
    }

    override fun visitUnaryExpr(expr: UnaryExpr): Any? {
        val right = expr.right.accept(this)

        return when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                when (right) {
                    is Double -> -right
                    is Int -> -right
                    else -> 0 // Should never reach here
                }
            }
            TokenType.NOT -> !isTruthy(right)
            else -> throw RuntimeError(
                expr.operator,
                "Unknown unary operator '${expr.operator.lexeme}'."
            )
        }
    }

    override fun visitBinaryExpr(expr: BinaryExpr): Any? {
        val left = expr.left.accept(this)
        val right = expr.right.accept(this)

        return when (expr.operator.type) {
            // Arithmetic operators
            TokenType.PLUS -> {
                when {
                    left is Double && right is Double -> left + right
                    left is Int && right is Int -> left + right
                    left is Double && right is Int -> left + right
                    left is Int && right is Double -> left + right
                    left is String && right is String -> left + right
                    else -> throw RuntimeError(
                        expr.operator,
                        "Operands must be two numbers or two strings."
                    )
                }
            }
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                when {
                    left is Double && right is Double -> left - right
                    left is Int && right is Int -> left - right
                    left is Double && right is Int -> left - right
                    left is Int && right is Double -> left - right
                    else -> 0 // Should never reach here due to check
                }
            }
            TokenType.MULTIPLY -> {
                checkNumberOperands(expr.operator, left, right)
                when {
                    left is Double && right is Double -> left * right
                    left is Int && right is Int -> left * right
                    left is Double && right is Int -> left * right
                    left is Int && right is Double -> left * right
                    else -> 0
                }
            }
            TokenType.DIVIDE -> {
                checkNumberOperands(expr.operator, left, right)
                val divisor = when (right) {
                    is Double -> right
                    is Int -> right.toDouble()
                    else -> 0.0
                }
                if (divisor == 0.0) {
                    throw RuntimeError(expr.operator, "Division by zero.")
                }
                when (left) {
                    is Double -> left / divisor
                    is Int -> left / divisor
                    else -> 0.0
                }
            }
            TokenType.MODULO -> {
                checkNumberOperands(expr.operator, left, right)
                when {
                    left is Int && right is Int -> {
                        if (right == 0) {
                            throw RuntimeError(expr.operator, "Division by zero.")
                        }
                        left % right
                    }
                    else -> throw RuntimeError(
                        expr.operator,
                        "Modulo operator requires integer operands."
                    )
                }
            }

            // Comparison operators
            TokenType.GREATER_THAN -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) > toDouble(right)
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) >= toDouble(right)
            }
            TokenType.LESS_THAN -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) < toDouble(right)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) <= toDouble(right)
            }

            // Equality operators
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            TokenType.NOT_EQUAL -> !isEqual(left, right)

            // Logical operators
            TokenType.AND -> {
                // Short-circuit evaluation
                if (!isTruthy(left)) {
                    false
                } else {
                    isTruthy(right)
                }
            }
            TokenType.OR -> {
                // Short-circuit evaluation
                if (isTruthy(left)) {
                    true
                } else {
                    isTruthy(right)
                }
            }

            else -> throw RuntimeError(
                expr.operator,
                "Unknown binary operator '${expr.operator.lexeme}'."
            )
        }
    }

    override fun visitAssignExpr(expr: AssignExpr): Any? {
        val value = expr.value.accept(this)
        // In a real interpreter, store the value in the environment
        // For now, just return it
        return value
    }

    override fun visitCallExpr(expr: CallExpr): Any? {
        // In a real interpreter, evaluate the callee and arguments
        // then invoke the function
        throw RuntimeError(
            Token(TokenType.EOF, "", null, 0),
            "Function calls not yet implemented in evaluator."
        )
    }

    override fun visitPropertyAccessExpr(expr: PropertyAccessExpr): Any? {
        // In a real interpreter, evaluate the object and access its property
        throw RuntimeError(
            expr.identifier,
            "Property access not yet implemented in evaluator."
        )
    }

    // ----------------------
    // HELPER FUNCTIONS
    // ----------------------

    /**
     * Determines if a value is "truthy"
     * false and null are falsey, everything else is truthy
     */
    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }

    /**
     * Checks if two values are equal
     */
    private fun isEqual(a: Any?, b: Any?): Boolean {
        if (a == null && b == null) return true
        if (a == null) return false
        return a == b
    }

    /**
     * Converts a value to a string for printing
     * Made public so Main.kt can use it
     */
    fun stringify(value: Any?): String {
        if (value == null) return "nil"

        // Format numbers nicely
        if (value is Double) {
            val text = value.toString()
            if (text.endsWith(".0")) {
                return text.substring(0, text.length - 2)
            }
            return text
        }

        // Booleans as lowercase
        if (value is Boolean) {
            return value.toString().lowercase()
        }

        return value.toString()
    }

    /**
     * Converts a number to Double for comparison
     */
    private fun toDouble(value: Any?): Double {
        return when (value) {
            is Double -> value
            is Int -> value.toDouble()
            else -> throw IllegalArgumentException("Cannot convert to double")
        }
    }

    /**
     * Type checking for unary operators
     */
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double || operand is Int) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    /**
     * Type checking for binary operators
     */
    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if ((left is Double || left is Int) && (right is Double || right is Int)) return
        throw RuntimeError(operator, "Operands must be numbers.")
    }
}
