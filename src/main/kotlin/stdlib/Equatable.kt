package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defEquatable(env: Environment, type: Environment.Type) {
    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it) }
    type.defMthd("equals", 1) { throw Exception("Abstract") }
}
