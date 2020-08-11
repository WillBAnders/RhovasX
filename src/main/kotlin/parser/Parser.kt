package dev.willbanders.rhovas.x.parser

abstract class Parser<T : Token.Type>(private val lexer: Lexer<T>) {

    protected val tokens = TokenStream()

    abstract fun parse(): Any

    protected fun match(vararg objects: Any): Boolean {
        val peek = peek(*objects)
        if (peek) {
            repeat(objects.size) { tokens.advance() }
        }
        return peek
    }

    protected fun peek(vararg objects: Any): Boolean {
        return objects.withIndex().all { o ->
            tokens[o.index]?.let { test(o.value, it) } == true
        }
    }

    private fun test(obj: Any, token: Token<T>): Boolean {
        return when(obj) {
            is Token.Type -> obj == token.type
            is String -> obj == token.literal
            is List<*> -> obj.any { test(it!!, token) }
            else -> throw AssertionError()
        }
    }

    protected fun require(condition: Boolean, message: () -> String = { "Broken parser invariant." }) {
        if (!condition) {
            throw Exception(message() + " @ " + (tokens[0]?.index ?: tokens[-1]!!.index))
        }
    }

    inner class TokenStream {

        private val tokens = mutableListOf<Token<T>?>()
        private var index = 0

        operator fun get(offset: Int): Token<T>? {
            return tokens.getOrElse(index + offset) {
                while (index + offset >= tokens.size) {
                    tokens.add(lexer.lexToken())
                }
                tokens.last()
            }
        }

        fun advance() {
            index++
        }

    }

}
