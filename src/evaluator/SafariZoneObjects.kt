// kotlin
package evaluator

import lexer.Token
import parser.*

/**
 * Handles creation of built-in object types (SafariZone, Team).
 * Centralizes constructor logic and argument resolution.
 */


class SafariZoneObjects(
    private val errorHandler: EvaluatorErrorHandler,
    private val evaluator: Evaluator
) {
    // ...
    /**
     * Resolves positional and named arguments into a parameter map.
     * Handles defaults and validates argument count.
     *
     * namedArgs: List of Pair(Token, evaluatedValue)
     */
    private fun resolveArguments(
        token: Token,
        paramNames: List<String>,
        positionalArgs: List<Any?>,
        namedArgs: List<Pair<Token, Any?>>,
        defaults: Map<String, Any?>
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        // Check for too many positional arguments
        if (positionalArgs.size > paramNames.size) {
            throw errorHandler.argumentError(
                token,
                "Expected at most ${paramNames.size} arguments, got ${positionalArgs.size}"
            )
        }

        // Assign positional arguments
        positionalArgs.forEachIndexed { index, value ->
            result[paramNames[index]] = value
        }

        // Assign named arguments
        for (namedArg in namedArgs) {
            val paramName = namedArg.first.lexeme

            if (paramName !in paramNames) {
                throw errorHandler.argumentError(
                    namedArg.first,
                    "Unknown parameter '$paramName'"
                )
            }

            if (paramName in result) {
                throw errorHandler.argumentError(
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
    fun tryCreate(
        name: String,
        positionalArgs: List<Any?>,
        namedArgs: List<NamedArg>,
        token: Token
    ): Any? {

        // Evaluate named argument expressions into (Token, Any?) pairs
        val evaluatedNamedArgs: List<Pair<Token, Any?>> = namedArgs.map { namedArg ->
            val evaluatedValue = evaluator.evaluate(namedArg.value, isReplMode = false)
            Pair(namedArg.name, evaluatedValue)
        }


        // Use evaluatedNamedArgs in your constructor calls
        return when (name) {
            "SafariZone" -> createSafariZone(positionalArgs, evaluatedNamedArgs, token)
            "Team" -> createTeam(positionalArgs, evaluatedNamedArgs, token)
            else -> null
        }
    }



    /**
     * Creates a SafariZone object with balls and turns parameters.
     * SafariZone(balls: Int = 10, turns: Int = 10)
     */
    private fun createSafariZone(
        positionalArgs: List<Any?>,
        namedArgs: List<Pair<Token, Any?>>,
        token: Token
    ): SafariZoneObject {
        val defaults = mapOf(
            "balls" to 10,
            "turns" to 10
        )

        val params = resolveArguments(
            token,
            listOf("balls", "turns"),
            positionalArgs,
            namedArgs,
            defaults
        )

        val balls = params["balls"] as? Int
            ?: throw errorHandler.typeError(token, "Parameter 'balls' must be Int")

        val turns = params["turns"] as? Int
            ?: throw errorHandler.typeError(token, "Parameter 'turns' must be Int")

        if (balls < 0) {
            throw errorHandler.argumentError(token, "balls cannot be negative")
        }

        if (turns < 0) {
            throw errorHandler.argumentError(token, "turns cannot be negative")
        }

        return SafariZoneObject(balls, turns)
    }

    /**
     * Creates a Team object with trainerName and maxSize parameters.
     * Team(trainerName: String, maxSize: Int = 6)
     */
    private fun createTeam(
        positionalArgs: List<Any?>,
        namedArgs: List<Pair<Token, Any?>>,
        token: Token
    ): TeamObject {
        val defaults = mapOf<String, Any?>(
            "maxSize" to 6
        )

        // parameter names must match defaults: trainerName and maxSize
        val params = resolveArguments(
            token,
            listOf("trainerName", "maxSize"),
            positionalArgs,
            namedArgs,
            defaults
        )

        val trainerName = params["trainerName"] as? String
            ?: throw errorHandler.typeError(token, "Parameter 'trainerName' must be String")

        val teamSize = params["maxSize"] as? Int
            ?: throw errorHandler.typeError(token, "Parameter 'maxSize' must be Int")

        if (teamSize < 1) {
            throw errorHandler.argumentError(token, "maxSize must be at least 1")
        }

        return TeamObject(trainerName, mutableListOf(), teamSize)
    }
}
