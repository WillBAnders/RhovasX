package dev.willbanders.rhovas.x.interpreter

class Scope(private val parent: Scope?) {

    val types: MutableMap<String, Environment.Type> = mutableMapOf()
    val vars: MutableMap<String, Environment.Variable> = mutableMapOf()
    val funcs: MutableMap<Pair<String, Int>, Environment.Function> = mutableMapOf()

    fun reqType(name: String): Environment.Type {
        return types[name]
            ?: parent?.reqType(name)
            ?: throw Exception("Undefined type $name.")
    }

    fun reqVar(name: String): Environment.Variable {
        return vars[name]
            ?: parent?.reqVar(name)
            ?: throw Exception("Undefined variable $name.")
    }

    fun reqFunc(name: String, arity: Int): Environment.Function {
        return funcs[Pair(name, arity)]
            ?: parent?.reqFunc(name, arity)
            ?: throw Exception("Undefined function $name/$arity.")
    }

}
