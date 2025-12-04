package evaluator

import lexer.*
import parser.*

/**
 * Evaluator - The runtime evaluation engine for PukiMO.
 * Delegates to specialized evaluators for different language features.
 */
class Evaluator : AstVisitor<Any? > {
    private val errorHandler = EvaluatorErrorHandler()
    private var environment = Environment(errorHandler = errorHandler)
    private var inUserFunction = false
    private var isReplMode = false

    companion object {
        val ENCOUNTER_TOKEN = Token(TokenType.IDENTIFIER, "encounter", null, -1)
        val POKEMON_TOKEN = Token(TokenType. IDENTIFIER, "pokemon", null, -1)
        val EOF_TOKEN = Token(TokenType. EOF, "", null, -1)
    }

    // Specialized evaluators
    private val arithmeticEvaluator = ArithmeticEvaluator(errorHandler)
    private val safariZoneObjects = SafariZoneObjects(errorHandler, this)
    private val builtinFunctions = BuiltinFunctions(errorHandler)

    private val expressionEvaluator = ExpressionEvaluator(this)
    private val statementEvaluator = StatementEvaluator(this)
    private val loopEvaluator = LoopEvaluator(this)
    private val functionEvaluator = FunctionEvaluator(this)
    private val arrayStringEvaluator = ArrayStringEvaluator(this)
    private val valueHelper = ValueHelper()

    init {
        registerBuiltinConstructors()
    }

    data class BuiltinConstructorMarker(val name: String)

    private fun registerBuiltinConstructors() {
        val constructors = listOf("SafariZone", "Team")
        for (name in constructors) {
            environment.define(
                Token(TokenType.IDENTIFIER, name, null, 0),
                BuiltinConstructorMarker(name)
            )
        }
    }

    // Getters for delegated evaluators
    fun getErrorHandler(): EvaluatorErrorHandler = errorHandler
    fun getEnvironment(): Environment = environment
    fun setEnvironment(env: Environment) { environment = env }
    fun isInUserFunction(): Boolean = inUserFunction
    fun setInUserFunction(value: Boolean) { inUserFunction = value }
    fun getArithmeticEvaluator(): ArithmeticEvaluator = arithmeticEvaluator
    fun getSafariZoneObjects(): SafariZoneObjects = safariZoneObjects
    fun getBuiltinFunctions(): BuiltinFunctions = builtinFunctions

    fun evaluate(node: AstNode, isReplMode: Boolean = false): Any? {
        this.isReplMode = isReplMode
        return node.accept(this)
    }

    // Program and basic visitors
    override fun visitProgram(program: Program): Any? {
        var lastValue: Any? = null
        for (stmt in program.stmtList) {
            lastValue = stmt.accept(this)
        }
        return lastValue
    }

    // Delegate to StatementEvaluator
    override fun visitExprStmt(stmt: ExprStmt): Any? =
        statementEvaluator.visitExprStmt(stmt, isReplMode)

    override fun visitPrintStmt(stmt: PrintStmt): Any? =
        statementEvaluator.visitPrintStmt(stmt)

    override fun visitVarDeclStmt(stmt: VarDeclStmt): Any? =
        statementEvaluator.visitVarDeclStmt(stmt)

    override fun visitBlock(block: Block): Any? =
        statementEvaluator. visitBlock(block)

    override fun visitIfStmt(stmt: IfStmt): Any? =
        statementEvaluator.visitIfStmt(stmt)

    override fun visitDefineStmt(stmt: DefineStmt): Any? =
        functionEvaluator.visitDefineStmt(stmt)

    override fun visitReturnStmt(stmt: ReturnStmt): Any? =
        functionEvaluator.visitReturnStmt(stmt)

    override fun visitExploreStmt(stmt: ExploreStmt): Any? =
        statementEvaluator.visitExploreStmt(stmt)

    // Delegate to LoopEvaluator
    override fun visitWhileStmt(stmt: WhileStmt): Any? =
        loopEvaluator. visitWhileStmt(stmt)

    override fun visitForStmt(stmt: ForStmt): Any? =
        loopEvaluator.visitForStmt(stmt)

    override fun visitBreakStmt(stmt: BreakStmt): Any? =
        loopEvaluator.visitBreakStmt(stmt)

    override fun visitContinueStmt(stmt: ContinueStmt): Any?  =
        loopEvaluator.visitContinueStmt(stmt)

    // Delegate to ExpressionEvaluator
    override fun visitLiteralExpr(expr: LiteralExpr): Any? =
        expressionEvaluator.visitLiteralExpr(expr)

    override fun visitVariableExpr(expr: VariableExpr): Any? =
        expressionEvaluator.visitVariableExpr(expr)

    override fun visitUnaryExpr(expr: UnaryExpr): Any? =
        expressionEvaluator. visitUnaryExpr(expr)

    override fun visitBinaryExpr(expr: BinaryExpr): Any? =
        expressionEvaluator. visitBinaryExpr(expr)

    override fun visitAssignExpr(expr: AssignExpr): Any? =
        expressionEvaluator. visitAssignExpr(expr)

    override fun visitCallExpr(expr: CallExpr): Any? =
        functionEvaluator.visitCallExpr(expr)

    override fun visitPropertyAccessExpr(expr: PropertyAccessExpr): Any? =
        expressionEvaluator.visitPropertyAccessExpr(expr)

    // Delegate to ArrayStringEvaluator
    override fun visitArrayLiteralExpr(expr: ArrayLiteralExpr): Any?  =
        arrayStringEvaluator.visitArrayLiteralExpr(expr)

    override fun visitArrayAccessExpr(expr: ArrayAccessExpr): Any? =
        arrayStringEvaluator. visitArrayAccessExpr(expr)

    override fun visitArrayAssignExpr(expr: ArrayAssignExpr): Any? =
        arrayStringEvaluator.visitArrayAssignExpr(expr)

    // Helper methods
    fun isTruthy(value: Any? ): Boolean = valueHelper.isTruthy(value)
    fun stringify(value: Any?): String = valueHelper.stringify(value)

    fun executeBlock(statements: List<Stmt>, blockEnvironment: Environment): Any? =
        statementEvaluator.executeBlock(statements, blockEnvironment)
}