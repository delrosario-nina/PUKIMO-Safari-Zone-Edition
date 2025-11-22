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
) : CustomObjectInterface {
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

    override fun callMethod(name: String, args: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token): Any? {
        return when (name) {
            "add" -> {
                val item = requireStringArg(args, errorHandler, token, name)
                pokemonList.add(item)
                null
            }
            "remove" -> {
                val item = requireStringArg(args, errorHandler, token, name)
                pokemonList.remove(item)
            }
            "list" -> pokemonList.joinToString(", ")
            "find" -> {
                val searchName = requireStringArg(args, errorHandler, token, "find")
                val found = pokemonList.find { it.equals(searchName, ignoreCase = true) }
                found ?: "null"
            }
            "random" -> {
                if (pokemonList.isEmpty()) {
                    throw errorHandler.error(token, "$ownerType has no Pokemon.")
                }
                pokemonList.random()
            }
            "count" -> pokemonList.size
            "clear" -> {
                pokemonList.clear()
                null
            }
            "isEmpty" -> pokemonList.isEmpty()
            else -> throw errorHandler.error(
                token,
                "PokemonCollection has no method '$name'. Available methods: add, remove, list, find, random, count, clear, isEmpty"
            )
        }
    }

    private fun requireStringArg(args: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token, methodName: String): String {
        if (args.isEmpty()) {
            throw errorHandler.error(token, "Method '$methodName' requires a string argument.")
        }
        val arg = args[0]
        if (arg !is String) {
            throw errorHandler.typeError(token, "Method '$methodName' requires a string argument.")
        }
        return arg
    }
}

/**
 * CustomObjectInterface - Base interface for all custom objects in the language.
 *
 * This interface provides a unified way to access properties and call methods
 * on custom objects. All language-specific objects (SafariZone, Team, Pokemon, etc.)
 * should implement this interface.
 *
 * Benefits:
 * - Uniform property access across all object types
 * - Extensible method calling mechanism
 * - Easier to add new object types
 * - Cleaner evaluator code
 */
interface CustomObjectInterface {
    /**
     * Gets a property value by name.
     *
     * @param name The property name
     * @param errorHandler Error handler for throwing runtime errors
     * @param token Token for error location reporting
     * @return The property value
     * @throws RuntimeError if property doesn't exist
     */
    fun getProperty(name: String, errorHandler: EvaluatorErrorHandler, token: Token): Any?

    /**
     * Sets a property value by name.
     *
     * @param name The property name
     * @param value The new value to set
     * @param errorHandler Error handler for throwing runtime errors
     * @param token Token for error location reporting
     * @throws RuntimeError if property is read-only or doesn't exist
     */
    fun setProperty(name: String, value: Any?, errorHandler: EvaluatorErrorHandler, token: Token)

    /**
     * Calls a method by name with the given arguments.
     *
     * @param name The method name
     * @param args List of arguments to pass to the method
     * @param errorHandler Error handler for throwing runtime errors
     * @param token Token for error location reporting
     * @return The method's return value
     * @throws RuntimeError if method doesn't exist or arguments are invalid
     */
    fun callMethod(name: String, args: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token): Any?

