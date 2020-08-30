package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defAtom(env: Environment, type: Environment.Type) {
    type.defProp("name",
        { env.init("String", (it[0]).toString()) },
        { throw Exception("Atom.name is read only.") },
    )


    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it) }
    type.defMthd("equals", 1) { args ->
        env.init("Boolean", args[0].value as String == args[1].value as String)
    }

    type.defMthd("toString", 0) { args ->
        env.init("String", ":" + args[0].value as String)
    }


}
