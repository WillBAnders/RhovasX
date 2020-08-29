package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defMap(env: Environment, type: Environment.Type) {
    type.defProp("keys",
        {
            val k = (it[0] as Map<Environment.Object, Environment.Object>).keys.toList()
            env.init("List", k)
        },
        { throw Exception("Map.keys is read only.") },
    )

    type.defProp("values",
        {
            val v = (it[0] as Map<Environment.Object, Environment.Object>).values.toList()
            env.init("List", v)
        },
        { throw Exception("Map.values is read only.") },
    )

    type.defProp("entries",
        {
            val e = (it[0] as Map<Environment.Object, Environment.Object>).entries.toList()
            env.init("List", e)
        },
        { throw Exception("Map.entries is read only.") },
    )

    type.defMthd("[]", 1) { it[0].reqMthd("get", 1).invoke(it) }
    type.defMthd("get", 1) { args ->
        env.init("Map", (args[0].value as Map<Environment.Object, Environment.Object>)[(args[1].value as Environment.Object)])
    }

    type.defMthd("[]=", 2) { it[0].reqMthd("set", 2).invoke(it) }
    type.defMthd("set", 2) { args ->
        (args[0].value as MutableMap<Environment.Object, Environment.Object>)[args[1].value as Environment.Object] = args[2];
        env.init("Null", null)
    }

    type.defMthd("iterate", 1) { args ->
        val lambda = args[1].value as Environment.Function
        val keys = env.init("List", (args[0].value as Map<Environment.Object, Environment.Object>).keys)
        val values = env.init("List", (args[0].value as Map<Environment.Object, Environment.Object>).values)
        (args[0].value as Map<Environment.Object, Environment.Object>).forEach { lambda.invoke(listOf(keys, values)) }
        env.init("Null", null)
    }

    type.defMthd("map", 1) { args ->
        val lambda = args[1].value as Environment.Function
        val keys = env.init("List", (args[0].value as Map<Environment.Object, Environment.Object>).keys)
        val values = env.init("List", (args[0].value as Map<Environment.Object, Environment.Object>).values)
        val mapped = (args[0].value as Map<Environment.Object, Environment.Object>).map { lambda.invoke(listOf(keys, values)) }
        env.init("Map", mapped)
    }

    type.defMthd("filter", 1) { args ->
        val lambda = args[1].value as Environment.Function
        val keys = env.init("List", (args[0].value as Map<Environment.Object, Environment.Object>).keys)
        val values = env.init("List", (args[0].value as Map<Environment.Object, Environment.Object>).values)
        val filtered = (args[0].value as Map<Environment.Object, Environment.Object>).filter { (lambda.invoke(listOf(keys, values)).value as Boolean) }
        env.init("Map", filtered)
    }

    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it)}
    type.defMthd("equals", 1) { args ->
        env.init("Boolean", (args[0].value as Map<Environment.Object, Environment.Object>) == (args[1].value as Map<Environment.Object, Environment.Object>))
    }

    type.defMthd("toString", 0) { args ->
        env.init("String", (args[0].value as Map<Environment.Object, Environment.Object>).toString())
    }

}
