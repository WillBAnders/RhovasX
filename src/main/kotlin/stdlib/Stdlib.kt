package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment
import dev.willbanders.rhovas.x.parser.Diagnostic
import dev.willbanders.rhovas.x.parser.ParseException
import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst
import dev.willbanders.rhovas.x.parser.rhovas.RhovasParser
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object Stdlib {

    fun init(env: Environment) {
        val root = Paths.get("src/main/resources/stdlib")
        Files.walk(root).filter { Files.isRegularFile(it) }.forEach {
            getOrInit(env, root.relativize(it).toString().replace(".rho", "").replace("/", "."))
        }
        env.defType("Struct") { type ->
            type.extds["Equatable"] = env.reqType("Equatable")
        }
        env.defType("Lambda") {}
        env.defType("KeywordVariable") { type ->
            type.defProp("val",
                { it[0].value as Environment.Object },
                { throw Exception("val is read only.") },
            )
        }
        env.scope.funcs.putAll(env.reqType("Kernel").funcs)
    }

    private fun getOrInit(env: Environment, name: String): Environment.Type {
        if (!env.scope.types.containsKey(name)) {
            env.defType(name) { init(env, it) }
        }
        return env.reqType(name)
    }

    private fun init(env: Environment, type: Environment.Type) {
        val input = File("src/main/resources/stdlib/${type.name.replace(".", "/")}.rho").readText()
        val ast = try {
            RhovasParser(input).parse()
        } catch(e: ParseException) {
            println(Diagnostic(type.name, input, e.error))
            throw e
        }
        ast.mbrs.forEach { when(it) {
            is RhovasAst.Mbr.Cmpt.Class -> it.extends.forEach { type.extds[it.name] = getOrInit(env, it.name) }
            is RhovasAst.Mbr.Cmpt.Interface -> it.extends.forEach { type.extds[it.name] = getOrInit(env, it.name) }
        } }
        val clazz = Class.forName("dev.willbanders.rhovas.x.stdlib.${type.name}Kt")
        val func = clazz.getDeclaredMethod("def${type.name.replace(".", "")}", Environment::class.java, Environment.Type::class.java)
        func.invoke(null, env, type)
        verify(ast, type)
    }

    private fun verify(ast: RhovasAst.Source, type: Environment.Type) {
        val funcs = mutableSetOf<Pair<String, Int>>()
        ast.mbrs.filterIsInstance<RhovasAst.Mbr.Cmpt.Class>().forEach { mbr ->
            require(type.name == mbr.type.name) { "Unexpected component on type ${type.name}: ${mbr.type.name}." }
            val props = mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Property>().map { it.name }.toSet()
            require(type.props.keys == props) { "Invalid properties on type ${type.name}: Expected " + (props - type.props.keys) + ", Unexpected " + (type.props.keys - props) + "."}
            val mthds = mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Function>().flatMap { func ->
                listOf(Pair(func.name, func.params.size)) + (func.op?.let { listOf(Pair(it, func.params.size)) } ?: listOf())
            }.toSet()
            require(type.mthds.keys == mthds) { "Invalid methods on type ${type.name}: Expected " + (mthds - type.mthds.keys) + ", Unexpected " + (type.mthds.keys - mthds) + "."}
            funcs.addAll(mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Constructor>().map { Pair(mbr.type.name, it.params.size) })
        }
        ast.mbrs.filterIsInstance<RhovasAst.Mbr.Cmpt.Interface>().forEach { mbr ->
            require(type.name == mbr.type.name) { "Unexpected component on type ${type.name}: ${mbr.type.name}." }
            val props = mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Property>().map { it.name }.toSet()
            require(type.props.keys == props) { "Invalid properties on type ${type.name}: Expected " + (props - type.props.keys) + ", Unexpected " + (type.props.keys - props) + "."}
            val mthds = mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Function>().flatMap { func ->
                listOf(Pair(func.name, func.params.size)) + (func.op?.let { listOf(Pair(it, func.params.size)) } ?: listOf())
            }.toSet()
            require(type.mthds.keys == mthds) { "Invalid methods on type ${type.name}: Expected " + (mthds - type.mthds.keys) + ", Unexpected " + (type.mthds.keys - mthds) + "."}
            funcs.addAll(mbr.mbrs.filterIsInstance<RhovasAst.Mbr.Constructor>().map { Pair(mbr.type.name, it.params.size) })
        }
        val vars = ast.mbrs.filterIsInstance<RhovasAst.Mbr.Property>().map { it.name }.toSet()
        require(type.vars.keys == vars) { "Invalid variables on type ${type.name}: Expected " + (vars - type.vars.keys) + ", Unexpected " + (type.vars.keys - vars) + "." }
        funcs.addAll(ast.mbrs.filterIsInstance<RhovasAst.Mbr.Function>().map { Pair(it.name, it.params.size) }.toSet())
        require(type.funcs.keys == funcs) { "Invalid functions on type ${type.name}: Expected " + (vars - type.vars.keys) + ", Unexpected " + (type.vars.keys - vars) + "." }
        require(type.flds.keys.isEmpty()) { "Unexpected fields on type ${type.name}: ${type.flds.keys}." }
    }

}
