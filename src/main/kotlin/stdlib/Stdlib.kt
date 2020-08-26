package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment
import dev.willbanders.rhovas.x.parser.Diagnostic
import dev.willbanders.rhovas.x.parser.ParseException
import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst
import dev.willbanders.rhovas.x.parser.rhovas.RhovasParser
import java.nio.file.Files
import java.nio.file.Paths

object Stdlib {

    fun init(env: Environment) {
        val root = Paths.get("src/main/resources/stdlib")
        Files.walk(root).filter { Files.isRegularFile(it) }.forEach {
            val input = it.toFile().readText()
            val ast = try {
                RhovasParser(it.toFile().readText()).parse()
            } catch(e: ParseException) {
                println(Diagnostic(it.fileName.toString(), input, e.error))
                throw e
            }
            val name = root.relativize(it).toString()
                .replace(".rho", "")
                .replace("/", ".")
            val clazz = Class.forName("dev.willbanders.rhovas.x.stdlib.${name}Kt")
            val func = clazz.getDeclaredMethod("def${name.replace(".", "")}", Environment::class.java, Environment.Type::class.java)
            val type = env.defType(name) {}.let { env.types[name]!! }
            func.invoke(null, env, type)
            verify(ast, type)
        }
        env.defType("Lambda") {}
    }

    private fun verify(ast: RhovasAst.Source, type: Environment.Type) {
        val funcs = mutableSetOf<Pair<String, Int>>()
        ast.mbrs.filterIsInstance<RhovasAst.Mbr.Cmpt.Class>().forEach { mbr ->
            require(type.name == mbr.name) { "Unexpected component on type ${type.name}: ${mbr.name}." }
            val props = mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Property>().map { it.name }.toSet()
            require(type.props.keys == props) { "Invalid properties on type ${type.name}: Expected " + (props - type.props.keys) + ", Unexpected " + (type.props.keys - props) + "."}
            val mthds = mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Function>().flatMap { func ->
                listOf(Pair(func.name, func.params.size)) + (func.op?.let { listOf(Pair(it, func.params.size)) } ?: listOf())
            }.toSet()
            require(type.mthds.keys == mthds) { "Invalid methods on type ${type.name}: Expected " + (mthds - type.mthds.keys) + ", Unexpected " + (type.mthds.keys - mthds) + "."}
            funcs.addAll(mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Constructor>().map { Pair(mbr.name, it.params.size) })
        }
        ast.mbrs.filterIsInstance<RhovasAst.Mbr.Cmpt.Interface>().forEach { mbr ->
            require(type.name == mbr.name) { "Unexpected component on type ${type.name}: ${mbr.name}." }
            val props = mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Property>().map { it.name }.toSet()
            require(type.props.keys == props) { "Invalid properties on type ${type.name}: Expected " + (props - type.props.keys) + ", Unexpected " + (type.props.keys - props) + "."}
            val mthds = mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Function>().flatMap { func ->
                listOf(Pair(func.name, func.params.size)) + (func.op?.let { listOf(Pair(it, func.params.size)) } ?: listOf())
            }.toSet()
            require(type.mthds.keys == mthds) { "Invalid methods on type ${type.name}: Expected " + (mthds - type.mthds.keys) + ", Unexpected " + (type.mthds.keys - mthds) + "."}
            funcs.addAll(mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Constructor>().map { Pair(mbr.name, it.params.size) })
        }
        val vars = ast.mbrs.filterIsInstance<RhovasAst.Mbr.Property>().map { it.name }.toSet()
        require(type.vars.keys == vars) { "Invalid variables on type ${type.name}: Expected " + (vars - type.vars.keys) + ", Unexpected " + (type.vars.keys - vars) + "." }
        funcs.addAll(ast.mbrs.filterIsInstance<RhovasAst.Mbr.Function>().map { Pair(it.name, it.params.size) }.toSet())
        require(type.funcs.keys == funcs) { "Invalid functions on type ${type.name}: Expected " + (vars - type.vars.keys) + ", Unexpected " + (type.vars.keys - vars) + "." }
        require(type.flds.keys.isEmpty()) { "Unexpected fields on type ${type.name}: ${type.flds.keys}." }
    }

}
