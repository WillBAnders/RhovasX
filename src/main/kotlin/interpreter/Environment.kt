package dev.willbanders.rhovas.x.interpreter

class Environment {

    val scope = Scope(null)
    val types = mutableMapOf<String, Type>()

    fun getType(name: String): Type? {
        return types[name]
    }

    fun reqType(name: String): Type {
        return types[name] ?: throw Exception("Undefined type $name.")
    }

    fun defType(name: String, scope: Scope = Scope(null), builder: (Type) -> Unit) {
        val static = Scope(scope)
        val instance = Scope(static)
        types[name] = Type(name, static, instance).also(builder)
    }

    class Scope(private val parent: Scope?) {

        val variables = mutableMapOf<String, Variable>()
        val functions = mutableMapOf<Pair<String, Int>, Function>()

        fun getVar(name: String): Variable? {
            return variables[name] ?: parent?.getVar(name)
        }

        fun getFunc(name: String, arity: Int): Function? {
            return functions[Pair(name, arity)] ?: parent?.getFunc(name, arity)
        }

        fun reqVar(name: String): Variable {
            return getVar(name) ?: throw Exception("Undefined variable $name.")
        }

        fun reqFunc(name: String, arity: Int): Function {
            return getFunc(name, arity) ?: throw Exception("Undefined function $name/$arity.")
        }

        fun defVar(name: String, value: Object) {
            variables[name] = Variable(name, value)
        }

        fun defFunc(name: String, arity: Int, invoke: (List<Object>) -> Object) {
            functions[Pair(name, arity)] = Function(name, arity, invoke)
        }

    }

    data class Type(
        val name: String,
        val static: Scope,
        val instance: Scope,
    ) {

        fun init(value: Any?): Object {
            return Object(this, Scope(instance), value)
        }

    }

    data class Variable(
        val name: String,
        var value: Object,
    )

    data class Function(
        val name: String,
        val arity: Int,
        val invoke: (List<Object>) -> Object,
    )

    data class Object(
        val type: Type,
        val scope: Scope,
        val value: Any?,
    ) {

        fun getProp(name: String): Variable? {
            return scope.variables[name] ?: type.instance.getFunc(name, 0)?.let {
                Variable(name, it.invoke(listOf()))
            }
        }

        fun reqProp(name: String): Variable {
            return getProp(name) ?: throw Exception("Undefined property $name on object of type ${type.name}.")
        }

        fun getMthd(name: String, arity: Int): Function? {
            return type.instance.getFunc(name, arity)?.let { f ->
                Function(f.name, f.arity) { f.invoke(listOf(this) + it) }
            }
        }

        fun reqMthd(name: String, arity: Int): Function {
            return getMthd(name, arity) ?: throw Exception("Undefined method $name/$arity on object of type ${type.name}.")
        }

    }

}
