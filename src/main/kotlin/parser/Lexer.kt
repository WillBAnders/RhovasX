package dev.willbanders.rhovas.x.parser

abstract class Lexer<T : Token.Type>(input: String) {

    val chars: CharStream = CharStream(input)

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

    protected fun require(condition: Boolean) {
        return require(condition) { error(
            "Broken lexer invariant.",
            "Please report this issue, this should never happen!\n" + Exception().toString()
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

    inner class CharStream(val input: String) {

        private var index = 0
        private var line = 1
        private var column = 1
        private var length = 0

        val range get() = Diagnostic.Range(index, line, column, length)

        operator fun get(offset: Int): Char? {
            return input.getOrNull(index + length + offset)
        }

        fun advance() {
            require(this[0] != null)
            length ++
        }

        fun newline() {
            require(length == 0)
            line++
            column = 1
        }

        fun reset(range: Diagnostic.Range? = null) {
            index = range?.index ?: index + length
            line = range?.line ?: line
            column = range?.column ?: column + length
            length = range?.length ?: 0
        }

        fun <T : Token.Type> emit(type: T): Token<T> {
            return Token(type, input.substring(index, index + length), range).also {
                index += length
                column += length
                length = 0
            }
        }

    }

}
