package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment
import javax.lang.model.type.NullType

fun defNull(env: Environment, type: Environment.Type)  {
    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it) }
    type.defMthd("equals", 1) { args ->
        env.init("Boolean", args[0].value == args[1].value)
    }
    type.defMthd("toString", 0) { args ->
        env.init("String", args[0].value.toString())
    }
}