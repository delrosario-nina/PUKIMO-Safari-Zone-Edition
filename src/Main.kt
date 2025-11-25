import lexer.Scanner
import parser.*
import evaluator.*
import kotlin.system.exitProcess

import java.util.Scanner as JavaScanner

/**
 * Manages multi-line input buffering for the REPL.
 *
 * Accumulates lines until braces are balanced, then returns
 * the complete code for execution.
 */
class InputBuffer {
    private val lines = mutableListOf<String>()
    private var openBraces = 0

    fun addLine(line: String) {
        lines.add(line)
        openBraces += line.count { it == '{' } - line.count { it == '}' }
    }

    fun isBalanced(): Boolean = openBraces == 0

    fun hasContent(): Boolean = lines.isNotEmpty()

    /**
     * Returns all buffered lines as a single code string and clears the buffer.
     * This is used for the evaluation step in the REPL.
     */
    fun consumeAndClear(): String {
        val code = lines.joinToString("\n")
        lines.clear()
        openBraces = 0
        return code
    }
}

/**
 * Maintains persistent execution state across REPL commands.
 *
 * Keeps the same Evaluator instance so that variables
 * defined in one command remain available in subsequent commands.
 */
class ReplExecutor {
    private val evaluator = Evaluator()

    fun execute(code: String) {
        try {
            val scanner = Scanner()
            val tokens = scanner.scanAll(code)
            val parser = Parser(tokens)
            val ast = parser.parse()

            for (stmt in ast.stmtList) {
                evaluator.evaluate(stmt, isReplMode = true)
            }

        } catch (e: RuntimeError) {
            println(e.message)
            evaluator.errorHandler.clearErrors()
        } catch (e: ParserError) {
            println(e.message)
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }
}

fun runFile(filename: String) {
    try {
        val code = java.io.File(filename).readText()
        val scanner = Scanner()
        val tokens = scanner.scanAll(code)
        val parser = Parser(tokens)
        val ast = parser.parse()
        val evaluator = Evaluator()

        for (stmt in ast.stmtList) {
            evaluator.evaluate(stmt, isReplMode = false)
        }

    } catch (e: java.io.FileNotFoundException) {
        println("Error: File '$filename' not found.")
        e.printStackTrace()
        exitProcess(1)
    } catch (e: RuntimeError) {
        println(e.message)
        e.printStackTrace()
        exitProcess(1)
    } catch (e: ParserError) {
        println(e.message)
        e.printStackTrace()
        exitProcess(1)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

fun runRepl() {
    val buffer = InputBuffer()
    val executor = ReplExecutor()
    val input = JavaScanner(System.`in`)

    println("Pukimo REPL - Safari Zone Edition")
    println("Enter code (type 'exit' to quit):")

    while (true) {
        val prompt = if (buffer.isBalanced()) "> " else "… "
        print(prompt)

        val line = input.nextLine() ?: break

        if (line.trim().lowercase() == "exit") break
        if (line.trim().isEmpty()) {
            if (buffer.isBalanced() && buffer.hasContent()) {
                executor.execute(buffer.consumeAndClear())
            }
            continue
        }

        buffer.addLine(line)

        if (buffer.isBalanced() && buffer.hasContent()) {
            executor.execute(buffer.consumeAndClear())
        }
    }
}

//kotlinc @sources.txt -include-runtime -d PukiMo.jar
//java -jar PukiMo.jar team.txt
fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        runFile(args[0])
    } else {
        runRepl()
    }
}
