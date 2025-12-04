package evaluator

import lexer.Token
import parser.*

/**
 * Handles creation of built-in object types (SafariZone, Team).
 * Uses a registry pattern for extensibility and reduced duplication.
 */
class SafariZoneObjects(
    private val errorHandler: EvaluatorErrorHandler,
    private val evaluator: Evaluator
) {

    // ========== Constructor Registry ==========

    private val constructors: Map<String, ConstructorDefinition> = mapOf(
        "SafariZone" to ConstructorDefinition(
            paramNames = listOf(SafariZoneParams.BALLS, SafariZoneParams.TURNS),
            defaults = SafariZoneParams.DEFAULTS,
            factory = ::createSafariZone
        ),
        "Team" to ConstructorDefinition(
            paramNames = listOf(TeamParams.TRAINER_NAME, TeamParams.MAX_SIZE),
            defaults = TeamParams.DEFAULTS,
            factory = ::createTeam
        )
    )

    /**
     * Attempts to create a built-in object by name.
     * Returns the created object or null if name is not recognized.
     */
    fun tryCreate(
        name: String,
        positionalArgs: List<Any?>,
        namedArgs: List<NamedArg>,
        token: Token
    ): Any? {
        val constructor = constructors[name] ?: return null

        // Evaluate named arguments
        val evaluatedNamedArgs = namedArgs.map { namedArg ->
            Pair(namedArg.name, evaluator.evaluate(namedArg.value))
        }

        // Resolve all arguments
        val resolvedArgs = resolveArguments(
            token,
            constructor.paramNames,
            positionalArgs,
            evaluatedNamedArgs,
            constructor.defaults
        )

        // Call factory function
        return constructor.factory(resolvedArgs, token)
    }

    // ========== Argument Resolution ==========

    /**
     * Resolves positional and named arguments into a parameter map.
     * Handles defaults and validates argument count.
     */
    private fun resolveArguments(
        token: Token,
        paramNames: List<String>,
        positionalArgs: List<Any?>,
        namedArgs: List<Pair<Token, Any?>>,
        defaults: Map<String, Any?>
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        // Validate positional argument count
        if (positionalArgs.size > paramNames.size) {
            throw errorHandler.argumentError(
                token,
                "Expected at most ${paramNames. size} arguments, got ${positionalArgs.size}"
            )
        }

        // Assign positional arguments
        positionalArgs. forEachIndexed { index, value ->
            result[paramNames[index]] = value
        }

        // Assign named arguments
        for (namedArg in namedArgs) {
            val paramName = namedArg. first.lexeme

            if (paramName !in paramNames) {
                throw errorHandler.argumentError(
                    namedArg.first,
                    "Unknown parameter '$paramName'"
                )
            }

            if (paramName in result) {
                throw errorHandler. argumentError(
                    namedArg.first,
                    "Parameter '$paramName' already specified"
                )
            }

            result[paramName] = namedArg.second
        }

        // Fill in defaults for missing parameters
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

    // ========== Constructor Implementations ==========

    /**
     * Creates a SafariZone object.
     * SafariZone(balls: Int = 10, turns: Int = 10)
     */
    private fun createSafariZone(params: Map<String, Any?>, token: Token): SafariZoneObject {
        val balls = getRequiredParam<Int>(params, SafariZoneParams.BALLS, token)
        val turns = getRequiredParam<Int>(params, SafariZoneParams.TURNS, token)

        validateNonNegative(balls, SafariZoneParams.BALLS, token)
        validateNonNegative(turns, SafariZoneParams.TURNS, token)

        return SafariZoneObject(balls, turns)
    }

    /**
     * Creates a Team object.
     * Team(trainerName: String, maxSize: Int = 6)
     */
    private fun createTeam(params: Map<String, Any?>, token: Token): TeamObject {
        val trainerName = getRequiredParam<String>(params, TeamParams. TRAINER_NAME, token)
        val maxSize = getRequiredParam<Int>(params, TeamParams.MAX_SIZE, token)

        if (maxSize < 1) {
            throw errorHandler. argumentError(token, "${TeamParams.MAX_SIZE} must be at least 1")
        }

        return TeamObject(trainerName, maxSize)
    }

    // ========== Helper Methods ==========

    /**
     * Extracts and validates a required parameter from the parameter map.
     */
    private inline fun <reified T> getRequiredParam(
        params: Map<String, Any?>,
        name: String,
        token: Token
    ): T {
        val value = params[name]
        if (value !is T) {
            val typeName = when (T::class) {
                Int::class -> "Int"
                String::class -> "String"
                Boolean::class -> "Boolean"
                else -> T::class.simpleName ?: "Unknown"
            }
            throw errorHandler.typeError(token, "Parameter '$name' must be $typeName")
        }
        return value
    }

    /**
     * Validates that an integer parameter is non-negative.
     */
    private fun validateNonNegative(value: Int, paramName: String, token: Token) {
        if (value < 0) {
            throw errorHandler.argumentError(token, "$paramName cannot be negative")
        }
    }

    // ========== Data Classes & Constants ==========

    /**
     * Represents a constructor definition with parameters, defaults, and factory function.
     */
    private data class ConstructorDefinition(
        val paramNames: List<String>,
        val defaults: Map<String, Any?>,
        val factory: (Map<String, Any?>, Token) -> SafariZoneObjectInterface
    )

    /**
     * Parameter names and defaults for SafariZone constructor.
     */
    private object SafariZoneParams {
        const val BALLS = "balls"
        const val TURNS = "turns"

        val DEFAULTS = mapOf(
            BALLS to 10,
            TURNS to 10
        )
    }

    /**
     * Parameter names and defaults for Team constructor.
     */
    private object TeamParams {
        const val TRAINER_NAME = "trainerName"
        const val MAX_SIZE = "maxSize"

        val DEFAULTS = mapOf<String, Any?>(
            MAX_SIZE to 6
        )
    }
}