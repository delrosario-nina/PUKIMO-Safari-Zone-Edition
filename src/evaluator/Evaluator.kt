package evaluator

import lexer.*
import parser.*

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
 *The only user-exposed repetition is via the explore statement, which repeats its block as part of the Safari game logic.
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
     * Evaluates a block statement (a sequence of statements in a new scope)
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
     * Evaluates an if statement (conditional).
     * Evaluates the condition expression and executes the thenBlock
     * if true, or the elseBlock if false (if present).
     *
     * Example:
     * if (x > 10) {
     *     print("Large");
     * } else {
     *     print("Small");
     * }
     *
     * @param stmt The if statement
     * @return The value of the executed block (then or else), or null
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
     * Defines a function.
     * Creates a FunctionObject and stores it in the environment.
     *
     * Example:
     * define myFunc(a, b) {
     *     print(a + b);
     * }
     *
     * @param stmt The function definition statement
     * @return null (function definitions don't produce values)
     */
    override fun visitDefineStmt(stmt: DefineStmt): Any? {
        // Create a function object that captures the current environment
        val function = FunctionObject(
            name = stmt.name,
            parameters = stmt.paramList,
            body = stmt.block,
            closure = environment
        )

        // Store the function in the environment
        environment.define(stmt.name, function)
        return null
    }



    /**
     * Executes an explore statement.
     * Repeats the block while there are turns left in the SafariZone object.
     *
     * Example:
     * explore myZone {
     *     print("Exploring...");
     * }
     *
     * @param stmt The explore statement
     * @return null
     * @throws RuntimeError if the variable is not a SafariZoneObject
     */
    override fun visitExploreStmt(stmt: ExploreStmt): Any? {
        val safariZoneObj = environment.get(stmt.safariZoneVar)
        if (safariZoneObj !is SafariZoneObject) {
            throw errorHandler.typeError(
                stmt.safariZoneVar,
                "Explore expects a SafariZone object for '${stmt.safariZoneVar.lexeme}'"
            )
        }

        val previous = environment
        var endedByRun = false // <--- Flag to track early exit
        try {
            environment = Environment(enclosing = previous)
            environment.define(stmt.safariZoneVar, safariZoneObj)

            // Define "encounter" variable ONCE in this scope before the loop
            val encounterToken = Token(TokenType.IDENTIFIER, "encounter", null, 0)
            environment.define(encounterToken, null)

            while (safariZoneObj.turns > 0) {
                safariZoneObj.turns--

                val pokemonCollection = safariZoneObj.getProperty("pokemon", errorHandler, Token(TokenType.IDENTIFIER, "pokemon", null, 0)) as PokemonCollection
                if (pokemonCollection.isEmpty()) {
                    println("No Pokemon left to encounter!")
                    break
                }
                val encounter = pokemonCollection.random(errorHandler, encounterToken)
                environment.assign(encounterToken, encounter) // Just assign, do NOT define again


                try {
                    stmt.block.accept(this)
                } catch (e: RunException) {
                    endedByRun = true
                    break
                }
            }
            if (safariZoneObj.turns==0 && !endedByRun) {
                println("Explore: Out of turns!")
            }
            return null
        } finally {
            environment = previous
        }
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
    private fun evaluateBuiltinFunction(expr: CallExpr): Any? {
        if (expr.callee is VariableExpr) {
            val name = (expr.callee as VariableExpr).identifier.lexeme
            when (name) {
                "readString" -> {
                    if (expr.args.isEmpty()) {
                        print("> ")
                        val input = readlnOrNull()
                        // If you want the value to never be null/empty
                        if (input == null || input.isEmpty()) {
                            throw errorHandler.error(
                                (expr.callee as VariableExpr).identifier,
                                "Input for readString() cannot be empty.",
                                ErrorPhase.RUNTIME
                            )
                        }
                        return input
                    }
                }
                "readInt" -> {
                    if (expr.args.isEmpty()) {
                        print("> ")
                        val input = readlnOrNull()
                        if (input == null || input.isEmpty()) {
                            throw errorHandler.error(
                                (expr.callee as VariableExpr).identifier,
                                "Input for readInt() cannot be empty.",
                                ErrorPhase.RUNTIME
                            )
                        }
                        val intVal = input.toIntOrNull()
                        if (intVal == null) {
                            throw errorHandler.error(
                                (expr.callee as VariableExpr).identifier,
                                "Input '$input' is not a valid integer for readInt().",
                                ErrorPhase.RUNTIME
                            )
                        }
                        return intVal
                    }
                }
                "length" -> {
                    if (expr.args.size == 1) {
                        val arg = expr.args[0].accept(this)
                        return when (arg) {
                            is String -> arg.length
                            is SafariZoneObjectInterface -> arg.getProperty("pokemonCount", errorHandler, (expr.callee as VariableExpr).identifier)
                            is Collection<*> -> arg.size
                            else -> throw errorHandler.typeError((expr.callee as VariableExpr).identifier, "length() only works for strings, SafariZone, collections - not ${arg?.javaClass?.simpleName}")
                        }
                    }
                }
                // Add other built-ins here!
            }
        }
        return null
    }

    override fun visitCallExpr(expr: CallExpr): Any? {
        val builtinResult = evaluateBuiltinFunction(expr)
        if (builtinResult != null) return builtinResult

        // Handle method calls on objects
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
            environment = Environment(enclosing = previous)
            // Bind parameters to arguments
            for ((param, arg) in function.parameters.zip(arguments)) {
                if (!typeMatches(param.type, arg)) {
                    throw errorHandler.typeError(
                        param.name,
                        "Type mismatch: parameter '${param.name.lexeme}' expects ${param.type}, got ${describeType(arg)}"
                    )
                }
                environment.define(param.name, arg)
            }

            // Execute body, catching return statements
            try {
                function.body.accept(this)
                return null
            } catch (e: ReturnException) {
                return e.value
            }
        } finally {
            environment = previous
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

private fun typeMatches(type: Type, value: Any?): Boolean {
    return when (type) {
        Type.INT        -> value is Int
        Type.DOUBLE     -> value is Double
        Type.STRING     -> value is String
        Type.BOOL       -> value is Boolean
        Type.SAFARIZONE -> value is SafariZoneObject
        Type.TEAM       -> value is TeamObject
        Type.OBJECT     -> value != null // generic, could refine
        Type.POKEMON    -> value != null // example, refine as needed
    }
}

private fun describeType(value: Any?): String {
    return when (value) {
        null                -> "null"
        is Int              -> "int"
        is Double           -> "double"
        is String           -> "string"
        is Boolean          -> "bool"
        is SafariZoneObject -> "SafariZoneObject"
        is TeamObject       -> "TeamObject"
        else                -> value?.javaClass?.simpleName ?: "unknown"
    }
}