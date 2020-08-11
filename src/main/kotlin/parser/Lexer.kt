package dev.willbanders.rhovas.x.parser

abstract class Lexer<T : Token.Type>(input: String) {

    val chars = CharStream(input)

    fun lex(): List<Token<T>> {
        return generateSequence { lexToken() }.toList()
    }

    abstract fun lexToken(): Token<T>?

    protected fun match(vararg objects: Any): Boolean {
        val peek = peek(*objects)
        if (peek) {
            repeat(objects.size) { chars.advance() }
        }
        return peek
    }

    protected fun peek(vararg objects: Any): Boolean {
        return objects.withIndex().all { o ->
            chars[o.index]?.let { test(o.value, it) } == true
        }
    }

    private fun test(obj: Any, char: Char): Boolean {
        return when(obj) {
            is Char -> obj == char
            is String -> char.toString().matches(obj.toRegex())
            is List<*> -> obj.any { test(it!!, char) }
            else -> throw AssertionError()
        }
    }

    protected fun require(condition: Boolean, message: () -> String = { "Broken lexer invariant." }) {
        if (!condition) {
            throw Exception(message() + " @ " + chars.index)
        }
    }

    class CharStream(private val input: String) {

        var index = 0
        var length = 0

        operator fun get(offset: Int): Char? {
            return input.getOrNull(index + offset)
        }

        fun advance() {
            index++
            length++
        }

        fun reset(index: Int? = null) {
            if (index != null) {
                this.index = index
            }
            length = 0
        }

        fun <T : Token.Type> emit(type: T): Token<T> {
            return Token(type, input.substring(index - length, index), index - length).also { reset() }
        }

    }

}
