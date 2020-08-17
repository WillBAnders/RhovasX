package dev.willbanders.rhovas.x.interpreter

class Environment {

    val scope = Scope(null)

    class Scope(private val parent: Scope?) {

        private val variables = mutableMapOf<String, Variable>()
        private val functions = mutableMapOf<Pair<String, Int>, Function>()

        fun lookupVariable(name: String): Variable? {
            return variables[name] ?: parent?.lookupVariable(name)
        }

        fun lookupFunction(name: String, arity: Int): Function? {
            return functions[Pair(name, arity)] ?: parent?.lookupFunction(name, arity)
        }

        fun defineVariable(name: String, value: Any? = null) {
            variables[name] =
                Variable(name, value)
        }

        fun defineFunction(name: String, arity: Int, invoke: (List<Any?>) -> Any?) {
            functions[Pair(name, arity)] =
                Function(name, arity, invoke)
        }

    }

    data class Variable(
        val name: String,
        var value: Any?
    )

    data class Function(
        val name: String,
        val arity: Int,
        val invoke: (List<Any?>) -> Any?
    )

}
