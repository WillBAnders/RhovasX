package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defMap(env: Environment, type: Environment.Type) {

    type.defProp("keys", { val k = (it[0] as Map<String, Environment.Object>).keys.toList(); env.init("List", k) }, { throw Exception("Map.keys is read only.") },)

    type.defProp("values", { val v = (it[0] as Map<String, Environment.Object>).values.toList(); env.init("List", v) }, { throw Exception("Map.values is read only.") },
    )

    type.defProp("entries",
        { env.init("List", (it[0] as Map<String, Environment.Object>).entries.map {
            env.init("List", listOf(env.init("String", it.key), it.value))
        }) },
        { throw Exception("Map.entries is read only.") },
    )

    type.defProp("size", {
        val v = (it[0] as Map<String, Environment.Object>).entries.toList().size;
        env.init("Integer", v) }, { throw Exception("Map.size is read only.") },
    )

    type.defMthd("map", 1) { args ->
        val lambda = args[1].value as Environment.Function
        env.init("Map", (args[0].value as Map<String, Environment.Object>).map {
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
        env.init("Map", (args[0].value as Map<String, Environment.Object>)[(args[1].value as Environment.Object)])
    }

    type.defMthd("[]=", 2) { it[0].reqMthd("set", 2).invoke(it) }
    type.defMthd("set", 2) { args ->
        (args[0].value as MutableMap<String, Environment.Object>)[args[1].value as String] = args[2];
        env.init("Null", null)
    }

    type.defMthd("iterate", 1) { args ->
        val lambda = args[1].value as Environment.Function
        val keys = env.init("List", (args[0].value as Map<String, Environment.Object>).keys)
        val values = env.init("List", (args[0].value as Map<String, Environment.Object>).values)
        (args[0].value as Map<String, Environment.Object>).forEach { lambda.invoke(listOf(keys, values)) }
        env.init("Null", null)
    }

    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it)}
    type.defMthd("equals", 1) { args ->

        val map1 = (args[0].value as Map<String, Environment.Object>)
        val map2 = (args[1].value as Map<String, Environment.Object>)
        var equals =  map1.keys == map2.keys && map1.keys.all {
            map1[it]!!.reqMthd("==", 1).invoke(listOf(map1[it]!!, map2[it]!!)).value == true
        }

        env.init("Boolean", equals)
    }

    type.defMthd("toString", 0) { args ->
        env.init("String", (args[0].value as Map<String, Environment.Object>).map {
            it.value.reqMthd("toString", 0).invoke(listOf(it.value)).value as String
        }.toString())
    }
}
