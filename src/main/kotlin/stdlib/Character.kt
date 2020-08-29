package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defCharacter(env: Environment, type: Environment.Type) {
    type.defMthd("+", 1) { it[0].reqMthd("concat", 1).invoke(it) }
    type.defMthd("concat", 1) { args ->
        env.init("String", args[0].value.toString() + args[1].reqMthd("toString", 0).invoke(listOf(args[1])).value as String)
    }

    type.defMthd("<=>", 1) { it[0].reqMthd("compare", 1).invoke(it) }
    type.defMthd("compare", 1) { args ->
        when ((args[0].value as Char).compareTo(args[1].value as Char)) {
            -1 -> env.init("Atom", "lt")
            1 -> env.init("Atom", "gt")
            0 -> env.init("Atom", "eq")
            else -> throw AssertionError()
        }
    }

    type.defMthd("hashCode", 0) { args ->
        env.init("Integer", (args[0].value as Char).hashCode())
    }

    type.defMthd("toString", 0) { args ->
        env.init("String", (args[0].value as Char).toString())
    }


}
