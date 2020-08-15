package dev.willbanders.rhovas.x.parser.embed

sealed class EmbedAst {

    data class Source(
        val text: String
    ) : EmbedAst()

}
