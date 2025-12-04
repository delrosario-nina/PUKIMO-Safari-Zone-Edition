package evaluator

import lexer.Token

/**
 * SafariZoneObject - Represents a Safari Zone area in the Pokemon game.
 *
 * A Safari Zone is a special area where players can catch Pokemon using a limited
 * number of Safari Balls and turns. Players must manage their resources carefully.
 *
 * Properties:
 * - initialBalls: Initial number of Safari Balls available (immutable)
 * - initialTurns: Initial number of turns allowed (immutable)
 * - balls: Current remaining Safari Balls (mutable, non-negative)
 * - turns: Current remaining turns (mutable, non-negative)
 * - pokemon: Pokemon collection available in this zone
 *
 * Methods:
 * - useBall(): Consumes one Safari Ball (throws error if none remaining)
 * - useTurn(): Consumes one turn (throws error if none remaining)
 * - reset(): Resets balls and turns to initial values
 * - isGameOver(): Returns true if no balls or turns remaining
 *
 * Example usage:
 * ```
 * var zone = SafariZone(balls=30, turns=500);
 * zone.pokemon->add("Pikachu");
 * zone.useBall();
 * print(zone.balls);  // 29
 * ```
 *
 * @property initialBalls Initial/maximum number of Safari Balls (must be non-negative)
 * @property initialTurns Initial/maximum number of turns (must be non-negative)
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
) : SafariZoneObjectInterface {

    // ========== Lazy-Initialized Pokemon Collection ==========

    /**
     * Cached Pokemon collection wrapper.
     * Created once and reused for consistent object identity.
     */
    private var pokemonCollection: PokemonCollection?  = null

    private fun getPokemonCollection(): PokemonCollection {
        return pokemonCollection ?: PokemonCollection(pokemon, "SafariZone"). also {
            pokemonCollection = it
        }
    }

    // ========== Property Access ==========

    /**
     * Property getters map.
     * Provides read access to SafariZone properties.
     */
    private val propertyGetters = mapOf<String, () -> Any?>(
        PROP_INITIAL_BALLS to { initialBalls },
        PROP_INITIAL_TURNS to { initialTurns },
        PROP_BALLS to { balls },
        PROP_TURNS to { turns },
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
                "SafariZone has no property '$name'.  Available: ${propertyGetters.keys. joinToString(", ")}"
            )
    }

    /**
     * Property setters map.
     * Provides write access to mutable SafariZone properties with validation.
     */
    private val propertySetters = mapOf<String, (Any?, EvaluatorErrorHandler, Token) -> Unit>(
        PROP_BALLS to { value, errorHandler, token ->
            validateIntProperty(value, PROP_BALLS, errorHandler, token)
            validateNonNegative(value as Int, PROP_BALLS, errorHandler, token)
            balls = value
        },
        PROP_TURNS to { value, errorHandler, token ->
            validateIntProperty(value, PROP_TURNS, errorHandler, token)
            validateNonNegative(value as Int, PROP_TURNS, errorHandler, token)
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
            ?: throw errorHandler.propertyError(
                token,
                "Cannot modify property '$name'.  Mutable properties: ${propertySetters.keys. joinToString(", ")}"
            )
    }

    // ========== Method Dispatch ==========

    /**
     * Method dispatch map.
     * Maps method names to their implementations.
     */
    private val methodList: Map<String, (List<Any?>, EvaluatorErrorHandler, Token) -> Any?> by lazy {
        mapOf(
            "useBall" to { _: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token ->
                decrementResource(balls, "Safari Balls", { balls-- }, errorHandler, token)
            },
            "useTurn" to { _: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token ->
                decrementResource(turns, "turns", { turns-- }, errorHandler, token)
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
    }
    override fun callMethod(
        name: String,
        args: List<Any?>,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Any?  {
        val method = methodList[name]
            ?: throw errorHandler.error(
                token,
                "SafariZone has no method '$name'. Use .pokemon-> for Pokemon management. Available methods: ${methodList. keys.joinToString(", ")}"
            )

        return method.invoke(args, errorHandler, token)
    }

    // ========== Helper Methods ==========

    /**
     * Validates that a property value is an Int.
     */
    private fun validateIntProperty(
        value: Any?,
        propertyName: String,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ) {
        if (value !is Int) {
            throw errorHandler.typeError(token, "Property '$propertyName' must be an integer, got ${value?. javaClass?.simpleName ?: "null"}")
        }
    }

    /**
     * Validates that an integer value is non-negative.
     */
    private fun validateNonNegative(
        value: Int,
        propertyName: String,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ) {
        if (value < 0) {
            throw errorHandler.argumentError(token, "Property '$propertyName' cannot be negative (got $value)")
        }
    }

    /**
     * Decrements a resource if available, throws error if depleted.
     *
     * @param currentValue Current resource count
     * @param resourceName Human-readable resource name for error messages
     * @param decrement Lambda to perform the decrement operation
     * @param errorHandler Error handler for throwing errors
     * @param token Token for error location reporting
     */
    private fun decrementResource(
        currentValue: Int,
        resourceName: String,
        decrement: () -> Unit,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Any? {
        if (currentValue <= 0) {
            throw errorHandler.error(token, "No $resourceName remaining!  (Current: $currentValue)")
        }
        decrement()
        return null
    }

    // ========== Object Methods ==========

    override fun toString(): String = "SafariZone(balls=$balls/$initialBalls, turns=$turns/$initialTurns, pokemon=${pokemon.size})"

    override fun getTypeName(): String = "SafariZone"

    // ========== Constants ==========

    companion object {
        // Property names
        private const val PROP_INITIAL_BALLS = "initialBalls"
        private const val PROP_INITIAL_TURNS = "initialTurns"
        private const val PROP_BALLS = "balls"
        private const val PROP_TURNS = "turns"
        private const val PROP_POKEMON = "pokemon"

        // Method names
        private const val METHOD_USE_BALL = "useBall"
        private const val METHOD_USE_TURN = "useTurn"
        private const val METHOD_RESET = "reset"
        private const val METHOD_IS_GAME_OVER = "isGameOver"
    }
}