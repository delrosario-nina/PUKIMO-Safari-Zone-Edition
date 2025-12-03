package evaluator

import lexer.Token

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
