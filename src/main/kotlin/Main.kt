package dev.willbanders.rhovas.x

import dev.willbanders.rhovas.x.analysis.Declare
import dev.willbanders.rhovas.x.analysis.Define
import dev.willbanders.rhovas.x.interpreter.ENV
import dev.willbanders.rhovas.x.interpreter.Environment
import dev.willbanders.rhovas.x.interpreter.Interpreter
import dev.willbanders.rhovas.x.interpreter.Scope
import dev.willbanders.rhovas.x.parser.Diagnostic
import dev.willbanders.rhovas.x.parser.ParseException
import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst
import dev.willbanders.rhovas.x.parser.rhovas.RhovasParser
import dev.willbanders.rhovas.x.stdlib.Stdlib
import java.lang.AssertionError
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.system.exitProcess

const val PATH = "src/main/resources/examples"

fun main() {
    //To whoever visits here, it is late and I am tired. I am so sorry.
    Stdlib.init(ENV)
    val REPL = ENV.defType("REPL") { type ->
        type.defFunc("help", 0) { args ->
            println("func help(): Opens this help menu.")
            println("func run(example: String): Runs the example source file of the given name.")
            println("func examples(): Displays a list of all examples available.")
            println("func about(): Displays information about Rhovas!")
            println()
            println("And of course, any other Rhovas code.")
            println("(the standard library is a little limited though xD)")
            ENV.init("Null", null)
        }
        type.defFunc("run", 1) { args ->
            val name = args[0].value as String
            if (Files.exists(Paths.get(PATH).resolve(name + ".rho"))) {
                compile(name + ".rho")
            } else if (Files.exists(Paths.get(PATH).resolve(name))) {
                compile(name)
            } else {
                throw Exception("Undefined example $name. Run `examples();` for a list.");
            }
            ENV.init("Null", null)
        }
        type.defFunc("examples", 0) { args ->
            Files.list(Paths.get(PATH)).filter { Files.isRegularFile(it) }.forEach { println(it.fileName.toString().replace(".rho", "")) }
            println()
            println("Run an example with `run(\"name\");`.")
            ENV.init("Null", null)
        }
        type.defFunc("about", 0) { args ->
            println("Rhovas is a programming language for API design and enforcement.")
            println("Using Rhovas, developers can better express the contracts and")
            println("intention of their code to help create correct, maintainable software.")
            println()
            println(" GitHub: https://github.com/WillBAnders/RhovasX")
            println(" Repl.it: https://repl.it/@WillBAnders/RhovasX")
            println(" Discord: https://discord.gg/gm96xd8")
            println()
            println("Remember that RhovasX is experimental - feel free to try things out and")
            println("provide feedback, but there's definitely a fair share of issues in here!")
            ENV.init("Null", null)
        }
    }.let { ENV.reqType("REPL") }
    ENV.scope.funcs.putAll(REPL.funcs)
    println("Rhovas - A Language for API Design and Enforcement.")
    println("REPL is running. To see available commands, enter `help();`.")
    while (true) {
        val builder = StringBuilder()
        var indent = 0
        do {
            val input = readLine()!!
            indent += input.count { it == '{' } - input.count {it == '}' }
            builder.append(input).append("\n")
        } while (indent != 0 || input.isEmpty())
        val input = builder.toString()
        val ast = try {
            val matcher = Pattern.compile("^([A-Za-z]+)").matcher(input)
            if (matcher.find() && matcher.group(1) in listOf("import", "public", "package", "protected", "private", "virtual", "abstract", "override", "class", "interface", "struct", "ctor", "func")) {
                RhovasParser(input).parse()
            } else if (input.startsWith("{")) {
                RhovasAst.Stmt.Expression(RhovasParser(input).parseExpr())
            } else {
                RhovasParser(input).parseStatement()
            }
        } catch (e: ParseException) {
            println(Diagnostic("REPL", input, e.error))
            continue
        }
        try {
            when (ast) {
                is RhovasAst.Source -> {
                    val types = mapOf(Pair("", Declare("REPL").apply {
                        type = REPL
                        types[""] = type
                        ast.mbrs.filterIsInstance<RhovasAst.Mbr.Cmpt>().forEach { visit(it) }
                    }.types.let { Pair(ast, it) }))
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
                    println(">> " + types[""]!!.second.filter { !it.key.isEmpty() }.map { it.key }.joinToString(", "))
                }
                is RhovasAst.Stmt -> {
                    Interpreter.eval(REPL.scope) {
                        when (ast) {
                            is RhovasAst.Stmt.Expression -> (visit(ast.expr) as Environment.Object).let { it.reqMthd("toString", 0).invoke(listOf(it)).value as String }.let { if (ast.expr is RhovasAst.Expr.Function && ast.expr.rec == null && ast.expr.name in listOf("print", "write", "help", "run", "examples", "about")) null else println(">> " + it) }
                            is RhovasAst.Stmt.Declaration -> println(">> " + ast.name + " = " + visit(ast).let { REPL.scope.reqVar(ast.name).value.let { it.reqMthd("toString", 0).invoke(listOf(it)).value as String } })
                            is RhovasAst.Stmt.Assignment -> {
                                if (ast.rec is RhovasAst.Expr.Access) {
                                    if (ast.rec.rec == null) {
                                        val variable = Interpreter.scope!!.reqVar(ast.rec.name)
                                        variable.value = visit(ast.value) as Environment.Object
                                        println(">> " + ast.rec.name + " = " + variable.value.let { it.reqMthd("toString", 0).invoke(listOf(it)).value as String })
                                    } else {
                                        val rec = visit(ast.rec.rec) as Environment.Object
                                        val prop = rec.reqProp(ast.rec.name)
                                        prop.set(listOf(rec, visit(ast.value) as Environment.Object))
                                        println(">> {" + rec.type.name + "}." + ast.rec.name + " = " + prop.get(listOf(rec)).let { it.reqMthd("toString", 0).invoke(listOf(it)).value as String })
                                    }
                                } else if (ast.rec is RhovasAst.Expr.Index) {
                                    val rec = visit(ast.rec.rec) as Environment.Object
                                    val args = ast.rec.args + listOf(ast.value)
                                    val method = rec.reqMthd("[]=", args.size)
                                    val evalArgs = args.map { visit(it) as Environment.Object }
                                    method.invoke(listOf(rec) + evalArgs)
                                    println(">> {" + rec.type.name + "}[" + evalArgs.subList(0, evalArgs.size - 1).map { when (it.type.name) {
                                            "String" -> "\"" + it.value as String + "\""
                                            in listOf("Atom", "Boolean", "Character", "Decimal", "Integer", "Null", "String") -> it.reqMthd("toString", 0).invoke(listOf(it)).value as String
                                            else -> "{" + it.type.name + "}"
                                    } } + "] = " + evalArgs.last().reqMthd("toString", 0).invoke(listOf(evalArgs.last())).value as String)
                                } else {
                                    throw Exception("Assignment receiver must be an access expression or index expression.")
                                }
                            }
                            is RhovasAst.Stmt.Assert -> println(">> " + (visit(ast.cond) as Environment.Object).value as Boolean)
                            is RhovasAst.Stmt.Require -> println(">> " + (visit(ast.cond) as Environment.Object).value as Boolean)
                            is RhovasAst.Stmt.Ensure -> println(">> " + (visit(ast.cond) as Environment.Object).value as Boolean)
                            else -> visit(ast)
                        }
                    }
                }
                else -> throw AssertionError()
            }
        } catch (e: Exception) {
            println("Error: " + e.message)
        }
    }
}

fun compile(pkg: String) {
    Stdlib.init(ENV)
    val path = Paths.get(PATH)
    val sources = mutableMapOf<String, RhovasAst.Source>()
    Files.walk(path.resolve(pkg))
        .filter { it.fileName.toString().endsWith(".rho") }
        .forEach {
            val name = path.relativize(it).joinToString(".") { it.toString().capitalize() }.replace(".rho", "")
            val input = it.toFile().readText()
            println("[$name.rho]".padEnd(40, '='))
            print(input)
            println("[$name.rho]".padEnd(40, '='))
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