    /**
     * Gets the type name of this object (e.g., "SafariZone", "Team", "Pokemon").
     *
     * @return The type name as a string
     */
    fun getTypeName(): String
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
 */
class SafariZoneObject(
    val initialBalls: Int,
    val initialTurns: Int,
    var balls: Int = initialBalls,
    var turns: Int = initialTurns,
    val pokemon: MutableList<String> = mutableListOf()
) : CustomObjectInterface {
    override fun toString(): String = "SafariZone(balls=$balls, turns=$turns)"

    override fun getTypeName(): String = "SafariZone"

    override fun getProperty(name: String, errorHandler: EvaluatorErrorHandler, token: Token): Any? {
        return when (name) {
            "initialBalls" -> initialBalls
            "initialTurns" -> initialTurns
            "balls" -> balls
            "turns" -> turns
            "pokemon" -> PokemonCollection(pokemon, "SafariZone")
            else -> throw errorHandler.propertyError(token, "SafariZone has no property '$name'")
        }
    }

    override fun setProperty(name: String, value: Any?, errorHandler: EvaluatorErrorHandler, token: Token) {
        when (name) {
            "balls" -> {
                if (value !is Int) {
                    throw errorHandler.typeError(token, "balls must be an integer.")
                }
                balls = value
            }
            "turns" -> {
                if (value !is Int) {
                    throw errorHandler.typeError(token, "turns must be an integer.")
                }
                turns = value
            }
            else -> throw errorHandler.propertyError(token, "Cannot modify property '$name'.")
        }
    }

    override fun callMethod(name: String, args: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token): Any? {
        return when (name) {
            "useBall" -> {
                if (balls <= 0) {
                    throw errorHandler.error(token, "No Safari Balls remaining!")
                }
                balls--
                null
            }
            "useTurn" -> {
                if (turns <= 0) {
                    throw errorHandler.error(token, "No turns remaining!")
                }
                turns--
                null
            }
            "reset" -> {
                balls = initialBalls
                turns = initialTurns
                null
            }
            "isGameOver" -> balls <= 0 || turns <= 0
            else -> throw errorHandler.error(token, "SafariZone has no method '$name'. Use .pokemon-> for Pokemon management.")
        }
    }
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
 * @property pokemons Mutable list of Pokemon names on the team
 * @property maxSize Maximum number of Pokemon allowed (default 6)
 */
class TeamObject(
    val trainerName: String,
    val pokemons: MutableList<String> = mutableListOf(),
    val maxSize: Int = 6
) : CustomObjectInterface {
    override fun toString(): String = "Team($trainerName, ${pokemons.size}/${maxSize} Pokemon)"

    override fun getTypeName(): String = "Team"

    override fun getProperty(name: String, errorHandler: EvaluatorErrorHandler, token: Token): Any? {
        return when (name) {
            "trainerName" -> trainerName
            "maxSize" -> maxSize
            "pokemonCount" -> pokemons.size
            "pokemon" -> PokemonCollection(pokemons, "Team")
            else -> throw errorHandler.propertyError(token, "Team has no property '$name'")
        }
    }

    override fun setProperty(name: String, value: Any?, errorHandler: EvaluatorErrorHandler, token: Token) {
        throw errorHandler.propertyError(token, "Team properties are read-only.")
    }

    override fun callMethod(name: String, args: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token): Any? {
        return when (name) {
            "isFull" -> pokemons.size >= maxSize
            "isEmpty" -> pokemons.isEmpty()
            "has" -> {
                val pokemonName = requireStringArg(args, errorHandler, token, "has")
                hasPokemon(pokemonName)
            }
            else -> throw errorHandler.error(token, "Team has no method '$name'. Use .pokemon-> for Pokemon management.")
        }
    }

    /**
     * Checks if a Pokemon with the given name is on the team.
     * Case-insensitive search.
     *
     * @param name Pokemon name to search for
     * @return true if Pokemon is on team, false otherwise
     */
    fun hasPokemon(name: String): Boolean {
        return pokemons.any { it.equals(name, ignoreCase = true) }
    }

    private fun requireStringArg(args: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token, methodName: String): String {
        if (args.isEmpty()) {
            throw errorHandler.error(token, "Method '$methodName' requires a string argument.")
        }
        val arg = args[0]
        if (arg !is String) {
            throw errorHandler.typeError(token, "Method '$methodName' requires a string argument.")
        }
        return arg
    }
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
    private var environment = Environment()

    /** Error handler for generating and throwing runtime errors */
    val errorHandler = EvaluatorErrorHandler()

    init {
        // Register built-in constructors as special marker values
        // These are recognized in visitCallExpr to create objects
        environment.define("SafariZone", "BUILTIN_CONSTRUCTOR_SAFARIZONE")
        environment.define("Team", "BUILTIN_CONSTRUCTOR_TEAM")
        environment.define("Object", "BUILTIN_CONSTRUCTOR_OBJECT")
        environment.define("Pokemon", "BUILTIN_CONSTRUCTOR_POKEMON")
    }

    /**
     * Main entry point for evaluating an AST node.
     * Delegates to the appropriate visit method via the Visitor pattern.
     *
     * @param node The AST node to evaluate
     * @return The result of evaluation (type depends on node type)
     */
    fun evaluate(node: AstNode): Any? {
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
        environment.define(stmt.identifier.lexeme, value)
        return null
    }

    /**
     * Block statements are not yet implemented.
     */
    override fun visitBlock(block: Block): Any? {
        throw errorHandler.error(
            Token(TokenType.LEFT_BRACE, "{", null, 0),
            "Block statements are not yet implemented."
        )
    }

    /**
     * If statements are not yet implemented.
     */
    override fun visitIfStmt(stmt: IfStmt): Any? {
        throw errorHandler.error(
            Token(TokenType.IF_KEYWORD, "if", null, 0),
            "If statements are not yet implemented."
        )
    }

    /**
     * User-defined functions are not yet implemented.
     */
    override fun visitDefineStmt(stmt: DefineStmt): Any? {
        throw errorHandler.error(
            stmt.name,
            "User-defined functions are not yet implemented."
        )
    }

    /**
     * Explore statements are not yet implemented.
     */
    override fun visitExploreStmt(stmt: ExploreStmt): Any? {
        throw errorHandler.error(
            Token(TokenType.EXPLORE_KEYWORD, "explore", null, 0),
            "Explore statements are not yet implemented."
        )
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
        return stmt.expression.accept(this)
    }

    /**
     * Return statements are not yet implemented.
     */
    override fun visitReturnStmt(stmt: ReturnStmt): Any? {
        throw errorHandler.error(
            stmt.keyword,
            "Return statements are not yet implemented."
        )
    }

    /**
     * Executes a run statement (loop control).
     * Currently a placeholder - would implement "continue" in a loop.
     *
     * @param stmt The run statement
     * @return null
     */
    override fun visitRunStmt(stmt: RunStmt): Any? {
        // In a real interpreter, this would continue a loop
        return null
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
                checkNumberOperand(expr.operator, right)
                when (right) {
                    is Double -> -right
                    is Int -> -right
                    else -> 0 // Should never reach here due to check
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
    override fun visitBinaryExpr(expr: BinaryExpr): Any? {
        val left = expr.left.accept(this)

        // Short-circuit evaluation for logical operators
        // Don't evaluate right side if result is already determined
        when (expr.operator.type) {
            TokenType.AND -> return if (!isTruthy(left)) false else isTruthy(expr.right.accept(this))
            TokenType.OR -> return if (isTruthy(left)) true else isTruthy(expr.right.accept(this))
            else -> {}
        }

        val right = expr.right.accept(this)

        return when (expr.operator.type) {
            // Arithmetic operators
            TokenType.PLUS -> {
                when {
                    left is Double && right is Double -> left + right
                    left is Int && right is Int -> left + right
                    left is Double && right is Int -> left + right
                    left is Int && right is Double -> left + right
                    left is String && right is String -> left + right
                    // Allow string concatenation with any type
                    left is String -> left + stringify(right)
                    right is String -> stringify(left) + right
                    else -> throw errorHandler.typeError(
                        expr.operator,
                        "Operands must be two numbers or two strings."
                    )
                }
            }
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                when {
                    left is Double && right is Double -> left - right
                    left is Int && right is Int -> left - right
                    left is Double && right is Int -> left - right
                    left is Int && right is Double -> left - right
                    else -> 0 // Should never reach here due to check
                }
            }
            TokenType.MULTIPLY -> {
                checkNumberOperands(expr.operator, left, right)
                when {
                    left is Double && right is Double -> left * right
                    left is Int && right is Int -> left * right
                    left is Double && right is Int -> left * right
                    left is Int && right is Double -> left * right
                    else -> 0
                }
            }
            TokenType.DIVIDE -> {
                checkNumberOperands(expr.operator, left, right)
                val divisor = when (right) {
                    is Double -> right
                    is Int -> right.toDouble()
                    else -> 0.0
                }
                if (divisor == 0.0) {
                    throw errorHandler.error(expr.operator, "Division by zero.")
                }
                when (left) {
                    is Double -> left / divisor
                    is Int -> left / divisor
                    else -> 0.0
                }
            }
            TokenType.MODULO -> {
                checkNumberOperands(expr.operator, left, right)
                when {
                    left is Int && right is Int -> {
                        if (right == 0) {
                            throw errorHandler.error(expr.operator, "Division by zero.")
                        }
                        left % right
                    }
                    left is Double && right is Double -> {
                        if (right == 0.0) {
                            throw errorHandler.error(expr.operator, "Division by zero.")
                        }
                        left % right
                    }
                    left is Int && right is Double -> {
                        if (right == 0.0) {
                            throw errorHandler.error(expr.operator, "Division by zero.")
                        }
                        left % right
                    }
                    left is Double && right is Int -> {
                        if (right == 0) {
                            throw errorHandler.error(expr.operator, "Division by zero.")
                        }
                        left % right
                    }
                    else -> throw errorHandler.typeError(
                        expr.operator,
                        "Modulo operator requires numeric operands."
                    )
                }
            }
            // Comparison operators
            TokenType.GREATER_THAN -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) > toDouble(right)
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) >= toDouble(right)
            }
            TokenType.LESS_THAN -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) < toDouble(right)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                toDouble(left) <= toDouble(right)
            }

