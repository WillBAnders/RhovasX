package dev.willbanders.rhovas.x.stdlib

import java.lang.Math;
import dev.willbanders.rhovas.x.interpreter.Environment

fun modulo(l: Int, r: Int): Int{
    return (l - r * Math.floorDiv(l, r));
}

fun defineInteger(env: Environment) {
    env.defType("Integer") { t ->
        t.instance.defFunc("+", 1) {
            t.init((it[0].value as Int) + (it[1].value as Int))
        }
        t.instance.defFunc("-", 1) {
            t.init((it[0].value as Int) - (it[1].value as Int))
        }
        t.instance.defFunc("*", 1) {
            t.init((it[0].value as Int) * (it[1].value as Int))
        }
        t.instance.defFunc("/", 1) {
            t.init((it[0].value as Int) / (it[1].value as Int))
        }
        t.instance.defFunc("equals", 1) {
            t.init((it[0].value as Int) == (it[1].value as Int))
        }
        t.instance.defFunc("mod", 1) {
            t.init(modulo((it[0].value as Int), (it[1].value as Int)))
        }
        t.instance.defFunc("rem", 1) {
            t.init((it[0].value as Int) % (it[1].value as Int))
        }

        t.instance.defFunc("toString", 0) {
            env.reqType("String").init((it[0].value as Int).toString())
        }
    }
}