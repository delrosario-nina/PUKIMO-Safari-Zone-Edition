package evaluator

import lexer.*

/**
 * Represents a lexical environment for variable storage.
 * Implements nested scope chains using the enclosing environment pattern.
 * Variables can shadow outer scope variables with the same name.
 */
class Environment(
    private val enclosing: Environment? = null,
    private val errorHandler: EvaluatorErrorHandler? = null
) {
    private val values: MutableMap<String, Any?> = mutableMapOf()

    /**
     * Defines a new variable in the current scope.
     * Throws an error if the variable already exists in THIS scope.
     * Shadowing outer scope variables is still allowed.
     */
    fun define(name: Token, value: Any?) {
        if (name.lexeme in values) {
            throw errorHandler?.nameError(
                name,
                "Variable '${name.lexeme}' already declared in this scope",
                hints = listOf("Variables can only be declared once per scope")
            ) ?: RuntimeError(
                name,
                "Variable '${name.lexeme}' already declared in this scope",
                phase = ErrorPhase.NAME
            )
        }
        values[name.lexeme] = value
    }

    /**
     * Retrieves a variable's value by searching through the scope chain.
     * Searches current scope first, then recursively searches enclosing scopes.
     */
    fun get(name: Token): Any? {
        // Return value if found (even if null)
        values[name.lexeme]?.let { return it }

        // Check if key exists with null value (important distinction!)
        if (name.lexeme in values) {
            return null
        }

        // Recursively search enclosing scopes
        enclosing?.let { return it.get(name) }

        // Variable not found - throw error
        throw errorHandler?.nameError(
            name,
            "Undefined variable '${name.lexeme}'",
            hints = listOf("Did you forget to declare this variable with 'var'?")
        ) ?: RuntimeError(
            name,
            "Undefined variable '${name.lexeme}'",
            phase = ErrorPhase.NAME
        )
    }

    /**
     * Assigns a value to an existing variable in the scope chain.
     * Searches current scope first, then recursively searches enclosing scopes.
     */
    fun assign(name: Token, value: Any?) {
        if (name.lexeme in values) {
            values[name.lexeme] = value
            return
        }

        // Try assigning in enclosing scope
        enclosing?.let {
            it.assign(name, value)
            return
        }

        // Variable not found - throw error
        throw errorHandler?.nameError(
            name,
            "Cannot assign to undefined variable '${name.lexeme}'",
            hints = listOf("Variable must be declared with 'var' before assignment")
        ) ?: RuntimeError(
            name,
            "Cannot assign to undefined variable '${name.lexeme}'",
            phase = ErrorPhase.NAME
        )
    }
}
