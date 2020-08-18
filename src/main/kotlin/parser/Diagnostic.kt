package dev.willbanders.rhovas.x.parser

import java.lang.StringBuilder
import java.util.*

data class Diagnostic(
    val source: String,
    val input: String,
    val error: Error,
) {

    data class Error(
        val message: String,
        val details: String,
        val range: Range,
        val context: Set<Range>,
    )

    data class Range(
        val index: Int,
        val line: Int,
        val column: Int,
        val length: Int,
    )

    override fun toString(): String {
        val sb = StringBuilder()
            .append("Error: ").append(error.message).append("\n")
            .append("[")
            .append(source)
            .append(": Line ")
            .append(error.range.line)
            .append(", Characters ")
            .append(error.range.column)
            .append("-")
            .append(error.range.column + error.range.length)
            .append("]")
            .append("\n")
        val context = TreeSet<Range>(compareBy(Range::line))
        context.addAll(error.context + error.range)
        val digits = context.last().line.toString().length
        context.forEach {
            val start = it.index - (it.column - 1)
            val end = input.indexOfAny(charArrayOf('\n', '\r'), it.index + it.length)
            sb.append(it.line.toString().padStart(digits + 1))
                .append(" | ")
                .append(input.substring(start, if (end != -1) end else input.length))
                .append("\n")
            if (it.line == error.range.line) {
                sb.append("".padStart(digits + 1))
                    .append(" | ")
                    .append("".padStart(error.range.column - 1))
                    .append("^".repeat(error.range.length))
                    .append("\n")
            }
        }
        return sb.append(error.details).toString()
    }

}
