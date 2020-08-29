package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defBoolean(env: Environment, type: Environment.Type) {
    type.defMthd("!", 0) { it[0].reqMthd("negate", 0).invoke(it) }
    type.defMthd("negate", 0) { args ->
        env.init("Boolean", !(args[0].value as Boolean))
    }

    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it) }
    type.defMthd("equals", 1) { args ->
        env.init("Boolean", (args[0].value as Boolean) == (args[1].value as Boolean))
    }

    type.defMthd("toString", 0) { args ->
        env.init("String", (args[0].value.toString()))
    }
}