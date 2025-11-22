package evaluator

import lexer.Token

/*
 * Represents a lexical environment for variable storage.
 * Implements nested scope chains using the enclosing environment pattern.
 * Variables can shadow outer scope variables with the same name.
 */
class Environment(private val enclosing: Environment? = null) {
    private val values: MutableMap<String, Any?> = mutableMapOf()

    /*
     * Defines a new variable in the current scope.
     * If the variable already exists in this scope, it will be overwritten.
     * This allows redeclaration in the same scope (shadowing in inner scopes).
     */
    fun define(name: String, value: Any?) {
        values[name] = value
    }

    /*
     * Retrieves a variable's value by searching through the scope chain.
     * Searches current scope first, then recursively searches enclosing scopes.
     */
    fun get(name: Token): Any? {
        values[name.lexeme]?.let { return it }

        // Check if key exists with null value (important distinction!)
        if (name.lexeme in values) {
            return null
        }

        // Recursively search enclosing scopes
        enclosing?.let { return it.get(name) }

        // Variable not found in any scope - throw NameError
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'", ErrorPhase.NAME)
    }

    /*
     * Assigns a value to an existing variable in the scope chain.
     * Searches current scope first, then recursively searches enclosing scopes.
     */
    fun assign(name: Token, value: Any?) {
        // Optimized: single containsKey check
        if (name.lexeme in values) {
            values[name.lexeme] = value
            return
        }

        enclosing?.let {
            it.assign(name, value)
            return
        }

        // Variable not found in any scope - throw NameError
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'", ErrorPhase.NAME)
    }
}
