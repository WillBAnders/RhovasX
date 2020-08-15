package dev.willbanders.rhovas.x.parser.embed

import dev.willbanders.rhovas.x.parser.Lexer
import dev.willbanders.rhovas.x.parser.Token

class EmbedLexer(chars: CharStream) : Lexer<EmbedTokenType>(chars) {

    override fun lexToken(): Token<EmbedTokenType>? {
        return when {
            chars[0] == null -> null
            match('\n', '\r') ||
            match('\r', '\n') ||
            match("[\n\r]") -> {
                chars.reset()
                chars.newline()
                while (match("[ \t]")) {}
                chars.emit(EmbedTokenType.INDENT)
            }
            match("[{}]") -> {
                chars.emit(EmbedTokenType.BRACE)
            }
            else -> {
                while (match("[^{}\n\r]")) {}
                chars.emit(EmbedTokenType.TEXT)
            }
        }
    }

}
