package dev.willbanders.rhovas.x

import dev.willbanders.rhovas.x.interpreter.Environment
import dev.willbanders.rhovas.x.interpreter.Interpreter
import dev.willbanders.rhovas.x.parser.Diagnostic
import dev.willbanders.rhovas.x.parser.ParseException
import dev.willbanders.rhovas.x.parser.rhovas.RhovasParser
import dev.willbanders.rhovas.x.stdlib.Stdlib
import java.io.File

val ENV = Environment()
const val PATH = "src/main/resources/Main.rho"

fun main() {
    Stdlib.init(ENV)
    val input = File(PATH).readText()
    try {
        Interpreter(ENV).visit(RhovasParser(input).parse())
    } catch (e: ParseException) {
        println(Diagnostic(PATH.substringAfterLast('/'), input, e.error))
    }
}
