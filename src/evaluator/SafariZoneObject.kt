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
) : SafariZoneObjectInterface {

    // Property getters
    private val propertyGetters = mapOf(
        "initialBalls" to { initialBalls },
        "initialTurns" to { initialTurns },
        "balls" to { balls },
        "turns" to { turns },
        "pokemon" to { PokemonCollection(pokemon, "SafariZone") }
    )

    override fun getProperty(
        name: String,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Any? {
        return propertyGetters[name]?.invoke()
            ?: throw errorHandler.propertyError(token, "SafariZone has no property '$name'")
    }

    // Property setters
    private val propertySetters = mapOf<String, (Any?, EvaluatorErrorHandler, Token) -> Unit>(
        "balls" to { value, errorHandler, token ->
            if (value !is Int) throw errorHandler.typeError(token, "balls must be an integer.")
            balls = value
        },
        "turns" to { value, errorHandler, token ->
            if (value !is Int) throw errorHandler.typeError(token, "turns must be an integer.")
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
            ?: throw errorHandler.propertyError(token, "Cannot modify property '$name'.")
    }

    // Method dispatch map
    private val methodList = mapOf(
        "useBall" to { _: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token ->
            if (balls <= 0) throw errorHandler.error(token, "No Safari Balls remaining!")
            balls--
            null
        },
        "useTurn" to { _: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token ->
            if (turns <= 0) throw errorHandler.error(token, "No turns remaining!")
            turns--
            null
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

    override fun callMethod(
        name: String,
        args: List<Any?>,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Any? {
        val method = methodList[name]
        if (method != null) {
            return method(args, errorHandler, token)
        } else {
            throw errorHandler.error(
                token,
                "SafariZone has no method '$name'. Use .pokemon-> for Pokemon management. Available methods: ${methodList.keys.joinToString(", ")}"
            )
        }
    }

    override fun toString(): String = "SafariZone(balls=$balls, turns=$turns)"
    override fun getTypeName(): String = "SafariZone"
}
