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

    /**
     * Visits the root Program node and evaluates each statement in sequence.
     * Returns the value of the last evaluated statement.
     */
    override fun visitProgram(program: Program): Any? {
        var lastValue: Any? = null
        for (stmt in program.stmtList) {
            lastValue = stmt.accept(this)
        }
        return lastValue
    }

    /**
     * Visits an expression statement.
     * Evaluates the expression and, in REPL mode, prints the result if not null.
     */
    override fun visitExprStmt(stmt: ExprStmt): Any? {
        val result = stmt.expression.accept(this)

        if (isReplMode && result != null) {
            println(stringify(result))
        }

        return result
    }

    /**
     * Visits a print statement.
     * Evaluates the expression and prints its string representation.
     */
    override fun visitPrintStmt(stmt: PrintStmt): Any? {
        val value = stmt.expression.accept(this)
        println(stringify(value))
        return null
    }

    /**
     * Visits a variable declaration statement.
     * Evaluates the initializer expression and defines the variable in the current environment.
     */
    override fun visitVarDeclStmt(stmt: VarDeclStmt): Any? {
        val value = stmt.expression.accept(this)
        environment.define(stmt. identifier, value)
        return null
    }

    /**
     * Visits a block statement.
     * Executes the block in a new environment scope.
     */
    override fun visitBlock(block: Block): Any? {
        return executeBlock(block.stmtList, Environment(enclosing = environment))
    }

    /**
     * Executes a list of statements within a given block environment.
     * Temporarily switches the current environment to the block's environment
     * and restores the previous environment afterward.
     */
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

    /** Visits a while statement.
     * Evaluates the condition and repeatedly executes the body while the condition is truthy.
     * Supports 'break' and 'continue' control flow within the loop body.
     */
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

    /** Visits a for statement.
     * Supports three styles:
     * - for i in 0 to 10 (integer range)
     * - for char in stringVar (string iteration)
     * - for item in arrayVar (array iteration)
     * Supports 'break' and 'continue' control flow within the loop body.
     */
    override fun visitForStmt(stmt: ForStmt): Any?  {
        val startVal = stmt.start.accept(this)

        // If no 'to' clause, iterate over collection/string
        if (stmt.end == null) {
            return when (startVal) {
                is String -> executeStringForLoop(stmt, startVal)
                is MutableList<*> -> executeArrayForLoop(stmt, startVal)
                else -> throw errorHandler. typeError(
                    stmt.keyword,
                    "Can only iterate over strings or arrays without 'to', got ${startVal?. javaClass?.simpleName ?: "null"}"
                )
            }
        }

        // If 'to' clause exists, evaluate both start and end
        val endVal = stmt.end.accept(this)

        // Check if still using string/array with 'to' (for backwards compatibility)
        if (startVal is String) {
            return executeStringForLoop(stmt, startVal)
        }
        if (startVal is MutableList<*>) {
            return executeArrayForLoop(stmt, startVal)
        }

        // Otherwise, treat as integer range
        val range = evaluateForRange(startVal, endVal, stmt. keyword)
        return executeForLoop(stmt, range)
    }

    /** Executes a for loop that iterates over an array's elements.
     * Creates a new environment scope for the loop variable,
     * assigns each element to the loop variable,
     * and executes the loop body.
     * Supports 'break' and 'continue' control flow within the loop body.
     */
    private fun executeArrayForLoop(stmt: ForStmt, array: MutableList<*>): Any? {
        val loopEnvironment = Environment(enclosing = environment)
        val previous = environment

        try {
            environment = loopEnvironment

            // Initialize with first element or null
            environment.define(stmt.variable, array.firstOrNull())

            for (element in array) {
                // Assign the current element
                environment.assign(stmt.variable, element)

                try {
                    stmt.body.accept(this)
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

    /** Evaluates the start and end expressions of a for loop to produce an IntRange.
     * Throws a type error if either expression does not evaluate to an integer.
     */
    private fun evaluateForRange(startVal: Any?, endVal: Any?, token: Token): IntRange {
        if (startVal !is Int) {
            throw errorHandler.typeError(token, "For loop start must be an integer or string")
        }
        if (endVal !is Int) {
            throw errorHandler.typeError(token, "For loop end must be an integer")
        }

        return startVal..endVal
    }

    /** Executes a for loop that iterates over a string's characters.
     * Creates a new environment scope for the loop variable,
     * assigns each character to the loop variable,
     * and executes the loop body.
     * Supports 'break' and 'continue' control flow within the loop body.
     */
    private fun executeStringForLoop(stmt: ForStmt, str: String): Any? {
        val loopEnvironment = Environment(enclosing = environment)
        val previous = environment

        try {
            environment = loopEnvironment

            // Initialize with empty string
            environment.define(stmt.variable, "")

            for (char in str) {
                // Assign the current character as a string
                environment.assign(stmt.variable, char.toString())

                try {
                    stmt.body.accept(this)
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

    /** Executes the body of a for loop over the specified integer range.
     * Creates a new environment scope for the loop variable,
     * assigns the loop variable for each iteration,
     * and executes the loop body.
     * Supports 'break' and 'continue' control flow within the loop body.
     */
    private fun executeForLoop(stmt: ForStmt, range: IntRange): Any? {
        val loopEnvironment = Environment(enclosing = environment)
        val previous = environment

        try {
            environment = loopEnvironment
            environment.define(stmt.variable, range.first)

            for (i in range) {
                environment.assign(stmt.variable, i)

                try {
                    stmt.body.accept(this)
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

    /** Visits a break statement.
     * Throws a BreakException to signal loop termination.
     */
    override fun visitBreakStmt(stmt: BreakStmt): Any? {
        throw BreakException()
    }

    /** Visits a continue statement.
     * Throws a ContinueException to signal skipping to the next loop iteration.
     */
    override fun visitContinueStmt(stmt: ContinueStmt): Any? {
        throw ContinueException()
    }

    /**
     * Visits a function definition statement.
     * Creates a FunctionObject and defines it in the current environment.
     */
    override fun visitDefineStmt(stmt: DefineStmt): Any? {
        val function = createFunctionObject(stmt)
        environment.define(stmt. name, function)
        return null
    }

    /**
     * Creates a FunctionObject representing a user-defined function.
     * Captures the current environment as the function's closure.
     */
    private fun createFunctionObject(stmt: DefineStmt): FunctionObject {
        return FunctionObject(
            name = stmt.name,
            parameters = stmt.paramList,
            body = stmt.block,
            closure = environment
        )
    }

    /** Visits an explore statement.
     * Retrieves the SafariZone object and executes the explore block.
     */
    override fun visitExploreStmt(stmt: ExploreStmt): Any?  {
        val safariZone = getSafariZoneObject(stmt. safariZoneVar)
        return executeExploreBlock(stmt, safariZone)
    }

    /**
     * Retrieves a SafariZoneObject from the environment by variable token.
     * Throws a type error if the variable is not a SafariZoneObject.
     */
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

    /**
     * Executes the explore block within the context of the given SafariZoneObject.
     * Sets up a new environment with the safari zone variable and encounter variable.
     * Iterates through the turns, randomly selecting encounters and executing the block.
     * Handles early exit via RunException and reports when turns are exhausted.
     * Only runs when turns, balls, and available Pokémon are present.
     */
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

    /**
     * Retrieves the PokemonCollection from a SafariZoneObject.
     */
    private fun getPokemonCollection(safariZone: SafariZoneObject): PokemonCollection {
        return safariZone.getProperty(
            "pokemon",
            errorHandler,
            POKEMON_TOKEN
        ) as PokemonCollection
    }

    /** Visits a return statement.
     * Validates that the return is within a function context,
     * evaluates the return value expression (if any),
     * and throws a ReturnException to signal function return.
     */
    override fun visitReturnStmt(stmt: ReturnStmt): Any? {
        validateReturnContext(stmt. keyword)
        val value = stmt. value?. accept(this)
        throw ReturnException(value)
    }

    /** Validates that a return statement is within a function context.
     * Throws a runtime error if the return is outside of a function.
     */
    private fun validateReturnContext(token: Token) {
        if (! inUserFunction) {
            throw errorHandler.error(
                token,
                "Return statement not allowed outside function",
                ErrorPhase.RUNTIME
            )
        }
    }

    /** Visits a run statement.
     * Throws a RunException to signal early exit from an explore block.
     */
    override fun visitRunStmt(stmt: RunStmt): Any? {
        throw RunException()
    }

    /**
     * Visits a literal expression.
     * Returns the literal value directly.
     */
    override fun visitLiteralExpr(expr: LiteralExpr): Any? = expr.value

    /**
     * Visits a variable expression.
     * Retrieves the variable's value from the environment.
     */
    override fun visitVariableExpr(expr: VariableExpr): Any? = environment.get(expr.identifier)

    /**
     * Visits a unary expression.
     * Evaluates the operand and applies the unary operator.
     */
    override fun visitUnaryExpr(expr: UnaryExpr): Any? {
        val operand = expr.right.accept(this)
        return evaluateUnaryOperation(expr.operator, operand)
    }

    /**
     * Evaluates a unary operation based on the operator type.
     * Supports negation for numbers and logical NOT for booleans.
     */
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

    /**
     * Negates a numeric operand.
     * Throws a type error if the operand is not a number.
     */
    private fun negateNumber(operator: Token, operand: Any? ): Int {
        if (operand !is Int) {
            throw errorHandler.typeError(operator, "Operand must be a number")
        }
        return -operand
    }

    /**
     * Visits a binary expression.
     * Evaluates the left and right operands and applies the binary operator.
     * Short-circuits logical AND and OR operators for efficiency.
     */
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

    /**
     * Evaluates a logical AND operation with short-circuiting
     */
    private fun evaluateAndOperator(expr: BinaryExpr): Boolean {
        val left = expr. left.accept(this)
        if (!isTruthy(left)) return false
        return isTruthy(expr.right.accept(this))
    }

    /**
     * Evaluates a logical OR operation with short-circuiting.
     */
    private fun evaluateOrOperator(expr: BinaryExpr): Boolean {
        val left = expr.left.accept(this)
        if (isTruthy(left)) return true
        return isTruthy(expr.right.accept(this))
    }

    /**
     * Visits an assignment expression.
     * Evaluates the value expression and assigns it to the target variable or property.
     */
    override fun visitAssignExpr(expr: AssignExpr): Any? {
        val value = expr.value.accept(this)
        performAssignment(expr. target, value, expr.equals)
        return value
    }

    /**
     * Performs the assignment to the target expression.
     * Supports variable assignments, property assignments, and array element assignments.
     */
    private fun performAssignment(target: Expr, value: Any?, token: Token) {
        when (target) {
            is VariableExpr -> environment.assign(target.identifier, value)
            is PropertyAccessExpr -> assignToProperty(target, value, token)
            is ArrayAccessExpr -> assignToArrayElement(target, value, token)
            else -> throw errorHandler.error(token, "Invalid assignment target")
        }
    }

    /**
     * Assigns a value to a property of an object.
     * Validates that the target is an object and sets the property.
     */
    private fun assignToProperty(target: PropertyAccessExpr, value: Any?, token: Token) {
        val obj = target.primaryWithSuffixes.accept(this)
        if (obj !is SafariZoneObjectInterface) {
            throw errorHandler.typeError(token, "Cannot set property on non-object type")
        }
        obj. setProperty(target.identifier.lexeme, value, errorHandler, target.identifier)
    }

    /**
     * Assigns a value to an element of an array.
     * Validates that the target is an array and sets the element at the specified index.
     */
    private fun assignToArrayElement(target: ArrayAccessExpr, value: Any?, token: Token) {
        val array = target.array. accept(this)
        val index = target.index.accept(this)
        performArrayAssignment(array, index, value, token)
    }

    /**
     * Maps the elements into a mutable list
     */
    override fun visitArrayLiteralExpr(expr: ArrayLiteralExpr): Any?  {
        return expr.elements.map { it. accept(this) }. toMutableList()
    }

    /**
     * Visits an array access expression.
     * Evaluates the array and index expressions and retrieves the element at the specified index.
     */
    override fun visitArrayAccessExpr(expr: ArrayAccessExpr): Any? {
        val array = expr.array.accept(this)
        val index = expr.index. accept(this)
        return performArrayAccess(array, index, expr.leftBracket)
    }

    /**
     * Visits an array assignment expression.
     * Evaluates the array, index, and value expressions and assigns the value at the specified
     */
    override fun visitArrayAssignExpr(expr: ArrayAssignExpr): Any? {
        val array = expr.array.accept(this)
        val index = expr.index.accept(this)
        val value = expr.value.accept(this)
        return performArrayAssignment(array, index, value, expr.leftBracket)
    }

    /**
     * Performs array/string access by validating the type and index,
     * then returning the element/character at the specified index.
     */
    private fun performArrayAccess(array: Any?, index: Any?, token: Token): Any? {
        return when (array) {
            is MutableList<*> -> {
                val validIndex = validateArrayIndex(array, index, token)
                array[validIndex]
            }
            is String -> {
                val validIndex = validateStringIndex(array, index, token)
                array[validIndex]. toString()  // Return single character as string
            }
            else -> throw errorHandler.typeError(
                token,
                "Can only index arrays and strings, got ${array?. javaClass?.simpleName ?: "null"}"
            )
        }
    }

    /**
     * Validates that the given index is an integer within the bounds of the string.
     * Throws a type error if the index is not an integer,
     * or an error if the index is out of bounds.
     */
    private fun validateStringIndex(str: String, index: Any?, token: Token): Int {
        if (index !is Int) {
            throw errorHandler.typeError(token, "String index must be an integer")
        }
        if (index < 0 || index >= str. length) {
            throw errorHandler.error(
                token,
                "String index $index out of bounds (length ${str.length})"
            )
        }
        return index
    }

    /**
     * Performs array assignment by validating the array type and index,
     * then setting the element at the specified index to the given value.
     * Strings cannot be assigned to because they are immutable.
     */
    private fun performArrayAssignment(array: Any?, index: Any?, value: Any?, token: Token): Any? {
        // Disallow string assignment (strings are immutable)
        if (array is String) {
            throw errorHandler.error(
                token,
                "Strings are immutable and cannot be modified.  Use string concatenation instead."
            )
        }

        validateArrayType(array, token)
        val validIndex = validateArrayIndex(array as MutableList<*>, index, token)

        (array as MutableList<Any?>)[validIndex] = value
        return value
    }

    /**
     * Validates that the given value is an array (MutableList).
     * Throws a type error if the value is not an array.
     */
    private fun validateArrayType(array: Any?, token: Token) {
        if (array !is MutableList<*>) {
            throw errorHandler.typeError(
                token,
                "Can only assign to arrays, not ${array?.javaClass?.simpleName ?: "null"}"
            )
        }
    }

    /**
     * Validates that the given index is an integer within the bounds of the array.
     * Throws a type error if the index is not an integer,
     * or an error if the index is out of bounds.
     */
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
    /**
     * Evaluates a function or method call expression.
     * First checks for built-in functions, then handles method calls on objects,
     */
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

    /**
     * Evaluates a method call on an object.
     * Retrieves the object and method name, evaluates the arguments,
     * and invokes the method on the object.
     */
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

    /**
     * Calls a user-defined function.
     * Validates the argument count and executes the function body in a new environment.
     */
    private fun callUserFunction(function: FunctionObject, arguments: List<Any?>): Any? {
        validateArgumentCount(function, arguments)
        return executeFunction(function, arguments)
    }

    /**
     * Validates that the number of arguments matches the function's parameter count.
     * Throws an error if the counts do not match.
     */
    private fun validateArgumentCount(function: FunctionObject, arguments: List<Any? >) {
        if (arguments. size != function.parameters.size) {
            throw errorHandler.error(
                function.name,
                "Expected ${function.parameters.size} arguments but got ${arguments.size}"
            )
        }
    }

    /**
     * Executes a user-defined function.
     * Creates a new environment for the function call,
     * binds the parameters to the arguments,
     * and executes the function body.
     * Catches ReturnException to retrieve the return value.
     */
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

    /**
     * Binds function parameters to the provided arguments in the given environment.
     */
    private fun bindParameters(parameters: List<Token>, arguments: List<Any?>, env: Environment) {
        for ((param, arg) in parameters.zip(arguments)) {
            env.define(param, arg)
        }
    }

    /**
     * Visits a property access expression.
     * Evaluates the primary expression to get the object,
     * then retrieves the specified property from the object.
     */
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


    /**
     * Determines the truthiness of a value.
     */
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

