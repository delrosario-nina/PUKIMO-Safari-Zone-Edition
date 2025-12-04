package evaluator

import lexer.*
import parser.*

/**
 * Evaluator - The runtime evaluation engine for PukiMO.
 *
 * This class implements the Visitor pattern to traverse and execute the Abstract
 * Syntax Tree (AST) produced by the parser. It maintains the runtime environment
 * (variable bindings) and evaluates expressions and statements.
 *
 * Key responsibilities:
 * - Expression evaluation (arithmetic, logical, comparison, arrays)
 * - Statement execution (variable declarations, control flow, loops)
 * - Function calls, returns, and closures
 * - Object method calls (SafariZone, Team, PokemonCollection)
 * - Domain-specific constructs (explore statement, encounter variable, run keyword)
 * - Built-in function evaluation (readString, readInt, length, contains, concat, push)
 * - Error reporting and validation
 * - Environment management (scoping, closures)
 *
 * Built-in types:
 * - Int: Integer numbers
 * - String: Text strings
 * - Boolean: true/false
 * - Null: Absence of value
 * - MutableList<Any?>: Dynamically-typed arrays
 * - SafariZoneObject: Safari Zone game objects
 * - TeamObject: Pokémon team objects
 * - PokemonCollection: Pokémon collection manager
 * - FunctionObject: User-defined functions with closures
 * - BuiltinConstructorMarker: Marker for built-in constructors
 *
 * Domain-specific features:
 * - explore statement: Iterates through SafariZone encounters
 * - encounter: Contextual keyword (only valid inside explore blocks)
 * - run: Early exit from explore (throws RunException)
 */
class Evaluator : AstVisitor<Any? > {
    private val errorHandler = EvaluatorErrorHandler()
    private var environment = Environment(errorHandler = errorHandler)
    private var inUserFunction = false
    private var isReplMode = false

    companion object {
        /**
         * Synthetic tokens for runtime-generated variables and contexts.
         * Line number -1 indicates these are not from user source code.
         *
         * - ENCOUNTER_TOKEN: Used for contextual 'encounter' variable in explore blocks
         * - POKEMON_TOKEN: Used for accessing pokemon collections from SafariZone/Team
         * - EOF_TOKEN: Generic token for errors not associated with specific source location
         */
        private val ENCOUNTER_TOKEN = Token(TokenType. IDENTIFIER, "encounter", null, -1)
        private val POKEMON_TOKEN = Token(TokenType. IDENTIFIER, "pokemon", null, -1)
        private val EOF_TOKEN = Token(TokenType. EOF, "", null, -1)
    }

    private val arithmeticEvaluator = ArithmeticEvaluator(errorHandler)
    private val safariZoneObjects = SafariZoneObjects(errorHandler, this)
    private val builtinFunctions = BuiltinFunctions(errorHandler)

    init {
        registerBuiltinConstructors()
    }

    data class BuiltinConstructorMarker(val name: String)

    /** Registers built-in constructors in the global environment.
     *
     * This allows the evaluator to recognize built-in types like SafariZone
     * and Team when they are instantiated in the user code.
     */
    private fun registerBuiltinConstructors() {
        val constructors = listOf("SafariZone", "Team")

        for (name in constructors) {
            environment.define(
                Token(TokenType.IDENTIFIER, name, null, 0),
                BuiltinConstructorMarker(name)
            )
        }
    }

    fun getErrorHandler(): EvaluatorErrorHandler = errorHandler

    fun evaluate(node: AstNode, isReplMode: Boolean = false): Any? {
        this.isReplMode = isReplMode
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
        val result = stmt.expression.accept(this)

        if (isReplMode && result != null) {
            println(stringify(result))
        }

        return result
    }

    override fun visitPrintStmt(stmt: PrintStmt): Any? {
        val value = stmt.expression.accept(this)
        println(stringify(value))
        return null
    }

    override fun visitVarDeclStmt(stmt: VarDeclStmt): Any? {
        val value = stmt.expression.accept(this)
        environment.define(stmt. identifier, value)
        return null
    }

    override fun visitBlock(block: Block): Any? {
        return executeBlock(block.stmtList, Environment(enclosing = environment))
    }

    private fun executeBlock(statements: List<Stmt>, blockEnvironment: Environment): Any? {
        val previous = environment
        try {
            environment = blockEnvironment
            var lastValue: Any? = null
            for (stmt in statements) {
                lastValue = stmt.accept(this)
            }
            return lastValue
        } finally {
            environment = previous
        }
    }

