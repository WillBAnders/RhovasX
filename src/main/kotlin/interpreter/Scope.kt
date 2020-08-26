package dev.willbanders.rhovas.x.interpreter

class Scope(private val parent: Scope?) {

    val vars: MutableMap<String, Environment.Variable> = mutableMapOf()
    val funcs: MutableMap<Pair<String, Int>, Environment.Function> = mutableMapOf()

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
