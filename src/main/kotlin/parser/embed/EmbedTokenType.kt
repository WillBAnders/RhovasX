package dev.willbanders.rhovas.x.parser.embed

import dev.willbanders.rhovas.x.parser.Token

enum class EmbedTokenType : Token.Type {
    INDENT,
    BRACE,
    TEXT
}
