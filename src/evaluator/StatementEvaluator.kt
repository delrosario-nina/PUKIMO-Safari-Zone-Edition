package evaluator

import lexer.*
import parser.*

/**
 * Handles execution of statements (var declarations, print, if, blocks, explore).
 */
class StatementEvaluator(private val evaluator: Evaluator) {

    fun visitExprStmt(stmt: ExprStmt, isReplMode: Boolean): Any? {
        val result = stmt.expression. accept(evaluator)

        if (isReplMode && result != null) {
            println(evaluator.stringify(result))
        }

        return result
    }

    fun visitPrintStmt(stmt: PrintStmt): Any? {
        val value = stmt.expression.accept(evaluator)
        println(evaluator.stringify(value))
        return null
    }

    fun visitVarDeclStmt(stmt: VarDeclStmt): Any? {
        val value = stmt.expression.accept(evaluator)
        evaluator.getEnvironment().define(stmt.identifier, value)
        return null
    }

    fun visitBlock(block: Block): Any? {
        return executeBlock(
            block.stmtList,
            Environment(
                enclosing = evaluator. getEnvironment(),
                errorHandler = evaluator.getErrorHandler()
            )
        )
    }

    fun executeBlock(statements: List<Stmt>, blockEnvironment: Environment): Any? {
        val previous = evaluator.getEnvironment()
        try {
            evaluator.setEnvironment(blockEnvironment)
            var lastValue: Any? = null
            for (stmt in statements) {
                lastValue = stmt.accept(evaluator)
            }
            return lastValue
        } finally {
            evaluator.setEnvironment(previous)
        }
    }

    fun visitIfStmt(stmt: IfStmt): Any?  {
        val condition = stmt. expression.accept(evaluator)
        return if (evaluator.isTruthy(condition)) {
            stmt.thenBlock.accept(evaluator)
        } else {
            stmt.elseBlock?. accept(evaluator)
        }
    }

    fun visitExploreStmt(stmt: ExploreStmt): Any? {
        val safariZone = getSafariZoneObject(stmt.safariZoneVar)
        return executeExploreBlock(stmt, safariZone)
    }

    private fun getSafariZoneObject(token: Token): SafariZoneObject {
        val obj = evaluator.getEnvironment().get(token)
        if (obj !is SafariZoneObject) {
            throw evaluator. getErrorHandler().typeError(
                token,
                "Explore expects a SafariZone object for '${token.lexeme}'"
            )
        }
        return obj
    }

    // In StatementEvaluator.kt
    private fun executeExploreBlock(stmt: ExploreStmt, safariZone: SafariZoneObject): Any? {
        val exploreEnvironment = Environment(
            enclosing = evaluator.getEnvironment(),
            errorHandler = evaluator.getErrorHandler()
        )
        exploreEnvironment.define(stmt.safariZoneVar, safariZone)
        exploreEnvironment.define(Evaluator.ENCOUNTER_TOKEN, null)

        val previous = evaluator.getEnvironment()

        try {
            evaluator.setEnvironment(exploreEnvironment)

            while (safariZone.turns > 0) {
                safariZone.turns--

                val pokemonCollection = getPokemonCollection(safariZone)
                if (pokemonCollection. isEmpty()) {
                    println("No Pokemon left to encounter!")
                    break
                }

                val encounter = pokemonCollection.random(
                    evaluator.getErrorHandler(),
                    Evaluator.ENCOUNTER_TOKEN
                )
                evaluator.getEnvironment().assign(Evaluator.ENCOUNTER_TOKEN, encounter)

                try {
                    stmt. block.accept(evaluator)
                } catch (_: BreakException) {
                    break
                } catch (_: RunException) {
                    break
                }
            }

            if (safariZone.turns == 0) {
                println("Explore: Out of turns!")
            }
            return null
        } finally {
            evaluator.setEnvironment(previous)
        }
    }
    private fun getPokemonCollection(safariZone: SafariZoneObject): PokemonCollection {
        return safariZone.getProperty(
            "pokemon",
            evaluator.getErrorHandler(),
            Evaluator.POKEMON_TOKEN
        ) as PokemonCollection
    }


}