    override fun visitIfStmt(stmt: IfStmt): Any? {
        val condition = stmt.expression. accept(this)
        return if (isTruthy(condition)) {
            stmt.thenBlock.accept(this)
        } else {
            stmt.elseBlock?.accept(this)
        }
    }

    // ========== Loop Statements ==========

    override fun visitWhileStmt(stmt: WhileStmt): Any? {
        while (isTruthy(stmt.condition.accept(this))) {
            try {
                stmt.body.accept(this)
            } catch (_: ContinueException) {
                continue
            } catch (_: BreakException) {
                break
            }
        }
        return null
    }

    override fun visitForStmt(stmt: ForStmt): Any? {
        val range = evaluateForRange(stmt)
        return executeForLoop(stmt, range)
    }

    private fun evaluateForRange(stmt: ForStmt): IntRange {
        val startVal = stmt.start. accept(this)
        val endVal = stmt.end.accept(this)

        if (startVal !is Int) {
            throw errorHandler.typeError(stmt.keyword, "For loop start must be an integer")
        }
        if (endVal !is Int) {
            throw errorHandler.typeError(stmt.keyword, "For loop end must be an integer")
        }

        return startVal..endVal
    }

    private fun executeForLoop(stmt: ForStmt, range: IntRange): Any? {
        val loopEnvironment = Environment(enclosing = environment)
        val previous = environment

        try {
            environment = loopEnvironment


            environment.define(stmt.variable, range.first)

            for (i in range) {
                environment.assign(stmt.variable, i)

                try {
                    stmt.body. accept(this)
                } catch (_: ContinueException) {
                    continue
                } catch (_: BreakException) {
                    break
                }
            }
            return null
        } finally {
            environment = previous
        }
    }

    override fun visitBreakStmt(stmt: BreakStmt): Any? {
        throw BreakException()
    }

    override fun visitContinueStmt(stmt: ContinueStmt): Any? {
        throw ContinueException()
    }

    // ========== Function Definition ==========

    override fun visitDefineStmt(stmt: DefineStmt): Any? {
        val function = createFunctionObject(stmt)
        environment.define(stmt. name, function)
        return null
    }

    private fun createFunctionObject(stmt: DefineStmt): FunctionObject {
        return FunctionObject(
            name = stmt.name,
            parameters = stmt.paramList,
            body = stmt.block,
            closure = environment
        )
    }

    // ========== Explore Statement ==========

    override fun visitExploreStmt(stmt: ExploreStmt): Any?  {
        val safariZone = getSafariZoneObject(stmt. safariZoneVar)
        return executeExploreBlock(stmt, safariZone)
    }

    private fun getSafariZoneObject(token: Token): SafariZoneObject {
        val obj = environment.get(token)
        if (obj !is SafariZoneObject) {
            throw errorHandler.typeError(
                token,
                "Explore expects a SafariZone object for '${token.lexeme}'"
            )
        }
        return obj
    }

    private fun executeExploreBlock(stmt: ExploreStmt, safariZone: SafariZoneObject): Any? {
        val exploreEnvironment = Environment(enclosing = environment)
        exploreEnvironment.define(stmt. safariZoneVar, safariZone)
        exploreEnvironment.define(ENCOUNTER_TOKEN, null)

        val previous = environment
        var endedByRun = false

        try {
            environment = exploreEnvironment

            while (safariZone.turns > 0) {
                safariZone.turns--

                val pokemonCollection = getPokemonCollection(safariZone)
                if (pokemonCollection.isEmpty()) {
                    println("No Pokemon left to encounter!")
                    break
                }

                val encounter = pokemonCollection.random(errorHandler, ENCOUNTER_TOKEN)
                environment.assign(ENCOUNTER_TOKEN, encounter)

                try {
                    stmt.block.accept(this)
                } catch (_: RunException) {
                    endedByRun = true
                    break
                }
            }

            if (safariZone.turns == 0 && !endedByRun) {
                println("Explore: Out of turns!")
            }
            return null
        } finally {
            environment = previous
        }
    }

    private fun getPokemonCollection(safariZone: SafariZoneObject): PokemonCollection {
        return safariZone.getProperty(
            "pokemon",
            errorHandler,
            POKEMON_TOKEN
        ) as PokemonCollection
    }

    // ========== Return & Run ==========

    override fun visitReturnStmt(stmt: ReturnStmt): Any? {
        validateReturnContext(stmt. keyword)
        val value = stmt. value?. accept(this)
        throw ReturnException(value)
    }

