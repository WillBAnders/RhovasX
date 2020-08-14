package dev.willbanders.rhovas.x.parser

data class ParseException(val error: Diagnostic.Error) : Exception(error.message)
