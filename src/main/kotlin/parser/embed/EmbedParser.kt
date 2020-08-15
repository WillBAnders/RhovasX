package dev.willbanders.rhovas.x.parser.embed

import dev.willbanders.rhovas.x.parser.Lexer
import dev.willbanders.rhovas.x.parser.Parser
import java.lang.StringBuilder

class EmbedParser(chars: Lexer.CharStream) : Parser<EmbedTokenType>(EmbedLexer(chars)) {

    override fun parse(): EmbedAst {
        require(match("{"))
        val sb = StringBuilder()
        if (match(EmbedTokenType.INDENT)) {
            val indent = tokens[-1]!!.literal
            while (true) {
                if (match(EmbedTokenType.INDENT)) {
                    sb.append("\n")
                    val literal = tokens[-1]!!.literal
                    if (literal.isNotEmpty()) {
                        if (literal.length < indent.length) {
                            break
                        }
                        sb.append(literal.substring(indent.length))
                    }
                } else {
                    tokens.advance()
                    sb.append(tokens[-1]!!.literal)
                }
            }
        } else if (match(EmbedTokenType.TEXT)) {
            sb.append(tokens[-1]!!.literal.trim())
        }
        require(match("}")) { error(
                "Expected close brace.",
                "Embed source should end with a closing brace."
        )}
        return EmbedAst.Source(sb.toString())
    }

}
