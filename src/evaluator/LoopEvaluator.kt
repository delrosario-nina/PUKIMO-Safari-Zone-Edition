package evaluator

import lexer.*
import parser.*

/**
 * Handles loop execution (for, while, break, continue).
 */
class LoopEvaluator(private val evaluator: Evaluator) {

    fun visitWhileStmt(stmt: WhileStmt): Any? {
        while (evaluator.isTruthy(stmt. condition.accept(evaluator))) {
            try {
                stmt.body.accept(evaluator)
            } catch (_: ContinueException) {
                continue
            } catch (_: BreakException) {
                break
            }
        }
        return null
    }

    fun visitForStmt(stmt: ForStmt): Any? {
        return if (stmt.isRangeLoop) {
            // Range loop: for i in 0 to 10
            executeRangeLoop(stmt)
        } else {
            // Collection loop: for item in collection
            executeCollectionLoop(stmt)
        }
    }

    private fun executeRangeLoop(stmt: ForStmt): Any? {
        val startVal = stmt.start.accept(evaluator)
        val endVal = stmt.end?.accept(evaluator)
            ?: throw evaluator.getErrorHandler().error(stmt.keyword, "Range loop requires 'to' clause")

        // Validate both are integers
        if (startVal !is Int) {
            throw evaluator.getErrorHandler().typeError(
                stmt.keyword,
                "Range loop start must be an integer, got ${startVal?.javaClass?.simpleName ?: "null"}"
            )
        }
        if (endVal !is Int) {
            throw evaluator. getErrorHandler().typeError(
                stmt.keyword,
                "Range loop end must be an integer, got ${endVal?.javaClass?.simpleName ?: "null"}"
            )
        }

        val range = startVal.. endVal
        return executeIntegerLoop(stmt, range)
    }

    private fun executeCollectionLoop(stmt: ForStmt): Any? {
        val collection = stmt.start.accept(evaluator)

        return when (collection) {
            is String -> executeStringLoop(stmt, collection)
            is MutableList<*> -> executeArrayLoop(stmt, collection)
            else -> throw evaluator.getErrorHandler().typeError(
                stmt.keyword,
                "Can only iterate over strings or arrays, got ${collection?.javaClass?. simpleName ?: "null"}"
            )
        }
    }

    private fun executeIntegerLoop(stmt: ForStmt, range: IntRange): Any? {
        val loopEnvironment = Environment(
            enclosing = evaluator. getEnvironment(),
            errorHandler = evaluator.getErrorHandler()
        )
        val previous = evaluator.getEnvironment()

        try {
            evaluator. setEnvironment(loopEnvironment)
            evaluator.getEnvironment().define(stmt.variable, range.first)

            for (i in range) {
                evaluator.getEnvironment().assign(stmt.variable, i)

                try {
                    stmt.body. accept(evaluator)
                } catch (_: ContinueException) {
                    continue
                } catch (_: BreakException) {
                    break
                }
            }
            return null
        } finally {
            evaluator.setEnvironment(previous)
        }
    }

    private fun executeArrayLoop(stmt: ForStmt, array: MutableList<*>): Any? {
        val loopEnvironment = Environment(
            enclosing = evaluator. getEnvironment(),
            errorHandler = evaluator.getErrorHandler()
        )
        val previous = evaluator.getEnvironment()

        try {
            evaluator.setEnvironment(loopEnvironment)
            // Initialize with first element or null
            evaluator.getEnvironment(). define(stmt.variable, array. firstOrNull())

            for (element in array) {
                evaluator.getEnvironment().assign(stmt.variable, element)

                try {
                    stmt.body. accept(evaluator)
                } catch (_: ContinueException) {
                    continue
                } catch (_: BreakException) {
                    break
                }
            }
            return null
        } finally {
            evaluator.setEnvironment(previous)
        }
    }

    private fun executeStringLoop(stmt: ForStmt, str: String): Any? {
        val loopEnvironment = Environment(
            enclosing = evaluator.getEnvironment(),
            errorHandler = evaluator.getErrorHandler()
        )
        val previous = evaluator. getEnvironment()

        try {
            evaluator.setEnvironment(loopEnvironment)
            evaluator.getEnvironment().define(stmt.variable, "")

            for (char in str) {
                evaluator.getEnvironment().assign(stmt.variable, char. toString())

                try {
                    stmt.body.accept(evaluator)
                } catch (_: ContinueException) {
                    continue
                } catch (_: BreakException) {
                    break
                }
            }
            return null
        } finally {
            evaluator.setEnvironment(previous)
        }
    }

    fun visitBreakStmt(stmt: BreakStmt): Any? {
        throw BreakException()
    }

    fun visitContinueStmt(stmt: ContinueStmt): Any? {
        throw ContinueException()
    }
}