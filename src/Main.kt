import lexer.Scanner
import parser.*
import evaluator.Evaluator
import evaluator.RuntimeError
import java.util.Scanner as JavaScanner

fun main() {
    val scanner = Scanner()
    val input = JavaScanner(System.`in`)
    val buffer = mutableListOf<String>()
    var openBraces = 0
    val evaluator = Evaluator()

    println("Pukimo REPL - Safari Zone Edition")
    println("Enter code (type 'exit' to quit):")

    while (true) {
        print(if (openBraces > 0) "… " else "> ")

        val line = input.nextLine() ?: break
        val trimmed = line.trim()
        if (trimmed.lowercase() == "exit") break

        if (trimmed.isEmpty() && openBraces == 0) continue

        buffer.add(line)
        openBraces += line.count { it == '{' } - line.count { it == '}' }

        if (openBraces <= 0 && buffer.isNotEmpty()) {
            val code = buffer.joinToString("\n")

            try {
                val tokens = scanner.scanAll(code)
                val parser = Parser(tokens)
                val ast = parser.parse()

                val result = evaluator.evaluate(ast)
                if (result != null) {
                    println(evaluator.stringify(result))
                }

            } catch (e: RuntimeError) {
                println(e.message)

            } catch (e: ParserError) {
                println(e.message)

            } catch (e: Exception) {
                println("Error: ${e.message}")
            }

            buffer.clear()
            openBraces = 0
        }
    }

    println("\nGoodbye, Trainer!")
}
