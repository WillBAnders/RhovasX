package dev.willbanders.rhovas.x.interpreter

abstract class Interpreter<T> {

    abstract fun eval(ast: T) : Any?

}
