package dev.willbanders.rhovas.x.parser

data class Token<T : Token.Type>(
    val type: T,
    val literal: String,
    val range: Diagnostic.Range,
) {

    interface Type

}
