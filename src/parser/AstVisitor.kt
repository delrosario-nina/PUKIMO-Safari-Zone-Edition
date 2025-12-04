package parser

interface AstVisitor<R> {
    // Statements
    fun visitProgram(program: Program): R
    fun visitExprStmt(stmt: ExprStmt): R
    fun visitPrintStmt(stmt: PrintStmt): R
    fun visitVarDeclStmt(stmt: VarDeclStmt): R
    fun visitBlock(block: Block): R
    fun visitIfStmt(stmt: IfStmt): R
    fun visitWhileStmt(stmt: WhileStmt): R
    fun visitForStmt(stmt: ForStmt): R
    fun visitBreakStmt(stmt: BreakStmt): R
    fun visitContinueStmt(stmt: ContinueStmt): R
    fun visitDefineStmt(stmt: DefineStmt): R
    fun visitExploreStmt(stmt: ExploreStmt): R
    fun visitReturnStmt(stmt: ReturnStmt): R

    // Expressions
    fun visitLiteralExpr(expr: LiteralExpr): R
    fun visitVariableExpr(expr: VariableExpr): R
    fun visitUnaryExpr(expr: UnaryExpr): R
    fun visitBinaryExpr(expr: BinaryExpr): R
    fun visitAssignExpr(expr: AssignExpr): R
    fun visitCallExpr(expr: CallExpr): R
    fun visitPropertyAccessExpr(expr: PropertyAccessExpr): R
    fun visitArrayLiteralExpr(expr: ArrayLiteralExpr): R
    fun visitArrayAccessExpr(expr: ArrayAccessExpr): R
    fun visitArrayAssignExpr(expr: ArrayAssignExpr): R
}