package evaluator

import lexer.Token
import kotlin.random.Random

class PokemonCollection(
    private val pokemonList: MutableList<String>,
    private val ownerType: String = "Collection",
    private val rng: Random = Random. Default
) : SafariZoneObjectInterface {

    // Configurable catch rates (0-100 percentage)
    private var defaultCatchRate: Int = 50  // 50% default
    private val speciesCatchRates: MutableMap<String, Int> = mutableMapOf()

    fun isEmpty(): Boolean = pokemonList.isEmpty()
    fun remove(item: String): Boolean = pokemonList.remove(item)

    fun random(errorHandler: EvaluatorErrorHandler, token: Token): String {
        if (pokemonList.isEmpty()) {
            throw errorHandler.error(token, "$ownerType has no Pokemon.")
        }
        return pokemonList.random(rng)
    }

    // Helper to get effective catch rate (species override > explicit > default)
    private fun effectiveCatchRate(encounter: String, explicitChance: Int? ): Int {
        val speciesRate = speciesCatchRates[encounter]
        return (speciesRate ?: explicitChance ?: defaultCatchRate).coerceIn(0, 100)
    }

    private fun attemptCatch(
        encounter: String,
        zone: SafariZoneObject?  = null,
        explicitChance: Int? = null,
        errorHandler: EvaluatorErrorHandler,
        token: Token
    ): Boolean {
        if (zone != null) {
            if (zone.balls <= 0)
                throw errorHandler.error(token, "No Safari Balls remaining!")
            zone.balls--
        }

        val idx = pokemonList.indexOf(encounter)
        if (idx == -1)
            throw errorHandler.error(token, "$encounter is not in the ${ownerType. lowercase()}.")

        val chancePercent = effectiveCatchRate(encounter, explicitChance)
        // Random int from 0-99, catch if < chancePercent
        if (rng.nextInt(100) < chancePercent) {
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
            pokemonList.add(item); null
        },
        "addAll" to { args, errorHandler, token ->
            if (args.isEmpty())
                throw errorHandler.error(token, "addAll requires at least one argument.")

            args.forEach { arg ->
                when (arg) {
                    is String -> pokemonList.add(arg)
                    is List<*> -> {
                        arg.forEach { item ->
                            if (item is String) {
                                pokemonList.add(item)
                            } else {
                                throw errorHandler.typeError(token, "All array elements must be strings, got ${item?. javaClass?.simpleName}")
                            }
                        }
                    }
                    else -> throw errorHandler.typeError(token, "addAll expects string or array, got ${arg?. javaClass?.simpleName}")
                }
            }
            null
        },

        "removeAll" to { args, errorHandler, token ->
            if (args.isEmpty())
                throw errorHandler.error(token, "removeAll requires at least one argument.")

            var count = 0
            args.forEach { arg ->
                when (arg) {
                    is String -> if (pokemonList.remove(arg)) count++
                    is List<*> -> {
                        arg.forEach { item ->
                            if (item is String && pokemonList.remove(item)) count++
                        }
                    }
                    else -> throw errorHandler.typeError(token, "removeAll expects string or array")
                }
            }
            count
        },
        "remove" to { args, errorHandler, token ->
            val item = requireStringArg(args, errorHandler, token, "remove")
            remove(item)
        },
        "list" to { _, _, _ -> pokemonList.toList() },
        "find" to { args, errorHandler, token ->
            val searchName = requireStringArg(args, errorHandler, token, "find")
            pokemonList.find { it.equals(searchName, ignoreCase = true) } ?: "null"
        },
        "random" to { _, errorHandler, token -> random(errorHandler, token) },
        "count" to { _, _, _ -> pokemonList.size },
        "clear" to { _, _, _ -> pokemonList.clear(); null },
        "isEmpty" to { _, _, _ -> isEmpty() },

        "attemptCatch" to { args, errorHandler, token ->
            if (args.isEmpty())
                throw errorHandler.error(token, "attemptCatch requires encounter name.")
            val encounter = args[0] as?  String
                ?: throw errorHandler.typeError(token, "First argument must be a string.")

            var chanceArg: Int? = null
            var zoneObj: SafariZoneObject? = null

            if (args.size >= 2) {
                when (val a1 = args[1]) {
                    is Int -> chanceArg = a1
                    is SafariZoneObject -> zoneObj = a1
                }
            }
            if (args.size >= 3) {
                val a2 = args[2]
                if (a2 is SafariZoneObject) zoneObj = a2
            }

            attemptCatch(encounter, zoneObj, chanceArg, errorHandler, token)
        },

        // setCatchRate(percent) - set global default (0-100)
        "setCatchRate" to { args, errorHandler, token ->
            if (args.isEmpty())
                throw errorHandler.error(token, "setCatchRate requires an integer (0 to 100).")
            val rate = args[0] as? Int
                ?: throw errorHandler.typeError(token, "setCatchRate requires an integer.")
            if (rate !in 0..100)
            throw errorHandler.error(token, "Catch rate must be between 0 and 100.")
            defaultCatchRate = rate
            null
        },

        // setSpeciesCatchRate(name, percent) - set per-species rate (0-100)
        "setSpeciesCatchRate" to { args, errorHandler, token ->
            if (args.size < 2)
                throw errorHandler.error(token, "setSpeciesCatchRate requires (name, rate).")
            val name = args[0] as? String
                ?: throw errorHandler.typeError(token, "First argument must be a string.")
            val rate = args[1] as? Int
                ?: throw errorHandler.typeError(token, "Second argument must be an integer.")
            if (rate !in 0..100)
                throw errorHandler.error(token, "Catch rate must be between 0 and 100.")
            speciesCatchRates[name] = rate
            null
        }
    )

    override fun callMethod(name: String, args: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token): Any? {
        val method = methodList[name]
            ?: throw errorHandler.error(token, "PokemonCollection has no method '$name'.  Available: ${methodList.keys.joinToString(", ")}")
        return method(args, errorHandler, token)
    }

    private fun requireStringArg(args: List<Any?>, errorHandler: EvaluatorErrorHandler, token: Token, methodName: String): String {
        if (args.isEmpty())
            throw errorHandler.error(token, "Method '$methodName' requires a string argument.")
        return args[0] as?  String
            ?: throw errorHandler.typeError(token, "Method '$methodName' requires a string argument.")
    }

    override fun toString(): String = pokemonList.joinToString(", ")
    override fun getTypeName(): String = "PokemonCollection"

    override fun getProperty(name: String, errorHandler: EvaluatorErrorHandler, token: Token): Any?  {
        throw errorHandler.error(token, "PokemonCollection has no properties. Use methods with '->' operator.")
    }

    override fun setProperty(name: String, value: Any?, errorHandler: EvaluatorErrorHandler, token: Token) {
        throw errorHandler.error(token, "Cannot set properties on PokemonCollection.")
    }
}