    private fun validateReturnContext(token: Token) {
        if (! inUserFunction) {
            throw errorHandler.error(
                token,
                "Return statement not allowed outside function",
                ErrorPhase.RUNTIME
            )
        }
    }

    override fun visitRunStmt(stmt: RunStmt): Any? {
        throw RunException()
    }

    // ========== Expressions ==========

    override fun visitLiteralExpr(expr: LiteralExpr): Any? = expr.value

    override fun visitVariableExpr(expr: VariableExpr): Any? = environment.get(expr.identifier)

    override fun visitUnaryExpr(expr: UnaryExpr): Any? {
        val operand = expr.right.accept(this)
        return evaluateUnaryOperation(expr.operator, operand)
    }

    private fun evaluateUnaryOperation(operator: Token, operand: Any?): Any?  {
        return when (operator.type) {
            TokenType.MINUS -> negateNumber(operator, operand)
            TokenType.NOT -> !isTruthy(operand)
            else -> throw errorHandler.error(
                operator,
                "Unknown unary operator '${operator.lexeme}'",
                ErrorPhase. RUNTIME
            )
        }
    }

    private fun negateNumber(operator: Token, operand: Any? ): Int {
        if (operand !is Int) {
            throw errorHandler.typeError(operator, "Operand must be a number")
        }
        return -operand
    }

    override fun visitBinaryExpr(expr: BinaryExpr): Any? {
        if (expr.operator.type == TokenType.AND) {
            return evaluateAndOperator(expr)
        }
        if (expr.operator.type == TokenType.OR) {
            return evaluateOrOperator(expr)
        }

        val left = expr.left. accept(this)
        val right = expr.right.accept(this)
        return arithmeticEvaluator.evaluate(left, expr.operator, right)
    }

    private fun evaluateAndOperator(expr: BinaryExpr): Boolean {
        val left = expr. left.accept(this)
        if (!isTruthy(left)) return false
        return isTruthy(expr.right.accept(this))
    }

    private fun evaluateOrOperator(expr: BinaryExpr): Boolean {
        val left = expr.left.accept(this)
        if (isTruthy(left)) return true
        return isTruthy(expr.right.accept(this))
    }

    override fun visitAssignExpr(expr: AssignExpr): Any? {
        val value = expr.value.accept(this)
        performAssignment(expr. target, value, expr.equals)
        return value
    }

    private fun performAssignment(target: Expr, value: Any?, token: Token) {
        when (target) {
            is VariableExpr -> environment.assign(target.identifier, value)
            is PropertyAccessExpr -> assignToProperty(target, value, token)
            is ArrayAccessExpr -> assignToArrayElement(target, value, token)
            else -> throw errorHandler.error(token, "Invalid assignment target")
        }
    }

    private fun assignToProperty(target: PropertyAccessExpr, value: Any?, token: Token) {
        val obj = target.primaryWithSuffixes.accept(this)
        if (obj !is SafariZoneObjectInterface) {
            throw errorHandler.typeError(token, "Cannot set property on non-object type")
        }
        obj. setProperty(target.identifier.lexeme, value, errorHandler, target.identifier)
    }

    private fun assignToArrayElement(target: ArrayAccessExpr, value: Any?, token: Token) {
        val array = target.array. accept(this)
        val index = target.index.accept(this)
        performArrayAssignment(array, index, value, token)
    }

    // ========== Array Expressions ==========

    override fun visitArrayLiteralExpr(expr: ArrayLiteralExpr): Any?  {
        return expr.elements.map { it. accept(this) }. toMutableList()
    }

    override fun visitArrayAccessExpr(expr: ArrayAccessExpr): Any? {
        val array = expr.array.accept(this)
        val index = expr.index. accept(this)
        return performArrayAccess(array, index, expr.leftBracket)
    }

    override fun visitArrayAssignExpr(expr: ArrayAssignExpr): Any? {
        val array = expr.array.accept(this)
        val index = expr.index.accept(this)
        val value = expr.value.accept(this)
        return performArrayAssignment(array, index, value, expr.leftBracket)
    }

    private fun performArrayAccess(array: Any?, index: Any?, token: Token): Any? {
        validateArrayType(array, token)
        val validIndex = validateArrayIndex(array as MutableList<*>, index, token)
        return array[validIndex]
    }

    private fun performArrayAssignment(array: Any?, index: Any?, value: Any?, token: Token): Any? {
        validateArrayType(array, token)
        val validIndex = validateArrayIndex(array as MutableList<*>, index, token)

        @Suppress("UNCHECKED_CAST")
        (array as MutableList<Any?>)[validIndex] = value
        return value
    }

