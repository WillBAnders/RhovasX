package dev.willbanders.rhovas.x.parser.rhovas

import dev.willbanders.rhovas.x.parser.Token

enum class RhovasTokenType : Token.Type {
    IDENTIFIER,
    INTEGER,
    DECIMAL,
    CHARACTER,
    STRING,
    OPERATOR
}
