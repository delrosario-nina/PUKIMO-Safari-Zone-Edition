package parser

class AstPrinter : AstVisitor<String> {
    fun print(node: AstNode): String = node.accept(this)

    fun printToConsole(node: AstNode) {
        println(print(node))
    }

    override fun visitProgram(node: Program): String =
        node.stmtList.joinToString("\n") { it.accept(this) }

    override fun visitVarDeclStmt(node: VarDeclStmt): String =
        "(var ${node.identifier.lexeme} = ${node.expression.accept(this)})"

    override fun visitDefineStmt(node: DefineStmt): String {
        val params = node.paramList.joinToString(", ") { it.lexeme }
        return "(define ${node.name.lexeme}($params) ${node.block.accept(this)})"
    }

    override fun visitPrintStmt(node: PrintStmt): String =
        "(print ${node.expression.accept(this)})"

    override fun visitExprStmt(node: ExprStmt): String =
        node.expression.accept(this)

    override fun visitIfStmt(node: IfStmt): String {
        val cond = node.expression.accept(this)
        val thenB = node.thenBlock.accept(this)
        val elseB = node.elseBlock?.accept(this) ?: ""
        return "(if $cond then $thenB${if (elseB.isNotEmpty()) " else $elseB" else ""})"
    }

    override fun visitBlock(node: Block): String =
        "(block ${node.stmtList.joinToString(" ") { it.accept(this) }})"

    override fun visitRunStmt(node: RunStmt): String =
        "(${node.token.lexeme})"

    override fun visitExploreStmt(node: ExploreStmt): String =
        "(explore ${node.target.accept(this)} ${node.block.accept(this)})"

    override fun visitThrowBallStmt(node: ThrowBallStmt): String =
        "(throwBall ${node.expression.accept(this)})"

    override fun visitBinaryExpr(node: BinaryExpr): String =
        parenthesize(node.operator.lexeme, node.left, node.right)

    override fun visitUnaryExpr(node: UnaryExpr): String =
        parenthesize(node.operator.lexeme, node.right)

    override fun visitLiteralExpr(node: LiteralExpr): String =
        when (node.value) {
            is String -> "\"${node.value}\""  // Add quotes around strings
            else -> node.value?.toString() ?: "null"
        }


    override fun visitVariableExpr(node: VariableExpr): String =
        node.identifier.lexeme

    override fun visitAssignExpr(node: AssignExpr): String =
        parenthesize("=", node.target, node.value)

    override fun visitPropertyAccessExpr(node: PropertyAccessExpr): String =
        parenthesize(".", node.primaryWithSuffixes, VariableExpr(node.identifier))

    override fun visitCallExpr(node: CallExpr): String {
        val positionalArgs = node.args.joinToString(", ") { it.accept(this) }
        val namedArgs = node.namedArgs.joinToString(", ") {
            "${it.name.lexeme}=${it.value.accept(this)}"
        }

        val allArgs = listOfNotNull(
            positionalArgs.takeIf { it.isNotEmpty() },
            namedArgs.takeIf { it.isNotEmpty() }
        ).joinToString(", ")

        return when (node.callee) {
            is PropertyAccessExpr -> {
                val obj = node.callee.primaryWithSuffixes.accept(this)
                val method = node.callee.identifier.lexeme
                "(-> $obj $method($allArgs))"
            }
            else -> "(call ${node.callee.accept(this)} ($allArgs))"
        }
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ").append(expr.accept(this))
        }
        builder.append(")")
        return builder.toString()
    }
}