    private fun validateArrayType(array: Any?, token: Token) {
        if (array !is MutableList<*>) {
            throw errorHandler.typeError(token, "Can only index arrays")
        }
    }

    private fun validateArrayIndex(array: MutableList<*>, index: Any?, token: Token): Int {
        if (index !is Int) {
            throw errorHandler.typeError(token, "Array index must be an integer")
        }
        if (index < 0 || index >= array.size) {
            throw errorHandler.error(
                token,
                "Array index $index out of bounds (size ${array.size})"
            )
        }
        return index
    }

    // ========== Function Calls ==========

    override fun visitCallExpr(expr: CallExpr): Any? {
        builtinFunctions.evaluate(expr, this)?.let { return it }

        return when (val callee = expr.callee) {
            is PropertyAccessExpr -> evaluateMethodCall(expr)
            is VariableExpr -> {
                val arguments = expr.args.map { it.accept(this) }

                // Try constructor
                safariZoneObjects.tryCreate(
                    callee.identifier.lexeme,
                    arguments,
                    expr. namedArgs,
                    callee.identifier
                ) ?: run {
                    // Try user function
                    val function = callee.accept(this)
                    if (function is FunctionObject) {
                        callUserFunction(function, arguments)
                    } else {
                        throw errorHandler.error(EOF_TOKEN, "Not a callable function")
                    }
                }
            }
            else -> {
                val function = callee.accept(this)
                if (function is FunctionObject) {
                    callUserFunction(function, expr.args. map { it.accept(this) })
                } else {
                    throw errorHandler.error(EOF_TOKEN, "Unknown function or not implemented")
                }
            }
        }
    }

    private fun evaluateMethodCall(expr: CallExpr): Any? {
        val propertyExpr = expr.callee as PropertyAccessExpr
        val obj = propertyExpr.primaryWithSuffixes.accept(this)
        val methodName = propertyExpr.identifier.lexeme
        val arguments = expr.args.map { it.accept(this) }

        if (obj !is SafariZoneObjectInterface) {
            throw errorHandler.typeError(propertyExpr.identifier, "Cannot call method on non-object type")
        }

        return obj.callMethod(methodName, arguments, errorHandler, propertyExpr.identifier)
    }

    private fun callUserFunction(function: FunctionObject, arguments: List<Any?>): Any? {
        validateArgumentCount(function, arguments)
        return executeFunction(function, arguments)
    }

    private fun validateArgumentCount(function: FunctionObject, arguments: List<Any? >) {
        if (arguments. size != function.parameters.size) {
            throw errorHandler.error(
                function.name,
                "Expected ${function.parameters.size} arguments but got ${arguments.size}"
            )
        }
    }

    private fun executeFunction(function: FunctionObject, arguments: List<Any?>): Any? {
        val functionEnvironment = Environment(enclosing = function.closure)
        bindParameters(function. parameters, arguments, functionEnvironment)

        val previous = environment
        val wasInFunction = inUserFunction

        try {
            environment = functionEnvironment
            inUserFunction = true

            function.body. accept(this)
            return null
        } catch (e: ReturnException) {
            return e.value
        } finally {
            inUserFunction = wasInFunction
            environment = previous
        }
    }

    private fun bindParameters(parameters: List<Token>, arguments: List<Any?>, env: Environment) {
        for ((param, arg) in parameters.zip(arguments)) {
            env.define(param, arg)
        }
    }

    override fun visitPropertyAccessExpr(expr: PropertyAccessExpr): Any? {
        val obj = expr.primaryWithSuffixes.accept(this)

        if (obj !is SafariZoneObjectInterface) {
            throw errorHandler.typeError(
                expr.identifier,
                "Cannot access property '${expr.identifier.lexeme}' on non-object type"
            )
        }

        return obj.getProperty(expr.identifier.lexeme, errorHandler, expr.identifier)
    }

    // ========== Helper Methods ==========

    private fun isTruthy(value: Any? ): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }

    fun stringify(value: Any?): String {
        return when (value) {
            null -> "null"
            is SafariZoneObjectInterface -> value.toString()
            is List<*> -> stringifyArray(value)
            is Boolean -> value.toString(). lowercase()
            else -> value. toString()
        }
    }

    private fun stringifyArray(list: List<*>): String {
        return "[" + list.joinToString(", ") { stringify(it) } + "]"
    }
}

