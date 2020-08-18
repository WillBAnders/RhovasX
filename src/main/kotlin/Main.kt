package dev.willbanders.rhovas.x

import dev.willbanders.rhovas.x.interpreter.Environment
import dev.willbanders.rhovas.x.interpreter.rhovas.RhovasInterpreter
import dev.willbanders.rhovas.x.parser.Diagnostic
import dev.willbanders.rhovas.x.parser.ParseException
import dev.willbanders.rhovas.x.parser.rhovas.RhovasParser
import java.io.File

const val PATH = "src/main/resources/Main.rho"

fun main() {
    val env = Environment()
    env.scope.defineFunction("print", 1) { println(it[0]) }
    val input = File(PATH).readText()
    try {
        RhovasInterpreter(env).eval(RhovasParser(input).parse())
    } catch (e: ParseException) {
        println(Diagnostic(PATH.substringAfterLast('/'), input, e.error))
    }
}
