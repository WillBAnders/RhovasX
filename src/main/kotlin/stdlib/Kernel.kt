package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defKernel(env: Environment, type: Environment.Type) {
    type.defFunc("print", 1) { args ->
        println(args[0].reqMthd("toString", 0).invoke(listOf(args[0])).value)
        env.init("Null", null)
    }
    type.defFunc("range", 2) { args ->
        type.reqFunc("range",3).invoke(args + listOf(env.init("Boolean", false)))
    }
    type.defFunc("range", 3) { args ->
        val range = if (args[2].value as? Boolean ?: args[2].type.name == "Atom" && args[2].value as String == "incl") {
            (args[0].value as Int).rangeTo(args[1].value as Int)
        } else {
            (args[0].value as Int).until(args[1].value as Int)
        }
        env.init("List", range.map { env.init("Integer", it) })
    }
}
