package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defineString(env: Environment) {
    env.defType("Boolean") { t ->
        t.instance.defFunc("+", 1) {
            t.init((it[0].value as String) + (it[1].value.toString()))
        }
        t.instance.defFunc("equals", 1) {
            t.init(it[0].value as String == it[1].value.toString())
        }
        t.instance.defFunc("toString", 0) {
            env.reqType("String").init(it[0])
        }
    }
}