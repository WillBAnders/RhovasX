package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defList(env: Environment, type: Environment.Type) {
    type.defProp("size",
        { env.init("Integer", (it[0] as List<Environment.Object>).size) },
        { throw Exception("List.size is read only.") },
    )
    type.defMthd("[]", 1) { it[0].reqMthd("get", 1).invoke(it) }
    type.defMthd("get", 1) { args ->
        (args[0].value as List<Environment.Object>)[(args[1].value as Int)]
    }
    type.defMthd("[]=", 2) { it[0].reqMthd("set", 2).invoke(it) }
    type.defMthd("set", 2) { args ->
        (args[0].value as MutableList<Environment.Object>)[args[1].value as Int] = args[2];
        env.init("Null", null)
    }
    type.defMthd("+", 1) { it[0].reqMthd("concat", 1).invoke(it) }
    type.defMthd("concat", 1) { args ->
        env.init("List", (args[0].value as List<Environment.Object>) + (args[1].value as List<Environment.Object>))
    }
    type.defMthd("iterate", 1) { args ->
        val lambda = args[1].value as Environment.Function
        (args[0].value as List<Environment.Object>).forEach { lambda.invoke(listOf(it)) }
        env.init("Null", null)
    }
    type.defMthd("map", 1) { args ->
        val lambda = args[1].value as Environment.Function
        env.init("List", (args[0].value as List<Environment.Object>).map { lambda.invoke(listOf(it)) })
    }
    type.defMthd("filter", 1) { args ->
        val lambda = args[1].value as Environment.Function
        env.init("List", (args[0].value as List<Environment.Object>).filter { lambda.invoke(listOf(it)).value as Boolean })
    }
    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it)}
    type.defMthd("equals", 1) { args ->
        val left = args[0].value as List<Environment.Object>
        val right = args[0].value as List<Environment.Object>
        env.init("Boolean", left.size == right.size && left.zip(right).all { it.first.reqMthd("==", 1).invoke(listOf(it.first, it.second)).value as Boolean })
    }
    type.defMthd("toString", 0) { args ->
        env.init("String", (args[0].value as List<Environment.Object>).map { it.reqMthd("toString", 0).invoke(listOf(it)).value as String }.toString())
    }
}