            // Equality operators
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            TokenType.NOT_EQUAL -> !isEqual(left, right)

            else -> throw errorHandler.error(
                expr.operator,
                "Unknown binary operator '${expr.operator.lexeme}'."
            )
        }
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
                // Handle property assignment for objects
                val obj = expr.target.primaryWithSuffixes.accept(this)
                val propertyName = expr.target.identifier.lexeme

                when (obj) {
                    is SafariZoneObject -> {
                        when (propertyName) {
                            "balls" -> {
                                if (value !is Int) {
                                    throw errorHandler.typeError(
                                        expr.target.identifier,
                                        "balls must be an integer."
                                    )
                                }
                                obj.balls = value
                            }
                            "turns" -> {
                                if (value !is Int) {
                                    throw errorHandler.typeError(
                                        expr.target.identifier,
                                        "turns must be an integer."
                                    )
                                }
                                obj.turns = value
                            }
                            else -> throw errorHandler.propertyError(
                                expr.target.identifier,
                                "Cannot modify property '$propertyName'."
                            )
                        }
                    }
                    is TeamObject -> {
                        throw errorHandler.propertyError(
                            expr.target.identifier,
                            "Team properties are read-only."
                        )
                    }
                    else -> throw errorHandler.typeError(
                        expr.equals,
                        "Cannot access property on non-object type."
                    )
                }
            }
            else -> {
                throw errorHandler.error(
                    expr.equals,
                    "Invalid assignment target."
                )
            }
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
        // Handle method calls on objects FIRST (before evaluating callee)
        // This prevents treating methods as properties
        if (expr.callee is PropertyAccessExpr) {
            val propertyExpr = expr.callee as PropertyAccessExpr
            val obj = propertyExpr.primaryWithSuffixes.accept(this)
            val methodName = propertyExpr.identifier.lexeme

            // Evaluate arguments
            val arguments = expr.args.map { it.accept(this) }

            // Use the CustomObjectInterface for unified method calling
            when (obj) {
                is CustomObjectInterface -> {
                    return obj.callMethod(methodName, arguments, errorHandler, propertyExpr.identifier)
                }

                else -> throw errorHandler.typeError(
                    propertyExpr.identifier,
                    "Cannot call method on non-object type."
                )
            }
        }

        // Evaluate arguments
        val arguments = expr.args.map { it.accept(this) }


        // Handle built-in constructors
        if (expr.callee is VariableExpr) {
            val calleeVar = expr.callee as VariableExpr
            val funcName = calleeVar.identifier.lexeme

            when (funcName) {
                "SafariZone" -> {
                    // Supports: SafariZone(), SafariZone(30, 500), SafariZone(balls=30, turns=500)

                    val defaults = mapOf("balls" to 10, "turns" to 10)
                    val params = resolveArguments(
                        calleeVar.identifier,
                        listOf("balls", "turns"),
                        arguments,
                        expr.namedArgs,
                        defaults
                    )

                    val balls = params["balls"]
                    if (balls !is Int) {
                        throw errorHandler.typeError(
                            calleeVar.identifier,
                            "SafariZone balls must be an integer."
                        )
                    }

                    val turns = params["turns"]
                    if (turns !is Int) {
                        throw errorHandler.typeError(
                            calleeVar.identifier,
                            "SafariZone turns must be an integer."
                        )
                    }

                    return SafariZoneObject(balls, turns)
                }

                "Team" -> {
                    // Team(trainerName: String, maxSize: Int = 6)
                    // Supports: Team("Ash"), Team("Ash", 8), Team(trainerName="Ash", maxSize=8)

                    val defaults = mapOf("maxSize" to 6)  // trainerName is required
                    val params = resolveArguments(
                        calleeVar.identifier,
                        listOf("trainerName", "maxSize"),
                        arguments,
                        expr.namedArgs,
                        defaults
                    )

                    val trainerName = params["trainerName"]
                    if (trainerName !is String) {
                        throw errorHandler.typeError(
                            calleeVar.identifier,
                            "Team trainerName must be a string."
                        )
                    }

                    val maxSize = params["maxSize"]
                    if (maxSize !is Int) {
                        throw errorHandler.typeError(
                            calleeVar.identifier,
                            "Team maxSize must be an integer."
                        )
                    }

                    return TeamObject(trainerName, mutableListOf(), maxSize)
                }
            }
        }

        throw errorHandler.error(
            Token(TokenType.EOF, "", null, 0),
            "Unknown function or not implemented."
        )
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
        val propertyName = node.identifier.lexeme

        return when (obj) {
            is SafariZoneObject -> {
                when (propertyName) {
                    "initialBalls" -> obj.initialBalls
                    "initialTurns" -> obj.initialTurns
                    "balls" -> obj.balls
                    "turns" -> obj.turns
                    "pokemon" -> obj.getProperty(propertyName, errorHandler, node.identifier)
                    else -> {
                        // Check if it's a resource management method
                        val methodNames = setOf("useBall", "useTurn", "reset", "isGameOver")
                        val collectionMethods = setOf("add", "remove", "list", "find", "random",
                                                      "count", "clear", "isEmpty")
                        if (propertyName in methodNames) {
                            throw errorHandler.error(
                                node.identifier,
                                "Cannot access method '$propertyName' with dot operator. Use '->' for methods: myZone->$propertyName()"
                            )
                        }
                        if (propertyName in collectionMethods) {
                            throw errorHandler.error(
                                node.identifier,
                                "Collection methods moved to .pokemon property. Use: myZone.pokemon->$propertyName()"
                            )
                        }
                        throw errorHandler.propertyError(
                            node.identifier,
                            "SafariZone has no property '$propertyName'"
                        )
                    }
                }
            }

            is TeamObject -> {
                when (propertyName) {
                    "trainerName" -> obj.trainerName
                    "maxSize" -> obj.maxSize
                    "pokemon" -> obj.getProperty(propertyName, errorHandler, node.identifier)
                    else -> {
                        // Check if it's a team method
                        val methodNames = setOf("isFull", "isEmpty", "has")
                        val collectionMethods = setOf("add", "remove", "list", "find", "random",
                                                      "count", "clear")
                        if (propertyName in methodNames) {
                            throw errorHandler.error(
                                node.identifier,
                                "Cannot access method '$propertyName' with dot operator. Use '->' for methods: myTeam->$propertyName()"
                            )
                        }
                        if (propertyName in collectionMethods) {
                            throw errorHandler.error(
                                node.identifier,
                                "Collection methods moved to .pokemon property. Use: myTeam.pokemon->$propertyName()"
                            )
                        }
                        throw errorHandler.propertyError(
                            node.identifier,
                            "Team has no property '$propertyName'"
                        )
                    }
                }
            }

            is PokemonCollection -> {
                // PokemonCollection has no properties, only methods
                val methodNames = setOf("add", "remove", "list", "find", "random",
                                       "count", "clear", "isEmpty")
                if (propertyName in methodNames) {
                    throw errorHandler.error(
                        node.identifier,
                        "Cannot access method '$propertyName' with dot operator. Use '->': .pokemon->$propertyName()"
                    )
                }
                throw errorHandler.error(
                    node.identifier,
                    "PokemonCollection has no properties. Use methods with '->'. Available methods: add, remove, list, find, random, count, clear, isEmpty"
                )
            }

            else -> throw errorHandler.typeError(
                node.identifier,
                "Cannot access property on non-object type"
            )
        }
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
     * Checks if two values are equal using value equality.
     *
     * Equality rules:
     * - null == null → true
     * - null == anything else → false
     * - Otherwise uses Kotlin's == (value equality)
     *
     * @param a First value
     * @param b Second value
     * @return true if values are equal
     */
    private fun isEqual(a: Any?, b: Any?): Boolean {
        if (a == null && b == null) return true
        if (a == null) return false
        return a == b
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

    /**
     * Converts a number to Double for numeric comparisons.
     * Handles both Int and Double types.
     *
     * @param value The number to convert
     * @return Double representation
     * @throws IllegalArgumentException if value is not a number
     */
    private fun toDouble(value: Any?): Double {
        return when (value) {
            is Double -> value
            is Int -> value.toDouble()
            else -> throw IllegalArgumentException("Cannot convert to double")
        }
    }

    /**
     * Type checking for unary operators.
     * Ensures the operand is a number (Int or Double).
     *
     * @param operator The operator token (for error reporting)
     * @param operand The operand value
     * @throws RuntimeError if operand is not a number
     */
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double || operand is Int) return
        throw errorHandler.typeError(operator, "Operand must be a number.")
    }

    /**
     * Type checking for binary operators.
     * Ensures both operands are numbers (Int or Double).
     *
     * @param operator The operator token (for error reporting)
     * @param left The left operand
     * @param right The right operand
     * @throws RuntimeError if either operand is not a number
     */
    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if ((left is Double || left is Int) && (right is Double || right is Int)) return
        throw errorHandler.typeError(operator, "Operands must be numbers.")
    }

    /**
     * Helper to validate and extract string argument for methods.
     * Checks that the first argument exists and is a string.
     *
     * @param arguments List of arguments
     * @param token Token for error reporting
     * @param methodName Method name for error messages
     * @return The string argument
     * @throws RuntimeError if argument is missing or not a string
     */
    private fun requireStringArg(arguments: List<Any?>, token: Token, methodName: String): String {
        if (arguments.isEmpty() || arguments[0] !is String) {
            throw errorHandler.argumentError(token, "$methodName() requires a string argument.")
        }
        return arguments[0] as String
    }

    /**
     * Resolves function arguments by merging positional args, named args, and defaults.
     * Supports Python-like named arguments: func(a=1, b=2) or func(1, b=2)
     *
     * Process:
     * 1. Apply positional arguments in order
     * 2. Apply named arguments (can override or add to positional)
     * 3. Fill missing parameters with defaults
     * 4. Error if any required parameter is missing
     *
     * Example:
     * ```
     * SafariZone(30, turns=500)
     * → balls=30 (positional), turns=500 (named)
     * ```
     *
     * @param token Token for error reporting
     * @param paramNames List of parameter names in order
     * @param positionalArgs List of positional argument values
     * @param namedArgs List of named arguments
     * @param defaults Map of parameter names to default values
     * @return Map of parameter names to resolved values
     * @throws RuntimeError if arguments are invalid or required parameters missing
     */
    private fun resolveArguments(
        token: Token,
        paramNames: List<String>,
        positionalArgs: List<Any?>,
        namedArgs: List<NamedArg>,
        defaults: Map<String, Any?> = emptyMap()
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        // 1. Apply positional arguments
        if (positionalArgs.size > paramNames.size) {
            throw errorHandler.argumentError(
                token,
                "Too many arguments: expected ${paramNames.size}, got ${positionalArgs.size}"
            )
        }

        for (i in positionalArgs.indices) {
            result[paramNames[i]] = positionalArgs[i]
        }

        // 2. Apply named arguments
        for (namedArg in namedArgs) {
            val argName = namedArg.name.lexeme

            if (argName !in paramNames) {
                throw errorHandler.argumentError(
                    namedArg.name,
                    "Unknown parameter '$argName'"
                )
            }

            if (argName in result) {
                throw errorHandler.argumentError(
                    namedArg.name,
                    "Parameter '$argName' specified multiple times"
                )
            }

            result[argName] = namedArg.value.accept(this)
        }

        // 3. Apply defaults for missing parameters
        for (paramName in paramNames) {
            if (paramName !in result) {
                if (paramName in defaults) {
                    result[paramName] = defaults[paramName]
                } else {
                    throw errorHandler.argumentError(
                        token,
                        "Missing required parameter '$paramName'"
                    )
                }
            }
        }

        return result
    }
}