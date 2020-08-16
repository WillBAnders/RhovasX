package dev.willbanders.rhovas.x.interpreter

class Scope(val parent: Scope? = null) {

    val variables = mutableMapOf<String, Variable>()
    val functions = mutableMapOf<Pair<String, Int>, Function>()

    fun getVariable(name: String): Variable {
        return when {
            variables.containsKey(name) -> variables[name]!!
            parent != null -> parent.getVariable(name)
            else -> throw Exception("Undefined variable $name.")
        }
    }

    fun getFunction(name: String, arity: Int): Function {
        return when {
            functions.containsKey(Pair(name, arity)) -> functions[Pair(name, arity)]!!
            parent != null -> parent.getFunction(name, arity)
            else -> throw Exception("Undefined function $name/$arity.")
        }
    }

    data class Variable(
        val mut: Boolean,
        val name: String,
        var value: Any?
    )

    data class Function(
        val name: String,
        val arity: Int,
        val apply: (List<Any?>) -> Any?
    )

}
