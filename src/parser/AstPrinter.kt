package parser

import lexer.Token

class AstPrinter : AstVisitor<String> {
    fun print(node: AstNode): String = node.accept(this)

    fun printToConsole(node: AstNode) {
        println(print(node))
    }

    override fun visitProgram(program: Program): String =
        program.stmtList.joinToString("\n") { it.accept(this) }

    override fun visitVarDeclStmt(stmt: VarDeclStmt): String =
        "(var ${stmt.identifier.lexeme} = ${stmt.expression.accept(this)})"

    // ✅ FIXED: No types anymore (dynamic typing)
    override fun visitDefineStmt(stmt: DefineStmt): String {
        val params = stmt.paramList.joinToString(", ") { it.lexeme }  // Just names
        return "(define ${stmt.name.lexeme}($params) ${stmt.block.accept(this)})"
    }

    override fun visitPrintStmt(stmt: PrintStmt): String =
        "(print ${stmt.expression.accept(this)})"

    override fun visitExprStmt(stmt: ExprStmt): String =
        stmt.expression.accept(this)

    override fun visitIfStmt(stmt: IfStmt): String {
        val cond = stmt.expression.accept(this)
        val thenB = stmt.thenBlock.accept(this)
        val elseB = stmt.elseBlock?.accept(this) ?: ""
        return "(if $cond then $thenB${if (elseB.isNotEmpty()) " else $elseB" else ""})"
    }

    override fun visitBlock(block: Block): String =
        "(block ${block.stmtList.joinToString(" ") { it.accept(this) }})"

    override fun visitWhileStmt(stmt: WhileStmt): String =
        "(while ${stmt.condition.accept(this)} ${stmt.body.accept(this)})"

    override fun visitForStmt(stmt: ForStmt): String =
        if (stmt.end != null) {
            "(for ${stmt.variable.lexeme} in ${stmt. start.accept(this)} to ${stmt.end.accept(this)} ${stmt.body.accept(this)})"
        } else {
            "(for ${stmt.variable. lexeme} in ${stmt.start.accept(this)} ${stmt. body.accept(this)})"
        }

    override fun visitBreakStmt(stmt: BreakStmt): String =
        "(break)"

    override fun visitContinueStmt(stmt: ContinueStmt): String =
        "(continue)"

    override fun visitExploreStmt(stmt: ExploreStmt): String =
        "(explore ${stmt.safariZoneVar.lexeme} ${stmt.block.accept(this)})"

    override fun visitReturnStmt(stmt: ReturnStmt): String =
        if (stmt.value != null) "(return ${stmt.value.accept(this)})" else "(return)"

    // ========== Expressions ==========

    override fun visitBinaryExpr(expr: BinaryExpr): String =
        parenthesize(expr. operator.lexeme, expr.left, expr.right)

    override fun visitUnaryExpr(expr: UnaryExpr): String =
        parenthesize(expr. operator.lexeme, expr.right)

    override fun visitLiteralExpr(expr: LiteralExpr): String =
        when (expr.value) {
            is String -> "\"${expr.value}\""
            else -> expr.value?. toString() ?: "null"
        }

    override fun visitVariableExpr(expr: VariableExpr): String =
        expr.identifier.lexeme

    override fun visitAssignExpr(expr: AssignExpr): String =
        parenthesize("=", expr.target, expr.value)

    override fun visitPropertyAccessExpr(expr: PropertyAccessExpr): String =
        parenthesize(".", expr.primaryWithSuffixes, VariableExpr(expr.identifier))

    override fun visitArrayLiteralExpr(expr: ArrayLiteralExpr): String {
        val elements = expr.elements. joinToString(", ") { it.accept(this) }
        return "[$elements]"
    }

    override fun visitArrayAccessExpr(expr: ArrayAccessExpr): String =
        "([] ${expr.array.accept(this)} ${expr.index.accept(this)})"

    override fun visitArrayAssignExpr(expr: ArrayAssignExpr): String =
        "([]= ${expr.array.accept(this)} ${expr. index.accept(this)} ${expr. value.accept(this)})"

    override fun visitCallExpr(expr: CallExpr): String {
        val positionalArgs = expr.args.joinToString(", ") { it. accept(this) }
        val namedArgs = expr.namedArgs.joinToString(", ") {
            "${it.name.lexeme}=${it.value.accept(this)}"
        }

        val allArgs = listOfNotNull(
            positionalArgs.takeIf { it. isNotEmpty() },
            namedArgs.takeIf { it.isNotEmpty() }
        ).joinToString(", ")

        return when (expr.callee) {
            is PropertyAccessExpr -> {
                val obj = expr.callee.primaryWithSuffixes.accept(this)
                val method = expr.callee.identifier.lexeme
                "(-> $obj $method($allArgs))"
            }
            else -> "(call ${expr.callee.accept(this)} ($allArgs))"
        }
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ").append(expr.accept(this))
        }
        builder. append(")")
        return builder. toString()
    }
}