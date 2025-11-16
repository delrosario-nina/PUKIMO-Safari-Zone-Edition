package parser

// Visitor interface
interface AstVisitor<R> {
    fun visitProgram(node: Program): R
    fun visitIfStmt(node: IfStmt): R
    fun visitVarDeclStmt(node: VarDeclStmt): R
    fun visitExprStmt(node: ExprStmt): R
    fun visitPrintStmt(node: PrintStmt): R
    fun visitBlock(node: Block): R
    fun visitRunStmt(node: RunStmt): R
    fun visitExploreStmt(node: ExploreStmt): R
    fun visitDefineStmt(node: DefineStmt): R
    fun visitThrowBallStmt(node: ThrowBallStmt): R
    fun visitBinaryExpr(node: BinaryExpr): R
    fun visitUnaryExpr(node: UnaryExpr): R
    fun visitLiteralExpr(node: LiteralExpr): R
    fun visitVariableExpr(node: VariableExpr): R
    fun visitAssignExpr(node: AssignExpr): R
    fun visitCallExpr(node: CallExpr): R
    fun visitPropertyAccessExpr(node: PropertyAccessExpr): R
}