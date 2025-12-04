package parser

import lexer.*
import evaluator.Environment

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

data class ExploreStmt(val safariZoneVar: Token, val block: Block) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitExploreStmt(this)
}

data class DefineStmt(
    val name: Token,
    val paramList: List<Token>,
    val block: Block
) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitDefineStmt(this)
}

data class ReturnStmt(val keyword: Token, val value: Expr?) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitReturnStmt(this)
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

data class FunctionObject(
    val name: Token,
    val parameters: List<Token>,
    val body: Block,
    val closure: Environment
)

data class WhileStmt(
    val keyword: Token,
    val condition: Expr,
    val body: Block
) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitWhileStmt(this)
}

data class ForStmt(
    val keyword: Token,
    val variable: Token,
    val start: Expr,
    val end: Expr?,
    val body: Block
) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitForStmt(this)
}

data class BreakStmt(val keyword: Token) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor. visitBreakStmt(this)
}

data class ContinueStmt(val keyword: Token) : Stmt() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitContinueStmt(this)
}

// ========== Array Expressions ==========

data class ArrayLiteralExpr(
    val leftBracket: Token,
    val elements: List<Expr>
) : Expr() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitArrayLiteralExpr(this)
}

data class ArrayAccessExpr(
    val array: Expr,
    val leftBracket: Token,
    val index: Expr
) : Expr() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitArrayAccessExpr(this)
}

data class ArrayAssignExpr(
    val array: Expr,
    val leftBracket: Token,
    val index: Expr,
    val value: Expr
) : Expr() {
    override fun <R> accept(visitor: AstVisitor<R>): R = visitor.visitArrayAssignExpr(this)
}