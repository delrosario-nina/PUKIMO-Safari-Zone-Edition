package parser

import lexer.Token

sealed class AstNode {
    abstract fun <R> accept(visitor: AstVisitor<R>): R
}

data class Program(val stmtList: List<Stmt>): AstNode() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitProgram(this)
}

// Statements
sealed class Stmt: AstNode()

data class IfStmt(val expression: Expr, val thenBlock: Block, val elseBlock: Block?): Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitIfStmt(this)
}
data class VarDeclStmt(val identifier: Token, val expression: Expr) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitVarDeclStmt(this)
}
data class ExprStmt(val expression: Expr) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitExprStmt(this)
}
data class PrintStmt(val expression: Expr) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitPrintStmt(this)
}
data class Block(val stmtList: List<Stmt>) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitBlock(this)
}
data class RunStmt(val token: Token) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitRunStmt(this)
}
data class ExploreStmt(val target: Expr, val block: Block) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitExploreStmt(this)
}
data class DefineStmt(val name: Token, val paramList: List<Token>, val block: Block) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitDefineStmt(this)
}
data class ThrowBallStmt(val expression: Expr) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitThrowBallStmt(this)
}

// Expressions
sealed class Expr: AstNode()

data class BinaryExpr(val left: Expr, val operator: Token, val right: Expr) : Expr() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitBinaryExpr(this)
}
data class UnaryExpr(val operator: Token, val right: Expr) : Expr() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitUnaryExpr(this)
}

data class LiteralExpr(val value: Any?) : Expr() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitLiteralExpr(this)
}
data class VariableExpr(val identifier: Token) : Expr() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitVariableExpr(this)
}
data class AssignExpr(val target: Expr, val equals: Token, val value: Expr) : Expr() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitAssignExpr(this)
}
data class NamedArg(val name: Token, val value: Expr)

data class CallExpr(val callee: Expr, val args: List<Expr>, val namedArgs: List<NamedArg> = emptyList()) : Expr() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitCallExpr(this)
}

data class PropertyAccessExpr(val primaryWithSuffixes: Expr, val identifier: Token) : Expr() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitPropertyAccessExpr(this)
}