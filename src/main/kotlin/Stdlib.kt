package dev.willbanders.rhovas.x

import dev.willbanders.rhovas.x.interpreter.Environment

fun init(env: Environment) {
    env.defType("Null") { t ->
        t.defMthd("toString", 0) {
            env.init("String", "null")
        }
    }
    env.defType("Boolean") { t ->
        t.defMthd("!", 0) {
            env.init("Boolean", !(it[0].value as Boolean))
        }
        t.defMthd("toString", 0) {
            env.init("String", it[0].value.toString())
        }
    }
    env.defType("Integer") { t ->
        t.defMthd("+", 1) {
            env.init("Integer", it[0].value as Int + it[1].value as Int)
        }
        t.defMthd("-", 1) {
            env.init("Integer", it[0].value as Int - it[1].value as Int)
        }
        t.defMthd("*", 1) {
            env.init("Integer", it[0].value as Int * it[1].value as Int)
        }
        t.defMthd("/", 1) {
            env.init("Integer", it[0].value as Int / it[1].value as Int)
        }
        t.defMthd("==", 1) {
            env.init("Boolean", it[0].value as Int == it[1].value)
        }
        t.defMthd("compare", 1) {
            when ((it[0].value as Int).compareTo(it[1].value as Int)) {
                -1 -> env.init("Atom", "lt")
                1 -> env.init("Atom", "gt")
                0 -> env.init("Atom", "eq")
                else -> throw AssertionError()
            }
        }
        t.defMthd("toString", 0) {
            env.init("String", it[0].value.toString())
        }
    }
    env.defType("Decimal") { t ->
        t.defMthd("+", 1) {
            env.init("Decimal", it[0].value as Double + it[1].value as Double)
        }
        t.defMthd("-", 1) {
            env.init("Decimal", it[0].value as Double - it[1].value as Double)
        }
        t.defMthd("*", 1) {
            env.init("Decimal", it[0].value as Double * it[1].value as Double)
        }
        t.defMthd("/", 1) {
            env.init("Decimal", it[0].value as Double / it[1].value as Double)
        }
        t.defMthd("==", 1) {
            env.init("Boolean", it[0].value as Double == it[1].value)
        }
        t.defMthd("compare", 1) {
            when ((it[0].value as Double).compareTo(it[1].value as Double)) {
                -1 -> env.init("Atom", "lt")
                1 -> env.init("Atom", "gt")
                0 -> env.init("Atom", "eq")
                else -> throw AssertionError()
            }
        }
        t.defMthd("toString", 0) {
            env.init("String", it[0].value.toString())
        }
    }
    env.defType("Character") { t ->
        t.defMthd("==", 1) {
            env.init("Boolean", it[0].value as Char == it[1].value)
        }
        t.defMthd("compare", 1) {
            when ((it[0].value as String).compareTo(it[1].value as String)) {
                -1 -> env.init("Atom", "lt")
                1 -> env.init("Atom", "gt")
                0 -> env.init("Atom", "eq")
                else -> throw AssertionError()
            }
        }
        t.defMthd("toString", 0) {
            env.init("String", it[0].value.toString())
        }
    }
    env.defType("String") { t ->
        t.defMthd("length", 0) {
            env.init("Integer", (it[0].value as String).length)
        }
        t.defMthd("substring", 1) {
            env.init("String", (it[0].value as String).substring(it[1].value as Int))
        }
        t.defMthd("+", 1) {
            env.init("String", it[0].value as String + it[1].reqMthd("toString", 0).invoke(listOf(it[1])).value)
        }
        t.defMthd("==", 1) {
            env.init("Boolean", it[0].value as String == it[1].value)
        }
        t.defMthd("compare", 1) {
            when ((it[0].value as String).compareTo(it[1].value as String)) {
                -1 -> env.init("Atom", "lt")
                1 -> env.init("Atom", "gt")
                0 -> env.init("Atom", "eq")
                else -> throw AssertionError()
            }
        }
        t.defMthd("toString", 0) {
            env.init("String", it[0].value.toString())
        }
    }
    env.defType("Atom") { t ->
        t.defMthd("==", 1) {
            env.init("Boolean", it[1].type == t && it[0].value as String == it[1].value)
        }
        t.defMthd("toString", 0) {
            env.init("String", ":" + it[0].value.toString())
        }
    }
    env.defType("List") { t ->
        t.defMthd("size", 0) {
            env.init("Integer", (it[0].value as List<Environment.Object>).size)
        }
        t.defMthd("iterate", 1) {
            val lambda = it[1].value as Environment.Function
            (it[0].value as List<Environment.Object>).forEach {
                lambda.invoke(listOf(it))
            }
            env.init("Null", null)
        }
        t.defMthd("map", 1) {
            val lambda = it[1].value as Environment.Function
            env.init("List", (it[0].value as List<Environment.Object>).map {
                lambda.invoke(listOf(it))
            })
        }
        t.defMthd("[]", 1) {
            (it[0].value as List<Environment.Object>)[it[1].value as Int]
        }
        t.defMthd("[]=", 2) {
            (it[0].value as MutableList<Environment.Object>)[it[1].value as Int] = it[2]
            env.init("Null", null)
        }
        t.defMthd("==", 1) {
            env.init("Boolean", it[1].type == t && it[0].value as List<Environment.Object> == it[1].value)
        }
        t.defMthd("toString", 0) {
            env.init("String", (it[0].value as List<Environment.Object>).map {
                it.reqMthd("toString", 0).invoke(listOf(it)).value as String
            }.toString())
        }
    }
    env.defType("Map") { t ->
        t.defMthd("size", 0) {
            env.init("Integer", (it[0].value as Map<String, Environment.Object>).size)
        }
        t.defMthd("iterate", 1) {
            val lambda = it[1].value as Environment.Function
            (it[0].value as Map<String, Environment.Object>).forEach {
                lambda.invoke(listOf(env.init("List", listOf(env.init("String", it.key), it.value))))
            }
            env.init("Null", null)
        }
        t.defMthd("map", 1) {
            val lambda = it[1].value as Environment.Function
            env.init("Map", (it[0].value as Map<String, Environment.Object>).mapValues {
                lambda.invoke(listOf(env.init("List", listOf(env.init("String", it.key), it.value))))
            })
        }
        t.defMthd("[]", 1) {
            (it[0].value as Map<String, Environment.Object>)[it[1] as String]
                ?: env.init("Null", null)
        }
        t.defMthd("[]=", 2) {
            (it[0].value as MutableMap<String, Environment.Object>)[it[1].value as String] = it[2]
            env.init("Null", null)
        }
        t.defMthd("==", 1) {
            env.init("Boolean", it[1].type == t && it[0].value as Map<String, Environment.Object> == it[1].value)
        }
        t.defMthd("toString", 0) {
            env.init("String", (it[0].value as Map<String, Environment.Object>).mapValues {
                it.value.reqMthd("toString", 0).invoke(listOf(it.value)).value as String
            })
        }
    }
    env.defType("Lambda") {}
    env.defType("Kernel") { t ->
        t.defFunc("print", 1) {
            println(it[0].reqMthd("toString", 0).invoke(listOf(it[0])).value)
            env.init("Null", null)
        }
        t.defFunc("range", 2) {
            t.reqFunc("range",3).invoke(it + listOf(env.init("Boolean", false)))
        }
        t.defFunc("range", 3) {
            val incl = it[2].value as? Boolean == true || it[2].value == "incl"
            env.init("List", (it[0].value as Int).rangeTo((it[1].value as Int - if (incl) 0 else 1)).toList().map {
                env.init("Integer", it)
            })
        }
    }
}
