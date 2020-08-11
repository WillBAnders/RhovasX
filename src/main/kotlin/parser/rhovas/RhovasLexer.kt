package dev.willbanders.rhovas.x.parser.rhovas

import dev.willbanders.rhovas.x.parser.Lexer
import dev.willbanders.rhovas.x.parser.Token

class RhovasLexer(input: String) : Lexer<RhovasTokenType>(input) {

    override fun lexToken(): Token<RhovasTokenType>? {
        while (match("[ \t\n\r]")) {}
        chars.reset()
        return when {
            chars[0] == null -> null
            peek("[A-Za-z]") -> lexIdentifier()
            peek("[0-9]") -> lexNumber()
            peek('\'') -> lexCharacter()
            peek('\"') -> lexString()
            else -> lexOperator()
        }
    }

    private fun lexIdentifier(): Token<RhovasTokenType> {
        require(match("[A-Za-z]"))
        while (match("[A-Za-z_-]")) {}
        return chars.emit(RhovasTokenType.IDENTIFIER)
    }

    private fun lexNumber(): Token<RhovasTokenType> {
        require(match("[0-9]"))
        while (match("[0-9]")) {}
        if (match('.', "[0-9]")) {
            while (match("[0-9]")) {}
            return chars.emit(RhovasTokenType.DECIMAL)
        }
        return chars.emit(RhovasTokenType.INTEGER)
    }

    private fun lexCharacter(): Token<RhovasTokenType> {
        require(match('\''))
        require(match("[^\'\n\r]")) { "Invalid character literal." }
        require(match('\'')) { "Unterminated character literal." }
        return chars.emit(RhovasTokenType.CHARACTER)
    }

    private fun lexString(): Token<RhovasTokenType> {
        require(match('\"'))
        while (match("[^\"\n\r]")) {}
        require(match('\"')) { "Unterminated string literal." }
        return chars.emit(RhovasTokenType.STRING)
    }

    private fun lexOperator(): Token<RhovasTokenType> {
        chars.advance()
        return chars.emit(RhovasTokenType.OPERATOR)
    }

}
