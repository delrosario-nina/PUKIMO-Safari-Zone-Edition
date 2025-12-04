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
        val startVal = stmt.start.accept(evaluator)

        // If no 'to' clause, iterate over collection/string
        if (stmt.end == null) {
            return when (startVal) {
                is String -> executeStringForLoop(stmt, startVal)
                is MutableList<*> -> executeArrayForLoop(stmt, startVal)
                else -> throw evaluator.getErrorHandler().typeError(
                    stmt.keyword,
                    "Can only iterate over strings or arrays without 'to', got ${startVal?. javaClass?.simpleName ?: "null"}"
                )
            }
        }

        // If 'to' clause exists, evaluate both start and end
        val endVal = stmt.end.accept(evaluator)

        // Check if still using string/array with 'to' (for backwards compatibility)
        if (startVal is String) {
            return executeStringForLoop(stmt, startVal)
        }
        if (startVal is MutableList<*>) {
            return executeArrayForLoop(stmt, startVal)
        }

        // Otherwise, treat as integer range
        val range = evaluateForRange(startVal, endVal, stmt.keyword)
        return executeForLoop(stmt, range)
    }

    private fun executeArrayForLoop(stmt: ForStmt, array: MutableList<*>): Any? {
        val loopEnvironment = Environment(enclosing = evaluator.getEnvironment())
        val previous = evaluator.getEnvironment()

        try {
            evaluator. setEnvironment(loopEnvironment)
            evaluator.getEnvironment().define(stmt.variable, array. firstOrNull())

            for (element in array) {
                evaluator.getEnvironment().assign(stmt.variable, element)

                try {
                    stmt. body.accept(evaluator)
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

    private fun evaluateForRange(startVal: Any?, endVal: Any?, token: Token): IntRange {
        if (startVal !is Int) {
            throw evaluator.getErrorHandler().typeError(token, "For loop start must be an integer or string")
        }
        if (endVal !is Int) {
            throw evaluator. getErrorHandler().typeError(token, "For loop end must be an integer")
        }

        return startVal.. endVal
    }

    private fun executeStringForLoop(stmt: ForStmt, str: String): Any? {
        val loopEnvironment = Environment(enclosing = evaluator.getEnvironment())
        val previous = evaluator.getEnvironment()

        try {
            evaluator. setEnvironment(loopEnvironment)
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

    private fun executeForLoop(stmt: ForStmt, range: IntRange): Any? {
        val loopEnvironment = Environment(enclosing = evaluator.getEnvironment())
        val previous = evaluator. getEnvironment()

        try {
            evaluator.setEnvironment(loopEnvironment)
            evaluator.getEnvironment().define(stmt.variable, range.first)

            for (i in range) {
                evaluator.getEnvironment().assign(stmt.variable, i)

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