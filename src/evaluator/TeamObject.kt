package evaluator

import lexer.Token

/**
 * TeamObject - Represents a Pokemon trainer's team.
 *
 * A team has a maximum size (default 6, like in Pokemon games) and stores
 * the trainer's Pokemon.  Teams have methods for managing Pokemon and checking
 * team status.
 *
 * Properties:
 * - trainerName: Name of the Pokemon trainer (immutable)
 * - maxSize: Maximum team size (default 6, immutable)
 * - pokemonCount: Current number of Pokemon on the team (read-only)
 * - pokemon: Pokemon collection wrapper for managing team Pokemon
 *
 * Methods:
 * - isFull(): Returns true if team has reached maximum size
 * - isEmpty(): Returns true if team has no Pokemon
 * - has(name): Returns true if team contains a Pokemon with given name (case-insensitive)
 *
 * Example usage:
 * ```
 * var team = Team("Ash");
 * team.pokemon->add("Pikachu");
 * team.pokemon->add("Charizard");
 * print(team.pokemon->list());  // "Pikachu, Charizard"
 * print(team.isFull());         // false
 * print(team.has("Pikachu"));    // true
 * ```
 *
 * @property trainerName The name of the Pokemon trainer
 * @property maxSize Maximum number of Pokemon allowed (default 6)
 */
class TeamObject(
    val trainerName: String,
    val maxSize: Int = 6
) : SafariZoneObjectInterface {

    /**
     * Internal list of Pokemon on the team.
     * Managed through the pokemon property's PokemonCollection wrapper.
     */
    private val pokemons: MutableList<String> = mutableListOf()

    // ========== Lazy-Initialized Pokemon Collection ==========

    /**
     * Cached Pokemon collection wrapper.
     * Created once and reused for consistent object identity.
     */
    private var pokemonCollection: PokemonCollection? = null

    private fun getPokemonCollection(): PokemonCollection {
        return pokemonCollection ?: PokemonCollection(pokemons, "Team").also {
            pokemonCollection = it
        }
    }

    // ========== Property Access ==========

    /**
     * Property getters map.
     * Provides read access to Team properties.
     */
    private val propertyGetters: Map<String, () -> Any?> = mapOf(
        PROP_TRAINER_NAME to { trainerName },
        PROP_MAX_SIZE to { maxSize },
        PROP_POKEMON_COUNT to { pokemons.size },
        PROP_POKEMON to { getPokemonCollection() }
    )

    override fun getProperty(
        name: String,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Any? {
        return propertyGetters[name]?.invoke()
            ?: throw errorHandler.propertyError(
                token,
                "Team has no property '$name'. Available: ${propertyGetters.keys.joinToString(", ")}"
            )
    }

    /**
     * All Team properties are read-only.
     * Pokemon management is done through the pokemon collection's methods.
     */
    override fun setProperty(
        name: String,
        value: Any?,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ) {
        throw errorHandler.propertyError(
            token,
            "Team properties are read-only. Use .pokemon-> to manage Pokemon."
        )
    }

    // ========== Method Dispatch ==========

    /**
     * Method dispatch map.
     * Maps method names to their implementations.
     */
    private val methodList: Map<String, (List<Any?>, EvaluatorErrorHandler, Token) -> Any?> = mapOf(
        METHOD_IS_FULL to { _, _, _ ->
            pokemons.size >= maxSize
        },
        METHOD_IS_EMPTY to { _, _, _ ->
            pokemons. isEmpty()
        },
        METHOD_HAS to { args, errorHandler, token ->
            val pokemonName = requireStringArg(args, errorHandler, token, METHOD_HAS)
            hasPokemon(pokemonName)
        }
    )

    override fun callMethod(
        name: String,
        args: List<Any? >,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Any?  {
        return methodList[name]?.invoke(args, errorHandler, token)
            ?: throw errorHandler.error(
                token,
                "Team has no method '$name'. Use .pokemon-> for Pokemon management.  Available methods: ${methodList.keys.joinToString(", ")}"
            )
    }

    // ========== Helper Methods ==========

    /**
     * Checks if the team contains a Pokemon with the given name.
     * Case-insensitive comparison.
     *
     * @param name Pokemon name to search for
     * @return true if Pokemon is on the team, false otherwise
     */
    private fun hasPokemon(name: String): Boolean {
        return pokemons.any { it. equals(name, ignoreCase = true) }
    }

    /**
     * Extracts and validates a required string argument from a method call.
     *
     * @param args List of method arguments
     * @param errorHandler Error handler for throwing errors
     * @param token Token for error location reporting
     * @param methodName Name of the method (for error messages)
     * @param argIndex Index of the argument to extract (default 0)
     * @return The validated string argument
     * @throws RuntimeError if argument is missing or not a string
     */
    private fun requireStringArg(
        args: List<Any?>,
        errorHandler: EvaluatorErrorHandler,
        token: Token,
        methodName: String,
        argIndex: Int = 0
    ): String {
        if (args.size <= argIndex) {
            throw errorHandler.error(
                token,
                "Method '$methodName()' requires at least ${argIndex + 1} argument(s), got ${args. size}"
            )
        }

        val arg = args[argIndex]
        if (arg !is String) {
            val typeName = arg?.javaClass?.simpleName ?: "null"
            throw errorHandler.typeError(
                token,
                "Method '$methodName()' argument ${argIndex + 1} must be a string, got $typeName"
            )
        }

        return arg
    }

    // ========== Object Methods ==========

    override fun toString(): String = "Team($trainerName, ${pokemons.size}/$maxSize Pokemon)"

    override fun getTypeName(): String = "Team"

    // ========== Constants ==========

    companion object {
        // Property names
        private const val PROP_TRAINER_NAME = "trainerName"
        private const val PROP_MAX_SIZE = "maxSize"
        private const val PROP_POKEMON_COUNT = "pokemonCount"
        private const val PROP_POKEMON = "pokemon"

        // Method names
        private const val METHOD_IS_FULL = "isFull"
        private const val METHOD_IS_EMPTY = "isEmpty"
        private const val METHOD_HAS = "has"
    }
}