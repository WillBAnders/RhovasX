package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defineBoolean(env: Environment) {
    env.defType("Boolean") { t ->
        t.instance.defFunc("!", 0) {
            t.init(!(it[0].value as Boolean))
        }
        t.instance.defFunc("equals", 1) {
            t.init(it[0].value as Boolean == it[1].value as Boolean)
        }
        t.instance.defFunc("toString", 0) {
            env.reqType("String").init((it[0].value as Boolean).toString())
        }
    }
}