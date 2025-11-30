package evaluator

import lexer.*
import parser.*


/**
 * PokemonCollection - Represents a collection of Pokemon with generic collection methods.
 *
 * This is a collection object that provides standard collection operations.
 * Used by both SafariZone and Team objects.
 *
 * Methods:
 * - add(item) → adds an item to the collection
 * - remove(item) → removes an item from the collection
 * - list() → returns all items as a string
 * - find(item) → finds an item by name (case-insensitive)
 * - random() → returns a random item from the collection
 * - count() → returns the number of items in the collection
 * - clear() → removes all items from the collection
 */
class PokemonCollection(
    private val pokemonList: MutableList<String>,
    private val ownerType: String = "Collection"
) : SafariZoneObjectInterface {

    // Direct Kotlin methods for internal use
    fun isEmpty(): Boolean = pokemonList.isEmpty()

    fun remove(item: String): Boolean = pokemonList.remove(item)

    fun random(errorHandler: EvaluatorErrorHandler, token: Token): String {
        if (pokemonList.isEmpty()) {
            throw errorHandler.error(token, "$ownerType has no Pokemon.")
        }
        return pokemonList.random()
    }

    private val methodList: Map<String, (List<Any?>, EvaluatorErrorHandler, Token) -> Any?> = mapOf(
        "add" to { args, errorHandler, token ->
            val item = requireStringArg(args, errorHandler, token, "add")
            pokemonList.add(item)
            null
        },
        "remove" to { args, errorHandler, token ->
            val item = requireStringArg(args, errorHandler, token, "remove")
            remove(item)
        },
        "list" to { _, _, _ ->
            pokemonList.joinToString(", ")
        },
        "find" to { args, errorHandler, token ->
            val searchName = requireStringArg(args, errorHandler, token, "find")
            val found = pokemonList.find { it.equals(searchName, ignoreCase = true) }
            found ?: "null"
        },
        "random" to { _, errorHandler, token ->
            random(errorHandler, token)
        },
        "count" to { _, _, _ ->
            pokemonList.size
        },
        "clear" to { _, _, _ ->
            pokemonList.clear()
            null
        },
        "isEmpty" to { _, _, _ ->
            isEmpty()
        }
    )

    override fun callMethod(
        name: String,
        args: List<Any?>,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Any? {
        val method = methodList[name]
        if (method != null) {
            return method(args, errorHandler, token)
        } else {
            throw errorHandler.error(
                token,
                "PokemonCollection has no method '$name'. Available methods: ${methodList.keys.joinToString(", ")}"
            )
        }
    }

    private fun requireStringArg(
        args: List<Any?>,
        errorHandler: EvaluatorErrorHandler,
        token: Token,
        methodName: String
    ): String {
        if (args.isEmpty()) {
            throw errorHandler.error(token, "Method '$methodName' requires a string argument.")
        }
        val arg = args[0]
        if (arg !is String) {
            throw errorHandler.typeError(token, "Method '$methodName' requires a string argument.")
        }
        return arg
    }

    override fun toString(): String = pokemonList.joinToString(", ")
    override fun getTypeName(): String = "PokemonCollection"

    override fun getProperty(name: String, errorHandler: EvaluatorErrorHandler, token: Token): Any? {
        throw errorHandler.error(
            token,
            "PokemonCollection has no properties. Use methods with '->' operator."
        )
    }

    override fun setProperty(name: String, value: Any?, errorHandler: EvaluatorErrorHandler, token: Token) {
        throw errorHandler.error(
            token,
            "Cannot set properties on PokemonCollection."
        )
    }
}



/**
 * SafariZoneObject - Represents a Safari Zone area in the Pokemon game.
 *
 * A Safari Zone is a special area where players can catch Pokemon using a limited
 * number of Safari Balls and turns. Players must manage their resources carefully.
 *
 * Properties:
 * - initialBalls: Initial number of Safari Balls available (immutable)
 * - initialTurns: Initial number of turns allowed (immutable)
 * - balls: Current remaining Safari Balls (mutable)
 * - turns: Current remaining turns (mutable)
 * - pokemon: List of Pokemon available in this zone
 *
 * Example usage:
 * ```
 * var zone = SafariZone(30, 500);
 * zone.addPokemon("Pikachu");
 * zone.useBall();
 * print(zone.balls);  // 29
 * ```
 *
 * @property initialBalls Initial/maximum number of Safari Balls
 * @property initialTurns Initial/maximum number of turns
 * @property balls Current remaining Safari Balls (defaults to initialBalls)
 * @property turns Current remaining turns (defaults to initialTurns)
 * @property pokemon Mutable list of Pokemon names in this zone
 */class SafariZoneObject(
    val initialBalls: Int,
    val initialTurns: Int,
    var balls: Int = initialBalls,
    var turns: Int = initialTurns,
    val pokemon: MutableList<String> = mutableListOf()
) : SafariZoneObjectInterface {

    // Property getters
    private val propertyGetters = mapOf(
        "initialBalls" to { initialBalls },
        "initialTurns" to { initialTurns },
        "balls" to { balls },
        "turns" to { turns },
        "pokemon" to { PokemonCollection(pokemon, "SafariZone") }
    )

    override fun getProperty(
        name: String,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Any? {
        return propertyGetters[name]?.invoke()
            ?: throw errorHandler.propertyError(token, "SafariZone has no property '$name'")
    }

    // Property setters
    private val propertySetters = mapOf<String, (Any?, EvaluatorErrorHandler, Token) -> Unit>(
        "balls" to { value, errorHandler, token ->
            if (value !is Int) throw errorHandler.typeError(token, "balls must be an integer.")
            balls = value
        },
        "turns" to { value, errorHandler, token ->
            if (value !is Int) throw errorHandler.typeError(token, "turns must be an integer.")
            turns = value
        }
    )

    override fun setProperty(
        name: String,
        value: Any?,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ) {
        propertySetters[name]?.invoke(value, errorHandler, token)
            ?: throw errorHandler.propertyError(token, "Cannot modify property '$name'.")
    }

    // Method dispatch map
    private val methodList = mapOf(
        "useBall" to { _: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token ->
            if (balls <= 0) throw errorHandler.error(token, "No Safari Balls remaining!")
            balls--
            null
        },
        "useTurn" to { _: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token ->
            if (turns <= 0) throw errorHandler.error(token, "No turns remaining!")
            turns--
            null
        },
        "reset" to { _: List<Any?>, _: EvaluatorErrorHandler, _: Token ->
            balls = initialBalls
            turns = initialTurns
            null
        },
        "isGameOver" to { _: List<Any?>, _: EvaluatorErrorHandler, _: Token ->
            balls <= 0 || turns <= 0
        }
    )

    override fun callMethod(
        name: String,
        args: List<Any?>,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Any? {
        val method = methodList[name]
        if (method != null) {
            return method(args, errorHandler, token)
        } else {
            throw errorHandler.error(
                token,
                "SafariZone has no method '$name'. Use .pokemon-> for Pokemon management. Available methods: ${methodList.keys.joinToString(", ")}"
            )
        }
    }

    override fun toString(): String = "SafariZone(balls=$balls, turns=$turns)"
    override fun getTypeName(): String = "SafariZone"
}

/**
 * TeamObject - Represents a Pokemon trainer's team.
 *
 * A team has a maximum size (default 6, like in Pokemon games) and stores
 * the trainer's Pokemon. Teams have methods for managing Pokemon and checking
 * team status.
 *
 * Properties:
 * - trainerName: Name of the Pokemon trainer
 * - pokemons: List of Pokemon on the team
 * - maxSize: Maximum team size (default 6)
 *
 * Example usage:
 * ```
 * var team = Team("Ash");
 * team.add("Pikachu");
 * team.add("Charizard");
 * print(team.listPokemons());  // "Pikachu, Charizard"
 * ```
 *
 * @property trainerName The name of the Pokemon trainer
 * @property pokemons Mutable list of Pokemon on the team
 * @property maxSize Maximum number of Pokemon allowed (default 6)
 */
class TeamObject(
    val trainerName: String,
    val pokemons: MutableList<String> = mutableListOf(),
    val teamSize: Int = 6
) : SafariZoneObjectInterface {

    // Property getters (for getProperty)
    private val propertyGetters: Map<String, () -> Any?> = mapOf(
        "trainerName" to { trainerName },
        "maxSize" to { teamSize },
        "pokemonCount" to { pokemons.size },
        "pokemon" to { PokemonCollection(pokemons, "Team") }
    )

    override fun getProperty(
        name: String,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Any? {
        return propertyGetters[name]?.invoke()
            ?: throw errorHandler.propertyError(token, "Team has no property '$name'")
    }

    // Team object: all properties are read-only
    override fun setProperty(
        name: String,
        value: Any?,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ) {
        throw errorHandler.propertyError(token, "Team properties are read-only.")
    }

    // Methods map
    private val methodList: Map<String, (List<Any?>, EvaluatorErrorHandler, Token) -> Any?> = mapOf(
        "isFull" to { _, _, _ -> pokemons.size >= teamSize },
        "isEmpty" to { _, _, _ -> pokemons.isEmpty() },
        "has" to { args, errorHandler, token ->
            val pokemonName = requireStringArg(args, errorHandler, token, "has")
            hasPokemon(pokemonName)
        }
    )

    override fun callMethod(
        name: String,
        args: List<Any?>,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Any? {
        return methodList[name]?.invoke(args, errorHandler, token)
            ?: throw errorHandler.error(token, "Team has no method '$name'. Use .pokemon-> for Pokemon management. Available methods: ${methodList.keys.joinToString(", ")}")
    }

    // -- helper for method "has"
    fun hasPokemon(name: String): Boolean {
        return pokemons.any { it.equals(name, ignoreCase = true) }
    }

    private fun requireStringArg(
        args: List<Any?>,
        errorHandler: EvaluatorErrorHandler,
        token: Token,
        methodName: String
    ): String {
        if (args.isEmpty()) {
            throw errorHandler.error(token, "Method '$methodName' requires a string argument.")
        }
        val arg = args[0]
        if (arg !is String) {
            throw errorHandler.typeError(token, "Method '$methodName' requires a string argument.")
        }
        return arg
    }

    override fun toString(): String = "Team($trainerName, ${pokemons.size}/${teamSize} Pokemon)"
    override fun getTypeName(): String = "Team"
}


/**
 * Evaluator - The runtime evaluation engine for the language.
 *
 * This class implements the Visitor pattern to traverse and execute the Abstract
 * Syntax Tree (AST) produced by the parser. It maintains the runtime environment
 * (variable bindings) and evaluates expressions and statements.
 *
 * Key responsibilities:
 * - Expression evaluation (arithmetic, logical, comparison)
 * - Statement execution (variable declarations, control flow)
 * - Function calls and returns
 * - Object method calls (SafariZone and Team)
 * - Type checking and error reporting
 * - Environment management (scoping)
 *
 * The evaluator uses a tree-walking approach: it recursively visits each node
 * in the AST and performs the corresponding operation.
 *
 * Built-in types:
 * - Int: Integer numbers
 * - Double: Floating-point numbers
 * - String: Text strings
 * - Boolean: true/false
 * - SafariZoneObject: Safari Zone game objects
 * - TeamObject: Pokemon team objects
 * - FunctionObject: User-defined functions
 */
class Evaluator : AstVisitor<Any?> {
    /** Current environment for variable lookups and assignments */
    val errorHandler = EvaluatorErrorHandler()
    private var environment = Environment(errorHandler = errorHandler)
    private var isReplMode: Boolean = false
    private val arithmeticEvaluator = ArithmeticEvaluator(errorHandler)
    private val safariZoneObjects = SafariZoneObjects(errorHandler, this)
    /** Error handler for generating and throwing runtime errors */

    init {
        // Register built-in constructors as special marker values
        // These are recognized in visitCallExpr to create objects
        environment.define(Token(TokenType.IDENTIFIER, "SafariZone", null, 0), "BUILTIN_CONSTRUCTOR_SAFARIZONE")
        environment.define(Token(TokenType.IDENTIFIER, "Team", null, 0), "BUILTIN_CONSTRUCTOR_TEAM")
        environment.define(Token(TokenType.IDENTIFIER, "Object", null, 0), "BUILTIN_CONSTRUCTOR_OBJECT")
        environment.define(Token(TokenType.IDENTIFIER, "Pokemon", null, 0), "BUILTIN_CONSTRUCTOR_POKEMON")
    }

    /**
     * Main entry point for evaluating an AST node.
     * Delegates to the appropriate visit method via the Visitor pattern.
     *
     * @param node The AST node to evaluate
     * @return The result of evaluation (type depends on node type)
     */
    fun evaluate(node: AstNode, isReplMode: Boolean): Any? {
        return node.accept(this)
    }

    /**
     * Evaluates a Program (root of AST).
     * Executes all statements in sequence and returns the last value.
     *
     * @param program The program node containing all top-level statements
     * @return The value of the last statement (for REPL mode)
     */
    override fun visitProgram(program: Program): Any? {
        var lastValue: Any? = null

        for (stmt in program.stmtList) {
            lastValue = stmt.accept(this)
        }

        return lastValue
    }

    /**
     * Evaluates an expression statement.
     * Expression statements are expressions used as statements (e.g., function calls).
     *
     * In REPL mode, the result is auto-printed if non-null (handled in Main.kt).
     *
     * @param stmt The expression statement
     * @return The value of the expression
     */
    override fun visitExprStmt(stmt: ExprStmt): Any? {
        return stmt.expression.accept(this)
    }

    /**
     * Executes a print statement.
     * Evaluates the expression and prints it to stdout.
     *
     * Example: print("Hello");
     *
     * @param stmt The print statement
     * @return null (print statements don't produce values)
     */
    override fun visitPrintStmt(stmt: PrintStmt): Any? {
        val value = stmt.expression.accept(this)
        println(stringify(value))
        return null
    }

    /**
     * Executes a variable declaration statement.
     * Evaluates the initializer expression and binds it to the variable name
     * in the current environment.
     *
     * Example: var x = 10;
     *
     * @param stmt The variable declaration statement
     * @return null (declarations don't produce values)
     */
    override fun visitVarDeclStmt(stmt: VarDeclStmt): Any? {
        val value = stmt.expression.accept(this)
        environment.define(stmt.identifier, value)
        return null
    }

    /**
     * Block statements are not yet implemented.
     */
    override fun visitBlock(block: Block): Any? {
        val previous = environment

        try {
            environment = Environment(enclosing = previous)

            var lastValue: Any? = null
            for (stmt in block.stmtList) {
                lastValue = stmt.accept(this)
            }

            return lastValue
        } finally {
            environment = previous
        }
    }


    /**
     * If statements are not yet implemented.
     */
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


    /**
     * User-defined functions are not yet implemented.
     */
    override fun visitDefineStmt(stmt: DefineStmt): Any? {
        // Create a function object that captures the current environment
        val function = FunctionObject(
            name = stmt.name,
            parameters = stmt.paramList,
            body = stmt.block,
            closure = environment  // Capture closure at definition time
        )

        // Store the function in the environment
        environment.define(stmt.name, function)
        return null
    }


    /**
     * Explore statements are not yet implemented.
     */
    override fun visitExploreStmt(stmt: ExploreStmt): Any? {
        // Infinite loop until run statement is called
        while (true) {
            try {
                stmt.block.accept(this)
            } catch (e: RunException) {
                break  // run statement was executed
            }
        }
        return null
    }


    /**
     * Executes a throw ball statement (Safari Zone specific).
     * Currently just evaluates the expression.
     *
     * Example: throwball myZone;
     *
     * @param stmt The throw ball statement
     * @return The value of the expression
     */
    override fun visitThrowBallStmt(stmt: ThrowBallStmt): Any? {
        val target = stmt.expression.accept(this)

        if (target !is SafariZoneObjectInterface) {
            throw errorHandler.typeError(
                Token(TokenType.THROWBALL_KEYWORD, "throwBall", null, 0),
                "throwBall target must be a SafariZone object"
            )
        }

        // Get the pokemon collection
        val pokemonCollection = target.getProperty("pokemon", errorHandler,
            Token(TokenType.IDENTIFIER, "pokemon", null, 0)) as PokemonCollection

        // Check if empty using direct method
        if (pokemonCollection.isEmpty()) {
            throw errorHandler.error(
                Token(TokenType.THROWBALL_KEYWORD, "throwBall", null, 0),
                "No Pokemon in this Safari Zone!"
            )
        }

        // Catch a random Pokemon
        val caught = pokemonCollection.random(errorHandler,
            Token(TokenType.THROWBALL_KEYWORD, "throwBall", null, 0)) as String

        // Remove it from the zone
        pokemonCollection.remove(caught)

        println("Caught a $caught!")
        return caught
    }



    /**
     * Return statements are not yet implemented.
     */
    override fun visitReturnStmt(stmt: ReturnStmt): Any? {
        val value = stmt.value?.accept(this)
        throw ReturnException(value)
    }

    /**
     * Executes a run statement (loop control).
     * Currently a placeholder - would implement "continue" in a loop.
     *
     * @param stmt The run statement
     * @return null
     */
    override fun visitRunStmt(stmt: RunStmt): Any? {
        throw RunException()
    }


    /**
     * Evaluates a literal expression (constant value).
     *
     * Literals are values directly written in code:
     * - Numbers: 42, 3.14
     * - Strings: "Hello"
     * - Booleans: true, false
     * - null: null
     *
     * @param expr The literal expression
     * @return The literal value
     */
    override fun visitLiteralExpr(expr: LiteralExpr): Any? {
        return expr.value
    }

    /**
     * Evaluates a variable expression (variable reference).
     * Looks up the variable in the current environment and returns its value.
     *
     * Example: x (returns the value of variable x)
     *
     * @param expr The variable expression
     * @return The variable's value
     * @throws RuntimeError if variable is undefined
     */
    override fun visitVariableExpr(expr: VariableExpr): Any? {
        return environment.get(expr.identifier)
    }

    /**
     * Evaluates a unary expression (single operand with prefix operator).
     *
     * Supported operators:
     * - Minus (-): Numeric negation
     * - Not (!): Logical negation
     *
     * Examples:
     * - -5 → -5
     * - !true → false
     * - !(x > 10) → true if x <= 10
     *
     * @param expr The unary expression
     * @return The result of applying the operator
     * @throws RuntimeError if operand type is invalid
     */
    override fun visitUnaryExpr(expr: UnaryExpr): Any? {
        val right = expr.right.accept(this)

        return when (expr.operator.type) {
            TokenType.MINUS -> {
                // Type check inline
                when (right) {
                    is Double -> -right
                    is Int -> -right
                    else -> throw errorHandler.typeError(expr.operator, "Operand must be a number.")
                }
            }
            TokenType.NOT -> !isTruthy(right)
            else -> throw errorHandler.error(
                expr.operator,
                "Unknown unary operator '${expr.operator.lexeme}'.",
                ErrorPhase.RUNTIME
            )
        }
    }

    /**
     * Evaluates a binary expression (two operands with infix operator).
     *
     * Supported operator categories:
     *
     * 1. Arithmetic: +, -, *, /, %
     *    - Work with Int and Double
     *    - + also concatenates strings
     *
     * 2. Comparison: >, >=, <, <=
     *    - Work with numbers, return boolean
     *
     * 3. Equality: ==, !=
     *    - Work with any types
     *
     * 4. Logical: and, or
     *    - Short-circuit evaluation (right side only evaluated if needed)
     *
     * Examples:
     * - 10 + 5 → 15
     * - "Hello" + " " + "World" → "Hello World"
     * - 10 > 5 → true
     * - true and false → false
     * - 10 % 3 → 1
     *
     * @param expr The binary expression
     * @return The result of applying the operator
     * @throws RuntimeError if operand types are invalid for the operator
     */
    override fun visitBinaryExpr(node: BinaryExpr): Any? {
        // Handle short-circuit operators BEFORE evaluating both sides
        if (node.operator.type == TokenType.AND) {
            val left = node.left.accept(this)
            if (!isTruthy(left)) return false
            return isTruthy(node.right.accept(this))
        }

        if (node.operator.type == TokenType.OR) {
            val left = node.left.accept(this)
            if (isTruthy(left)) return true
            return isTruthy(node.right.accept(this))
        }

        // All other operators evaluate both sides
        val left = node.left.accept(this)
        val right = node.right.accept(this)
        return arithmeticEvaluator.evaluate(left, node.operator, right)
    }


    /**
     * Evaluates an assignment expression.
     *
     * Supports two types of assignment:
     * 1. Variable assignment: x = 10
     * 2. Property assignment: myZone.balls = 5
     *
     * For SafariZone objects, only balls and turns can be modified.
     * For Team objects, all properties are read-only.
     *
     * Examples:
     * - x = 20
     * - myZone.balls = 10
     *
     * @param expr The assignment expression
     * @return The assigned value
     * @throws RuntimeError if assignment target is invalid or property is read-only
     */
    override fun visitAssignExpr(expr: AssignExpr): Any? {
        val value = expr.value.accept(this)

        when (expr.target) {
            is VariableExpr -> {
                environment.assign(expr.target.identifier, value)
            }

            is PropertyAccessExpr -> {
                val obj = expr.target.primaryWithSuffixes.accept(this)

                if (obj !is SafariZoneObjectInterface) {
                    throw errorHandler.typeError(
                        expr.equals,
                        "Cannot set property on non-object type"
                    )
                }

                obj.setProperty(
                    expr.target.identifier.lexeme,
                    value,
                    errorHandler,
                    expr.target.identifier
                )
            }

            else -> throw errorHandler.error(expr.equals, "Invalid assignment target")
        }

        return value
    }

    /**
     * Evaluates a call expression (function or constructor call).
     *
     * Handles three types of calls:
     *
     * 1. User-defined functions:
     *    ```
     *    define add(a, b) { return a + b; }
     *    add(5, 10);  // Returns 15
     *    ```
     *
     * 2. Built-in constructors:
     *    ```
     *    SafariZone(30, 500);
     *    Team("Ash", 6);
     *    ```
     *
     * 3. Object methods:
     *    ```
     *    myZone.useBall();
     *    team.add("Pikachu");
     *    ```
     *
     * Constructors support both positional and named arguments:
     * - SafariZone(30, 500)
     * - SafariZone(balls=30, turns=500)
     * - SafariZone(30, turns=500)
     *
     * @param expr The call expression
     * @return The return value of the function/method, or the created object
     * @throws RuntimeError if function doesn't exist or arguments are invalid
     */
    override fun visitCallExpr(expr: CallExpr): Any? {
        // Handle method calls on objects (existing logic)
        if (expr.callee is PropertyAccessExpr) {
            val propertyExpr = expr.callee
            val obj = propertyExpr.primaryWithSuffixes.accept(this)
            val methodName = propertyExpr.identifier.lexeme
            val arguments = expr.args.map { it.accept(this) }

            if (obj is SafariZoneObjectInterface) {
                return obj.callMethod(methodName, arguments, errorHandler, propertyExpr.identifier)
            }

            throw errorHandler.typeError(
                propertyExpr.identifier,
                "Cannot call method on non-object type."
            )
        }

        // Evaluate arguments
        val arguments = expr.args.map { it.accept(this) }

        // Handle built-in constructors
        if (expr.callee is VariableExpr) {
            val calleeVar = expr.callee as VariableExpr
            val funcName = calleeVar.identifier.lexeme

            val builtinObject = safariZoneObjects.tryCreate(
                funcName, arguments, expr.namedArgs, calleeVar.identifier
            )

            if (builtinObject != null) return builtinObject
        }

        // Handle user-defined functions
        val function = expr.callee.accept(this)
        if (function is FunctionObject) {
            return callUserFunction(function, arguments)
        }

        throw errorHandler.error(
            Token(TokenType.EOF, "", null, 0),
            "Unknown function or not implemented."
        )
    }

    private fun callUserFunction(
        function: FunctionObject,
        arguments: List<Any?>
    ): Any? {
        // Arity checking
        if (arguments.size != function.parameters.size) {
            throw errorHandler.error(
                function.name,
                "Expected ${function.parameters.size} arguments but got ${arguments.size}."
            )
        }

        val previous = environment
        try {
            // Create environment with closure as parent (not current environment)
            environment = Environment(enclosing = function.closure)

            // Bind parameters to arguments
            for ((param, arg) in function.parameters.zip(arguments)) {
                environment.define(param, arg)
            }

            // Execute body, catching return statements
            try {
                function.body.accept(this)
                return null  // Implicit return
            } catch (e: ReturnException) {
                return e.value  // Explicit return value
            }
        } finally {
            environment = previous  // Restore caller's environment
        }
    }

    override fun visitFunctionCall(function: FunctionObject, arguments: List<Any?>): Any? {
        return callUserFunction(function, arguments)
    }



    /**
     * Evaluates a property access expression.
     *
     * The dot operator (.) is ONLY for accessing properties, not methods.
     * Use the arrow operator (->) for method calls.
     *
     * SafariZone properties:
     * - initialBalls, initialTurns: Initial values (read-only)
     * - balls, turns: Current values (mutable)
     * - pokemonCount: Number of Pokemon
     *
     * Team properties:
     * - trainerName: Trainer's name
     * - maxSize: Maximum team size
     * - pokemonCount: Number of Pokemon
     *
     * Example: myZone.balls (correct)
     * Example: myZone.useBall() (wrong - use myZone->useBall())
     *
     * @param node The property access expression
     * @return The property value
     * @throws RuntimeError if property doesn't exist or object type is invalid
     */
    override fun visitPropertyAccessExpr(node: PropertyAccessExpr): Any? {
        val obj = node.primaryWithSuffixes.accept(this)

        if (obj !is SafariZoneObjectInterface) {
            throw errorHandler.typeError(
                node.identifier,
                "Cannot access property '${node.identifier.lexeme}' on non-object type"
            )
        }

        return obj.getProperty(node.identifier.lexeme, errorHandler, node.identifier)
    }

    // ========== Helper Methods ==========

    /**
     * Determines if a value is "truthy" in conditional contexts.
     *
     * Truthiness rules:
     * - null → false
     * - false → false
     * - Everything else → true
     *
     * This means 0, empty strings, etc. are truthy (unlike some languages).
     *
     * @param value The value to check
     * @return true if value is truthy, false if falsey
     */
    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }


    /**
     * Converts a value to a string for printing.
     * Made public so REPL (Main.kt) can use it.
     *
     * Formatting rules:
     * - null → "null"
     * - Custom objects → their toString() method
     * - Doubles ending in .0 → remove decimal (10.0 → "10")
     * - Booleans → lowercase ("true", "false")
     *
     * @param value The value to convert
     * @return String representation
     */
    fun stringify(value: Any?): String {
        if (value == null) return "null"

        // Handle custom objects
        if (value is SafariZoneObject || value is TeamObject) {
            return value.toString()
        }

        // Format numbers nicely (remove .0 from integers)
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
}
