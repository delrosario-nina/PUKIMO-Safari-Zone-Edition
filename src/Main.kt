import lexer.Scanner
import parser.*
import evaluator.Evaluator
import evaluator.RuntimeError
import java.util.Scanner as JavaScanner

/*handles the ff:
- buffers multi-line input until braces are balanced
- tracks brace count
- executes buffered code through lexer, parser, and evaluator
- maintains environment across executions*/

class ReplSession(
    private val scanner: Scanner = Scanner(),
    private val evaluator: Evaluator = Evaluator()
) {
    private val buffer = mutableListOf<String>()
    private var openBraces = 0

    //adds line to the buffer and updates brace count
    fun addLine(line: String) {
        buffer.add(line)
        openBraces += line.count { it == '{' } - line.count { it == '}' }
    }

    //checks if all braces in the buffer are balanced
    fun isBalanced(): Boolean {
        return openBraces == 0
    }

    //checks if there is code to execute
    fun hasContent(): Boolean {
        return buffer.isNotEmpty()
    }

    //executes buffered code and clears the buffer
    fun executeAndClear() {
        if (buffer.isEmpty()) {
            return
        }

        val code = buffer.joinToString("\n")
        buffer.clear()
        openBraces = 0

        try {
            val tokens = scanner.scanAll(code)
            val parser = Parser(tokens, replMode = true)  // Enable REPL mode - semicolons optional
            val ast = parser.parse()

            // Execute each statement
            for (stmt in ast.stmtList) {
                evaluator.evaluate(stmt)
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
        val parser = Parser(tokens, replMode = false)  // File mode - semicolons required
        val ast = parser.parse()
        val evaluator = Evaluator()

        // Execute all statements
        for (stmt in ast.stmtList) {
            evaluator.evaluate(stmt)
        }

    } catch (e: java.io.FileNotFoundException) {
        println("Error: File '$filename' not found.")
        System.exit(1)
    } catch (e: RuntimeError) {
        println(e.message)
        System.exit(1)
    } catch (e: ParserError) {
        println(e.message)
        System.exit(1)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    }
}

fun runRepl() {
    val session = ReplSession()
    val input = JavaScanner(System.`in`)

    println("Pukimo REPL - Safari Zone Edition")
    println("Enter code (type 'exit' to quit, empty line to execute when braces are balanced):")

    while (true) {
        // Show different prompt based on brace balance
        if (session.isBalanced()) {
            print("> ")
        } else {
            print("… ")
        }

        val line = input.nextLine() ?: break

        val trimmed = line.trim()

        // Check if user wants to exit
        if (trimmed.lowercase() == "exit") {
            break
        }

        // Handle empty line - execute if braces are balanced
        if (trimmed.isEmpty()) {
            if (session.isBalanced() && session.hasContent()) {
                session.executeAndClear()
            }
            continue
        }

        // Add line to buffer
        session.addLine(line)

        // Auto-execute when braces are balanced
        if (session.isBalanced() && session.hasContent()) {
            session.executeAndClear()
        }
    }
}

//$files = Get-ChildItem -Path src -Filter *.kt -Recurse | ForEach-Object { $_.FullName }
//kotlinc @files -include-runtime -d PukiMO.jar
//kotlin -cp PukiMO.jar MainKt simple.txt
fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        runFile(args[0])
    } else {
        // REPL mode - interactive prompt
        runRepl()
    }
}
