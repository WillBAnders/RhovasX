package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defCharacter(env: Environment, type: Environment.Type) {
    type.defProp("codepoint",
        { env.init("Integer", (it[0].value as Char).toInt()) },
        { throw Exception("Character.codepoint is read only.") },
    )
    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it) }
    type.defMthd("equals", 1) { args ->
        env.init("Boolean", (args[0].value as Char) == (args[1].value as Char))
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
    type.defMthd("toString", 0) { args ->
        env.init("String", (args[0].value as Char).toString())
    }
}
