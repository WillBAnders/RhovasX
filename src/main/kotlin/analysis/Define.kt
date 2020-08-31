package dev.willbanders.rhovas.x.analysis

import dev.willbanders.rhovas.x.interpreter.ENV
import dev.willbanders.rhovas.x.interpreter.Environment
import dev.willbanders.rhovas.x.interpreter.Interpreter
import dev.willbanders.rhovas.x.interpreter.Scope
import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst

class Define(types: Map<String, Environment.Type>): RhovasAst.Visitor<Unit>() {

    private val types = types.toMutableMap()
    private var type = types[""]!!
    private var static = true

    override fun visit(ast: RhovasAst.Source) {
        ast.impts.forEach { visit(it) }
        ast.mbrs.forEach { visit(it) }
    }

    override fun visit(ast: RhovasAst.Import) {
        val name = ast.name ?: ast.path.last()
        type.scope.types[name] = ENV.reqType(ast.path.joinToString("."))
    }

    override fun visit(ast: RhovasAst.Mbr.Cmpt.Class) {
        val t = type
        val s = static
        type = types[ast.type.name] ?: types[""]!!
        static = false
        ast.extends.forEach {
            type.extds[it.name] = types[it.name] ?: throw Exception("Undefined type ${it.name} (missing import?).")
        }
        ast.mbrs.forEach { visit(it) }
        type = t
        static = s
    }

    override fun visit(ast: RhovasAst.Mbr.Cmpt.Interface) {
        val t = type
        val s = static
        type = types[ast.type.name]!!
        static = false
        ast.extends.forEach {
            type.extds[it.name] = types[it.name] ?: throw Exception("Undefined type ${it.name} (missing import?).")
        }
        ast.mbrs.forEach { visit(it) }
        type = t
        static = s
    }

    override fun visit(ast: RhovasAst.Mbr.Property) {
        if (static) {
            val type = type
            type.defVar(ast.name, ENV.init("Lambda", Environment.Function(ast.name, 0) { args ->
                Interpreter.eval(Scope(type.scope)) {
                    ast.value?.let { visit(it) as Environment.Object } ?: ENV.init("Null", null)
                }
            }))
            type.scope.vars[ast.name] = type.vars[ast.name]!!
        } else {
            type.defFld(ast.name, ENV.init("Lambda", Environment.Function(ast.name, 0) { args ->
                Interpreter.eval(Scope(args[0].value as Scope)) {
                    ast.value?.let { visit(it) as Environment.Object } ?: ENV.init("Null", null)
                }
            }))
            type.defProp(ast.name,
                { (it[0].value as Scope).reqVar(ast.name).value },
                { (it[0].value as Scope).reqVar(ast.name).value = it[1] },
            )
        }
    }

    override fun visit(ast: RhovasAst.Mbr.Constructor) {
        if (static) {
            throw Exception("Constructor defined outside of component.")
        } else {
            val type = this.type
            type.defFunc("", ast.params.size) { args ->
                Interpreter.eval(Scope(type.scope)) {
                    val obj = Environment.Object(type, scope)
                    scope!!.vars["this"] = Environment.Variable("this", obj)
                    type.flds.values.forEach {
                        scope!!.vars[it.name] = Environment.Variable(it.name, (it.value.value as Environment.Function).invoke(listOf(obj)))
                    }
                    scoped(Scope(scope)) {
                        ast.params.withIndex().forEach {
                            scope!!.vars[it.value.name] = Environment.Variable(it.value.name, args[it.index])
                        }
                        try {
                            visit(ast.body)
                        } catch (ignored: Interpreter.Return) {}
                    }
                    obj
                }
            }
        }
    }

    override fun visit(ast: RhovasAst.Mbr.Function) {
        if (static) {
            val type = this.type
            type.defFunc(ast.name, ast.params.size) { args ->
                Interpreter.eval(Scope(type.scope)) {
                    ast.params.withIndex().forEach {
                        scope!!.vars[it.value.name] = Environment.Variable(it.value.name, args[it.index])
                    }
                    try {
                        Interpreter.visit(ast.body)
                        ENV.init("Null", null)
                    } catch (e: Interpreter.Return) {
                        e.value ?: ENV.init("Null", null)
                    }
                }
            }
            type.scope.funcs[Pair(ast.name, ast.params.size)] = type.funcs[Pair(ast.name, ast.params.size)]!!
        } else {
            type.defMthd(ast.name, ast.params.size) { args ->
                Interpreter.eval(Scope(args[0].value as Scope)) {
                    ast.params.withIndex().forEach {
                        scope!!.vars[it.value.name] = Environment.Variable(it.value.name, args[it.index + 1])
                    }
                    try {
                        visit(ast.body)
                        ENV.init("Null", null)
                    } catch (e: Interpreter.Return) {
                        e.value ?: ENV.init("Null", null)
                    }
                }
            }
        }
    }

}
