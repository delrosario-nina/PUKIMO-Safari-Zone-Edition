package evaluator

import lexer.Token
import kotlin.random.Random

/**
 * PokemonCollection - Represents a collection of Pokemon with generic collection methods.
 *
 * This is a collection object that provides standard collection operations.
 * Used by both SafariZone and Team objects.
 *
 * Methods:
 * - add(item) → adds an item to the collection
 * - remove(item) → removes an item from the collection
 * - list() → returns all items as a string
 * - find(item) → finds an item by name (case-insensitive)
 * - random() → returns a random item from the collection
 * - count() → returns the number of items in the collection
 * - clear() → removes all items from the collection
 */
class PokemonCollection(private val pokemonList: MutableList<String>, private val ownerType: String = "Collection") : SafariZoneObjectInterface {

    // Direct Kotlin methods for internal use
    fun isEmpty(): Boolean = pokemonList.isEmpty()

    fun remove(item: String): Boolean = pokemonList.remove(item)

    fun random(errorHandler: EvaluatorErrorHandler, token: Token): String {
        if (pokemonList.isEmpty()) {
            throw errorHandler.error(token, "$ownerType has no Pokemon.")
        }
        return pokemonList.random()
    }

    private fun attemptCatch(encounter: String, zone: SafariZoneObject? = null, errorHandler: EvaluatorErrorHandler, token: Token): Boolean {
        if (zone != null) {
            if (zone.balls <= 0)
                throw errorHandler.error(token, "No Safari Balls remaining!")
            zone.balls--
        }
        // Check encounter exists
        val idx = pokemonList.indexOf(encounter)
        if (idx == -1)
            throw errorHandler.error(token, "$encounter is not in the ${ownerType.lowercase()}.")

        // 50/50 catch chance:
        if (Random.nextBoolean()) {
            pokemonList.removeAt(idx)
            println("Caught a $encounter!")
            return true
        } else {
            println("$encounter escaped!")
            return false
        }
    }

    private val methodList: Map<String, (List<Any?>, EvaluatorErrorHandler, Token) -> Any?> = mapOf(
        "add" to { args, errorHandler, token ->
            val item = requireStringArg(args, errorHandler, token, "add")
            pokemonList.add(item)
            null
        },
        "remove" to { args, errorHandler, token ->
            val item = requireStringArg(args, errorHandler, token, "remove")
            remove(item)
        },
        "list" to { _, _, _ ->
            pokemonList.joinToString(", ")
        },
        "find" to { args, errorHandler, token ->
            val searchName = requireStringArg(args, errorHandler, token, "find")
            val found = pokemonList.find { it.equals(searchName, ignoreCase = true) }
            found ?: "null"
        },
        "random" to { _, errorHandler, token ->
            random(errorHandler, token)
        },
        "count" to { _, _, _ ->
            pokemonList.size
        },
        "clear" to { _, _, _ ->
            pokemonList.clear()
            null
        },
        "isEmpty" to { _, _, _ ->
            isEmpty()
        },
        // attemptCatch(encounter, zone)
        "attemptCatch" to { args, errorHandler, token ->
            if (args.isEmpty())
                throw errorHandler.error(token, "attemptCatch requires at least the encounter name as argument.")
            val encounter = args[0]
            if (encounter !is String)
                throw errorHandler.typeError(token, "First argument to attemptCatch must be a string.")
            // Optionally pass zone as second argument
            val zoneObj = args.getOrNull(1) as? SafariZoneObject
            attemptCatch(encounter, zoneObj, errorHandler, token)
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
                "PokemonCollection has no method '$name'. Available methods: ${methodList.keys.joinToString(", ")}"
            )
        }
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

    override fun toString(): String = pokemonList.joinToString(", ")
    override fun getTypeName(): String = "PokemonCollection"

    override fun getProperty(name: String, errorHandler: EvaluatorErrorHandler, token: Token): Any? {
        throw errorHandler.error(
            token,
            "PokemonCollection has no properties. Use methods with '->' operator."
        )
    }

    override fun setProperty(name: String, value: Any?, errorHandler: EvaluatorErrorHandler, token: Token) {
        throw errorHandler.error(
            token,
            "Cannot set properties on PokemonCollection."
        )
    }
}
