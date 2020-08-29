package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment
import javax.lang.model.type.NullType

fun defNull(env: Environment, type: Environment.Type)  {
    type.defMthd("==", 1) { args ->
        env.init("Boolean", (args[0].value as NullType) == (args[1].value as NullType) )
    }

}