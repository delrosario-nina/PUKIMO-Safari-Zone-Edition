package evaluator
import evaluator.EvaluatorErrorHandler
import evaluator.RuntimeError
import lexer.Token


/**
 * SafariZoneObjectInterface - Base interface for all custom objects in the language.
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
interface SafariZoneObjectInterface {
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