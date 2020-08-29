package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defComparable(env: Environment, type: Environment.Type) {
    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it) }
    type.defMthd("equals", 1) { args ->
        env.init("Boolean", args[0].reqMthd("compare", 1).invoke(args).value as String == "eq")
    }
    type.defMthd("<=>", 1) { it[0].reqMthd("compare", 1).invoke(it) }
    type.defMthd("compare", 1) { throw Exception("Abstract") }
}
