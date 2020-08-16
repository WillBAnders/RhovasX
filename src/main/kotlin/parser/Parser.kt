package dev.willbanders.rhovas.x.parser

import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

abstract class Parser<T : Token.Type>(val lexer: Lexer<T>) {

    protected val tokens = TokenStream()
    protected val context = Stack<Diagnostic.Range>()

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

    protected fun require(condition: Boolean) {
        return require(condition) {
            val writer = StringWriter()
            Exception().printStackTrace(PrintWriter(writer))
            error(
            "Broken parser invariant.",
                "Please report this issue, this should never happen!\n$writer"
            )
        }
    }

    protected fun require(condition: Boolean, error: () -> Diagnostic.Error) {
        if (!condition) {
            throw ParseException(error())
        }
    }

    protected fun error(message: String, details: String) : Diagnostic.Error {
        val range = (tokens[0] ?: tokens[-1])?.range ?: Diagnostic.Range(0, 1, 1, 0)
        return Diagnostic.Error(message, details, range, context.toHashSet())
    }

    inner class TokenStream {

        private val tokens = mutableListOf<Token<T>?>()
        private var index = 0

        operator fun get(offset: Int): Token<T>? {
            return tokens.getOrElse(index + offset) {
                while (index + offset >= tokens.size) {
                    try {
                        tokens.add(lexer.lexToken())
                    } catch (e: ParseException) {
                        throw ParseException(e.error.copy(context = context.toHashSet())).initCause(e)
                    }
                }
                tokens.last()
            }
        }

        fun advance() {
            require(this[0] != null)
            index++
        }

    }

}
