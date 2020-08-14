package dev.willbanders.rhovas.x.parser

abstract class Lexer<T : Token.Type>(input: String) {

    val chars = CharStream(input)

    fun lex(): List<Token<T>> {
        return generateSequence { lexToken() }.toList()
    }

    /*
        lexToken() is lazy, so it's called when the parser needs another token. 
        If the lexer is out of input, it returns null instead.
     */
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

    protected fun require(condition: Boolean) {
        return require(condition) { error(
            "Broken lexer invariant.",
            "Please report this issue, this should never happen!"
        )}
    }

    protected fun require(condition: Boolean, error: () -> Diagnostic.Error) {
        if (!condition) {
            throw ParseException(error())
        }
    }

    protected fun error(message: String, details: String) : Diagnostic.Error {
        return Diagnostic.Error(message, details, chars.range, emptySet())
    }

    class CharStream(private val input: String) {

        var index = 0
        var line = 1
        var column = 1
        var length = 0

        val range get() = Diagnostic.Range(index - length, line, column - length, length)

        operator fun get(offset: Int): Char? {
            return input.getOrNull(index + offset)
        }

        fun advance() {
            index++
            column++
            length++
        }

        fun newline() {
            line++
            column = 1
        }

        fun reset(index: Int? = null) {
            if (index != null) {
                this.index = index
                column -= length
            }
            length = 0
        }

        fun <T : Token.Type> emit(type: T): Token<T> {
            return Token(type, input.substring(index - length, index), range).also { reset() }
        }

    }

}
