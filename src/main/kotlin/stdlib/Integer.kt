package dev.willbanders.rhovas.x.stdlib

import dev.willbanders.rhovas.x.interpreter.Environment

fun defInteger(env: Environment, type: Environment.Type) {
    type.defMthd("+", 1) { it[0].reqMthd("add", 1).invoke(it) }
    type.defMthd("add", 1) { args ->
        env.init("Integer", args[0].value as Int + args[1].value as Int)
    }
    type.defMthd("-", 1) { it[0].reqMthd("sub", 1).invoke(it) }
    type.defMthd("sub", 1) { args ->
        env.init("Integer", args[0].value as Int - args[1].value as Int)
    }
    type.defMthd("*", 1) { it[0].reqMthd("mul", 1).invoke(it) }
    type.defMthd("mul", 1) { args ->
        env.init("Integer", args[0].value as Int * args[1].value as Int)
    }
    type.defMthd("/", 1) { it[0].reqMthd("div", 1).invoke(it) }
    type.defMthd("div", 1) { args ->
        env.init("Integer", args[0].value as Int / args[1].value as Int)
    }
    type.defMthd("mod", 1) { args ->
        env.init("Integer", Math.floorMod(args[0].value as Int, args[1].value as Int))
    }
    type.defMthd("rem", 1) { args ->
        env.init("Integer", (args[0].value as Int).rem(args[1].value as Int))
    }
    type.defMthd("==", 1) { it[0].reqMthd("equals", 1).invoke(it) }
    type.defMthd("equals", 1) { args ->
        env.init("Boolean", args[0].value as Int == args[1].value as Int)
    }
    type.defMthd("<=>", 1) { it[0].reqMthd("compare", 1).invoke(it) }
    type.defMthd("compare", 1) { args ->
        when ((args[0].value as Int).compareTo(args[1].value as Int)) {
            -1 -> env.init("Atom", "lt")
            1 -> env.init("Atom", "gt")
            0 -> env.init("Atom", "eq")
            else -> throw AssertionError()
        }
    }
    type.defMthd("toString", 0) { args ->
        env.init("String", (args[0].value as Int).toString())
    }
}
