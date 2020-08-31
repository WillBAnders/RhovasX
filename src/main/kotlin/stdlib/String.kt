package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defString(env: Environment, type: Environment.Type) {
    type.defProp("length",
        { env.init("Integer", (it[0].value as String).length) },
        { throw Exception("String.length is read only.") },
    )
    type.defProp("chars",
        { env.init("List", (it[0].value as String).toCharArray().map { env.init("Character", it) }.toList()) },
        { throw Exception("String.chars is read only.") },
    )
    type.defMthd("+", 1) { it[0].reqMthd("concat", 1).invoke(it) }
    type.defMthd("concat", 1) { args ->
        env.init("String", args[0].value as String + args[1].reqMthd("toString", 0).invoke(listOf(args[1])).value as String)
    }
    type.defMthd("contains", 1) { args ->
        env.init("Boolean", (args[0].value as String).contains(args[1].value as String) )
    }
    type.defMthd("substring", 1) { args ->
        env.init("String", (args[0].value as String).substring(args[1].value as Int) )
    }
    type.defMthd("substring", 2) { args ->
        env.init("String", (args[0].value as String).substring(args[1].value as Int, args[2].value as Int) )
    }
    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it) }
    type.defMthd("equals", 1) { args ->
        env.init("Boolean", args[0].value as String == args[1].value as String)
    }
    type.defMthd("<=>", 1) { it[0].reqMthd("compare", 1).invoke(it) }
    type.defMthd("compare", 1) { args ->
        when ((args[0].value as String).compareTo(args[1].value as String)) {
            -1 -> env.init("Atom", "lt")
            1 -> env.init("Atom", "gt")
            0 -> env.init("Atom", "eq")
            else -> throw AssertionError()
        }
    }
    type.defMthd("toString", 0) { it[0] }
}
