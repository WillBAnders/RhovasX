package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defMap(env: Environment, type: Environment.Type) {
    type.defProp("size", {
        env.init("Integer", (it[0] as Map<String, Environment.Object>).entries.toList().size) },
        { throw Exception("Map.size is read only.") },
    )
    type.defProp("keys",
        { env.init("List", (it[0].value as Map<String, Environment.Object>).keys.map { env.init("String", it) }.toList()) },
        { throw Exception("Map.keys is read only.") },
    )
    type.defProp("values",
        { env.init("List", (it[0].value as Map<String, Environment.Object>).values.toList()) },
        { throw Exception("Map.values is read only.") },
    )
    type.defProp("entries",
        { env.init("List", (it[0].value as Map<String, Environment.Object>).entries.map { env.init("List", listOf(env.init("String", it.key), it.value)) }.toList()) },
        { throw Exception("Map.entries is read only.") },
    )
    type.defMthd("map", 1) { args ->
        val lambda = args[1].value as Environment.Function
        env.init("Map", (args[0].value as Map<String, Environment.Object>).mapValues {
            lambda.invoke(listOf(env.init("List", listOf(env.init("String", it.key), it.value))))
        })
    }
    type.defMthd("filter", 1) { args ->
        val lambda = args[1].value as Environment.Function
        env.init("Map", (args[0].value as Map<String, Environment.Object>).filter {
            lambda.invoke(listOf(env.init("List", listOf(env.init("String", it.key), it.value)))).value as Boolean
        })
    }
    type.defMthd("[]", 1) { it[0].reqMthd("get", 1).invoke(it) }
    type.defMthd("get", 1) { args ->
        (args[0].value as Map<String, Environment.Object>)[args[1].value as String] ?: env.init("Null", null)
    }
    type.defMthd("[]=", 2) { it[0].reqMthd("set", 2).invoke(it) }
    type.defMthd("set", 2) { args ->
        (args[0].value as MutableMap<String, Environment.Object>)[args[1].value as String] = args[2]
        env.init("Null", null)
    }
    type.defMthd("iterate", 1) { args ->
        val lambda = args[1].value as Environment.Function
        (args[0].value as Map<String, Environment.Object>).forEach { lambda.invoke(listOf(env.init("List", listOf(env.init("String", it.key), it.value)))) }
        env.init("Null", null)
    }
    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it)}
    type.defMthd("equals", 1) { args ->
        val left = args[0].value as Map<String, Environment.Object>
        val right = args[1].value as Map<String, Environment.Object>
        env.init("Boolean", left.keys == right.keys && left.all { it.value.reqMthd("==", 1).invoke(listOf(it.value, right[it.key]!!)).value as Boolean })
    }
    type.defMthd("toString", 0) { args ->
        env.init("String", (args[0].value as Map<String, Environment.Object>).mapValues { it.value.reqMthd("toString", 0).invoke(listOf(it.value)).value as String }.toString())
    }
}
