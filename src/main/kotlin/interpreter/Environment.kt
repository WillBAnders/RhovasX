package dev.willbanders.rhovas.x.interpreter

class Environment {

    val types: MutableMap<String, Type> = mutableMapOf()

    fun reqType(name: String): Type {
        return types[name] ?: throw Exception("Undefined type $name.")
    }

    fun defType(name: String, builder: (Type) -> Unit) {
        types[name] = Type(name).also(builder)
    }

    fun init(name: String, value: Any?): Object {
        return Object(reqType(name), value)
    }

    class Type(val name: String) {

        val extds: MutableMap<String, Type> = mutableMapOf()
        val vars: MutableMap<String, Variable> = mutableMapOf()
        val flds: MutableMap<String, Variable> = mutableMapOf()
        val props: MutableMap<String, Property> = mutableMapOf()
        val funcs: MutableMap<Pair<String, Int>, Function> = mutableMapOf()
        val mthds: MutableMap<Pair<String, Int>, Function> = mutableMapOf()

        fun getFld(name: String): Variable? {
            return flds[name] ?: extds.values.mapNotNull { it.getFld(name) }.firstOrNull()
        }

        fun getProp(name: String): Property? {
            return props[name] ?: extds.values.mapNotNull { it.getProp(name) }.firstOrNull()
        }

        fun getMthd(name: String, arity: Int): Function? {
            return mthds[Pair(name, arity)] ?: extds.values.mapNotNull { it.getMthd(name, arity) }.firstOrNull()
        }

        fun reqVar(name: String): Variable {
            return vars[name] ?: throw Exception("Undefined variable ${this.name}.$name.")
        }

        fun reqFld(name: String): Variable {
            return getFld(name) ?: throw Exception("Undefined field ${this.name}.$name.")
        }

        fun reqProp(name: String): Property {
            return getProp(name) ?: throw Exception("Undefined property ${this.name}.$name.")
        }

        fun reqFunc(name: String, arity: Int): Function {
            return funcs[Pair(name, arity)] ?: throw Exception("Undefined function ${this.name}.$name/$arity.")
        }

        fun reqMthd(name: String, arity: Int): Function {
            return getMthd(name, arity) ?: throw Exception("Undefined method ${this.name}.$name/$arity.")
        }

        fun defVar(name: String, value: Any?) {
            vars[name] = Variable(name, Object(this, value))
        }

        fun defFld(name: String, value: Any?) {
            flds[name] = Variable(name, Object(this, value))
        }

        fun defProp(name: String, get: (List<Object>) -> Object, set: (List<Object>) -> Unit) {
            props[name] = Property(name, get, set)
        }

        fun defFunc(name: String, arity: Int, invoke: (List<Object>) -> Object) {
            funcs[Pair(name, arity)] = Function(name, arity, invoke)
        }

        fun defMthd(name: String, arity: Int, invoke: (List<Object>) -> Object) {
            mthds[Pair(name, arity)] = Function(name, arity, invoke)
        }

        fun isSubtypeOf(other: Type): Boolean {
            return other.name == name || extds.values.any { it.isSubtypeOf(other) }
        }

    }

    data class Variable(
        val name: String,
        var value: Object,
    )

    data class Property(
        val name: String,
        val get: (List<Object>) -> Object,
        val set: (List<Object>) -> Unit,
    )

    data class Function(
        val name: String,
        val arity: Int,
        val invoke: (List<Object>) -> Object,
    )

    data class Object(
        val type: Type,
        val value: Any?,
    ) {

        fun reqProp(name: String): Property {
            return type.reqProp(name)
        }

        fun reqMthd(name: String, arity: Int): Function {
            return type.reqMthd(name, arity)
        }

        fun reqType(other: Type) {
            if (!type.isSubtypeOf(other)) {
                throw Exception("Object of type ${type.name} is not a subtype of type ${other.name}.")
            }
        }

    }

}
