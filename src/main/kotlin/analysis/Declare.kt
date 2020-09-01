package dev.willbanders.rhovas.x.analysis

import dev.willbanders.rhovas.x.interpreter.ENV
import dev.willbanders.rhovas.x.interpreter.Environment
import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst

class Declare(private val name: String): RhovasAst.Visitor<Any?>() {

    val types = mutableMapOf<String, Environment.Type>() //TODO Scoping of names
    lateinit var type: Environment.Type

    override fun visit(ast: RhovasAst.Source) {
        ENV.defType(name) { type = it }
        types[""] = type
        ast.mbrs.filterIsInstance<RhovasAst.Mbr.Cmpt>().forEach { visit(it) }
    }

    override fun visit(ast: RhovasAst.Mbr.Cmpt.Class) {
        val t = type
        if (name != type.name || name.substringAfterLast(".") != ast.type.name) {
            ENV.defType(type.name + "." + ast.type.name, type.scope) {type = it }
            types[ast.type.name] = type
        }
        ast.mbrs.filterIsInstance<RhovasAst.Mbr.Cmpt>().forEach { visit(it) }
        type = t
    }

    override fun visit(ast: RhovasAst.Mbr.Cmpt.Interface) {
        val t = type
        if (name != type.name || name.substringAfterLast(".") != ast.type.name) {
            ENV.defType(type.name + "." + ast.type.name, type.scope) { type = it }
            types[ast.type.name] = type
        }
        ast.mbrs.filterIsInstance<RhovasAst.Mbr.Cmpt>().forEach { visit(it) }
        type = t
    }

    override fun visit(ast: RhovasAst.Mbr.Cmpt.Struct) {
        val t = type
        if (name != type.name || name.substringAfterLast(".") != ast.type.name) {
            ENV.defType(type.name + "." + ast.type.name, type.scope) { type = it }
            types[ast.type.name] = type
        }
        ast.mbrs.filterIsInstance<RhovasAst.Mbr.Cmpt>().forEach { visit(it) }
        type = t
    }

}
