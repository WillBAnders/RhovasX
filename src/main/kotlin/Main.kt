package dev.willbanders.rhovas.x

import dev.willbanders.rhovas.x.analysis.Declare
import dev.willbanders.rhovas.x.analysis.Define
import dev.willbanders.rhovas.x.interpreter.ENV
import dev.willbanders.rhovas.x.interpreter.Environment
import dev.willbanders.rhovas.x.interpreter.Scope
import dev.willbanders.rhovas.x.parser.Diagnostic
import dev.willbanders.rhovas.x.parser.ParseException
import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst
import dev.willbanders.rhovas.x.parser.rhovas.RhovasParser
import dev.willbanders.rhovas.x.stdlib.Stdlib
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

<<<<<<< HEAD
const val PATH = "src/main/resources"
const val PACKAGE = "Main.rho"
=======
val ENV = Environment()
const val PATH = "src/main/resources/maptest.rho"
>>>>>>> list

fun main() {
    Stdlib.init(ENV)
    val path = Paths.get(PATH)
    val sources = mutableMapOf<String, RhovasAst.Source>()
    Files.walk(path.resolve(PACKAGE))
        .filter { it.fileName.toString().endsWith(".rho") }
        .forEach {
            val name = path.relativize(it).joinToString(".") { it.toString().capitalize() }.replace(".rho", "")
            val input = it.toFile().readText()
            try {
                sources[name] = RhovasParser(input).parse()
            } catch (e: ParseException) {
                println(Diagnostic(name, input, e.error))
                exitProcess(0)
            }
        }
    val types = sources.mapValues { Pair(it.value, Declare(it.key).apply { visit(it.value) }.types) }
    types.forEach { Define(it.value.second).visit(it.value.first) }
    types.values.forEach { pair -> pair.first.impts.forEach {
        val name = it.name ?: it.path.joinToString(".")
        val type = ENV.reqType(it.path.joinToString("."))
        type.funcs.filter { it.key.first == "" }.forEach { pair.second[""]!!.scope.funcs[Pair(name, it.key.second)] = it.value }
        pair.second[""]!!.scope.vars[name] = Environment.Variable(name, Environment.Object(Environment.Type("$name.Static", Scope(null)).also { t ->
            type.scope.vars.forEach { v -> t.props[v.key] = Environment.Property(v.key,
                { v.value.value },
                { v.value.value = it[1] },
            ) }
            type.scope.funcs.forEach { f -> t.mthds[f.key] = Environment.Function(f.key.first, f.key.second) {
                f.value.invoke(it.subList(1, it.size))
            } }
        }, null))
    } }
    types.values.forEach { it.second.values.forEach { it.vars.values.forEach {
        it.value = (it.value.value as Environment.Function).invoke(listOf())
    } } }
    val main = types.values.map { it.second[""]!! }.filter { it.funcs[Pair("main", 0)] != null }
    when (main.size) {
        1 -> main[0].funcs[Pair("main", 0)]!!.invoke(listOf())
        0 -> throw Exception("A main/0 function is not defined.")
        else -> throw Exception("Found multiple main/0 functions in types " + main.map { it.name } + ".")
    }
}
