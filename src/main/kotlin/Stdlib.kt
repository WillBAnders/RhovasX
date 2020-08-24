package dev.willbanders.rhovas.x

import dev.willbanders.rhovas.x.interpreter.Environment

fun init(env: Environment) {
    env.defType("Null") { t ->
        t.instance.defFunc("toString", 0) {
            env.reqType("String").init(it[0].value.toString())
        }
        t.instance.defFunc("==", 1) {
            env.reqType("Null").init(it[0].value == it[1].value)
        }
    }
    env.defType("Boolean") { t ->
        t.instance.defFunc("!", 0) {
            t.init(!(it[0].value as Boolean))
        }
        t.instance.defFunc("toString", 0) {
            env.reqType("String").init(it[0].value.toString())
        }
        t.instance.defFunc("==", 1) {
            env.reqType("Boolean").init(it[0].value as Boolean == it[1].value as Boolean)
        }
        t.instance.defFunc("compare", 1){
            env.reqType("Boolean").init(it[0].value as Boolean == it[1].value as Boolean)
        }
    }
    env.defType("Integer") { t ->
        t.instance.defFunc("+", 1) {
            t.init(it[0].value as Int + it[1].value as Int)
        }
        t.instance.defFunc("-", 1) {
            t.init(it[0].value as Int - it[1].value as Int)
        }
        t.instance.defFunc("*", 1) {
            t.init(it[0].value as Int * it[1].value as Int)
        }
        t.instance.defFunc("/", 1) {
            t.init(it[0].value as Int / it[1].value as Int)
        }
        t.instance.defFunc("==", 1) {
            env.reqType("Boolean").init(it[0].value as Int == it[1].value as Int)
        }
        t.instance.defFunc("compare", 1) {
            when ((it[0].value as Int).compareTo(it[1].value as Int)) {
                -1 -> env.reqType("Atom").init("lt")
                1 -> env.reqType("Atom").init("gt")
                0 -> env.reqType("Atom").init("eq")
                else -> throw AssertionError()
            }
        }
        t.instance.defFunc("toString", 0) {
            env.reqType("String").init(it[0].value.toString())
        }
    }
    env.defType("Decimal") { t ->
        t.instance.defFunc("+", 1) {
            t.init(it[0].value as Double + it[1].value as Double)
        }
        t.instance.defFunc("-", 1) {
            t.init(it[0].value as Double - it[1].value as Double)
        }
        t.instance.defFunc("*", 1) {
            t.init(it[0].value as Double * it[1].value as Double)
        }
        t.instance.defFunc("/", 1) {
            t.init(it[0].value as Double / it[1].value as Double)
        }
        t.instance.defFunc("==", 1) {
            env.reqType("Boolean").init(it[0].value as Double == it[1].value)
        }
        t.instance.defFunc("compare", 1) {
            when ((it[0].value as Double).compareTo(it[1].value as Double)) {
                -1 -> env.reqType("Atom").init("lt")
                1 -> env.reqType("Atom").init("gt")
                0 -> env.reqType("Atom").init("eq")
                else -> throw AssertionError()
            }
        }
        t.instance.defFunc("toString", 0) {
            env.reqType("String").init(it[0].value.toString())
        }
    }
    env.defType("Character") { t ->
        t.instance.defFunc("==", 1) {
            env.reqType("Boolean").init(it[0].value as Char == it[1].value)
        }
        t.instance.defFunc("compare", 1) {
            when ((it[0].value as String).compareTo(it[1].value as String)) {
                -1 -> env.reqType("Atom").init("lt")
                1 -> env.reqType("Atom").init("gt")
                0 -> env.reqType("Atom").init("eq")
                else -> throw AssertionError()
            }
        }
        t.instance.defFunc("toString", 0) {
            env.reqType("String").init(it[0].value.toString())
        }
    }
    env.defType("String") { t ->
        t.instance.defFunc("length", 0) {
            env.reqType("Integer").init((it[0].value as String).length)
        }
        t.instance.defFunc("substring", 1) {
            t.init((it[0].value as String).substring(it[1].value as Int))
        }
        t.instance.defFunc("+", 1) {
            t.init(it[0].value as String + it[1].reqMthd("toString", 0).invoke(listOf()).value)
        }
        t.instance.defFunc("==", 1) {
            env.reqType("Boolean").init(it[0].value as String == it[1].value)
        }
        t.instance.defFunc("compare", 1) {
            when ((it[0].value as String).compareTo(it[1].value as String)) {
                -1 -> env.reqType("Atom").init("lt")
                1 -> env.reqType("Atom").init("gt")
                0 -> env.reqType("Atom").init("eq")
                else -> throw AssertionError()
            }
        }
        t.instance.defFunc("toString", 0) {
            env.reqType("String").init(it[0].value.toString())
        }
    }
    env.defType("Atom") { t ->
        t.instance.defFunc("==", 1) {
            env.reqType("Boolean").init(it[1].type == t && it[0].value as String == it[1].value)
        }
        t.instance.defFunc("toString", 0) {
            env.reqType("String").init(":" + it[0].value.toString())
        }
    }
    env.defType("List") { t ->
        t.instance.defFunc("size", 0) {
            env.reqType("Integer").init((it[0].value as List<Environment.Object>).size)
        }
        t.instance.defFunc("iterate", 1) {
            val lambda = it[1].value as Environment.Function
            (it[0].value as List<Environment.Object>).forEach {
                lambda.invoke(listOf(it))
            }
            env.reqType("Null").init(null)
        }
        t.instance.defFunc("map", 1) {
            val lambda = it[1].value as Environment.Function
            t.init((it[0].value as List<Environment.Object>).map {
                lambda.invoke(listOf(it))
            })
        }
        t.instance.defFunc("[]", 1) {
            (it[0].value as List<Environment.Object>)[it[1].value as Int]
        }
        t.instance.defFunc("[]=", 2) {
            (it[0].value as MutableList<Environment.Object>)[it[1].value as Int] = it[2]
            env.reqType("Null").init(null)
        }
        t.instance.defFunc("==", 1) {
            env.reqType("Boolean").init(it[1].type == t && it[0].value as List<Environment.Object> == it[1].value)
        }
        t.instance.defFunc("toString", 0) {
            env.reqType("String").init((it[0].value as List<Environment.Object>).map {
                it.reqMthd("toString", 0).invoke(listOf()).value as String
            }.toString())
        }
    }
    env.defType("Map") { t ->
        t.instance.defFunc("size", 0) {
            env.reqType("Integer").init((it[0].value as Map<String, Environment.Object>).size)
        }
        t.instance.defFunc("iterate", 1) {
            val lambda = it[1].value as Environment.Function
            (it[0].value as Map<String, Environment.Object>).forEach {
                lambda.invoke(listOf(env.reqType("List").init(listOf(env.reqType("String").init(it.key), it.value))))
            }
            env.reqType("Null").init(null)
        }
        t.instance.defFunc("map", 1) {
            val lambda = it[1].value as Environment.Function
            t.init((it[0].value as Map<String, Environment.Object>).mapValues {
                lambda.invoke(listOf(env.reqType("List").init(listOf(env.reqType("String").init(it.key), it.value))))
            })
        }
        t.instance.defFunc("[]", 1) {
            (it[0].value as Map<String, Environment.Object>)[it[1] as String]
                ?: env.reqType("Null").init(null)
        }
        t.instance.defFunc("[]=", 2) {
            (it[0].value as MutableMap<String, Environment.Object>)[it[1].value as String] = it[2]
            env.reqType("Null").init(null)
        }
        t.instance.defFunc("==", 1) {
            env.reqType("Boolean").init(it[1].type == t && it[0].value as Map<String, Environment.Object> == it[1].value)
        }
        t.instance.defFunc("toString", 0) {
            env.reqType("String").init((it[0].value as Map<String, Environment.Object>).mapValues {
                it.value.reqMthd("toString", 0).invoke(listOf()).value as String
            })
        }
    }
    env.defType("Lambda") {}
    env.scope.defFunc("print", 1) {
        println(it[0].reqMthd("toString", 0).invoke(listOf()).value)
        env.reqType("Null").init(null)
    }
    env.scope.defFunc("range", 2) {
        env.scope.reqFunc("range",3).invoke(it + listOf(env.reqType("Boolean").init(false)))
    }
    env.scope.defFunc("range", 3) {
        val incl = it[2].value as? Boolean == true || it[2].value == "incl"
        env.reqType("List").init((it[0].value as Int).rangeTo((it[1].value as Int - if (incl) 0 else 1)).toList().map {
            env.reqType("Integer").init(it)
        })
    }
}
