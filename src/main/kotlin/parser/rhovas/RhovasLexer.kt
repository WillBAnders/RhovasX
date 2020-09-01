package dev.willbanders.rhovas.x.parser.rhovas

import dev.willbanders.rhovas.x.parser.Lexer
import dev.willbanders.rhovas.x.parser.Token

class RhovasLexer(input: String) : Lexer<RhovasTokenType>(input) {

    override fun lexToken(): Token<RhovasTokenType>? {
        while (peek("[ \t\n\r]")) {
            if (match('\n', '\r') || match('\r', '\n') || match("[\n\r]")) {
                chars.reset()
                chars.newline()
            } else {
                chars.advance()
            }
        }
        chars.reset()
        return when {
            chars[0] == null -> null
            peek("[A-Za-z_]") -> lexIdentifier()
            peek("[0-9]") -> lexNumber()
            peek('\'') -> lexCharacter()
            peek('\"') -> lexString()
            peek('-', '-') -> lexComment()
            else -> lexOperator()
        }
    }

    private fun lexIdentifier(): Token<RhovasTokenType> {
        require(match("[A-Za-z_]"))
        while (match("[A-Za-z_0-9]")) {}
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
        require(match("[^\'\n\r]")) { error(
            "Invalid character literal.",
            "A character literal must start and end with a single quote (\'), contain a single character, and cannot span multiple lines."
        )}
        require(match('\'')) { error(
            "Unterminated character literal.",
            "A character literal must start and end with a single quote (\'), contain a single character, and cannot span multiple lines."
        )}
        return chars.emit(RhovasTokenType.CHARACTER)
    }

    private fun lexString(): Token<RhovasTokenType> {
        require(match('\"'))
        while (match("[^\"\n\r]")) {}
        require(match('\"')) { error(
            "Unterminated string literal.",
            "A string literal must start and end with a double quote (\") and cannot span multiple lines."
        )}
        return chars.emit(RhovasTokenType.STRING)
    }

    private fun lexComment(): Token<RhovasTokenType>? {
        match('-', '-')
        if (match('[')) {
            while (chars[0] != null && !match('-', '-', ']')) { chars.advance() }
        } else {
            while (match("[^\n\r]")) {}
        }
        chars.reset()
        return lexToken()
    }

    private fun lexOperator(): Token<RhovasTokenType> {
        chars.advance()
        return chars.emit(RhovasTokenType.OPERATOR)
    }

}
