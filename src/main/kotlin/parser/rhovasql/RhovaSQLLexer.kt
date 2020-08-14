package dev.willbanders.rhovas.x.parser.rhovas

import dev.willbanders.rhovas.x.parser.Lexer
import dev.willbanders.rhovas.x.parser.Token

class RhovaSQLLexer(input : String) : Lexer<SQLTokenType>(input){
    override fun lexToken(): Token<SQLTokenType>? {
        while (peek("[ \t\n\r\w]")){
            if(match('\n', '\r') || match('\r', '\n') || match("[\n\r]")) {
                chars.newline()
            }
            else{
                chars.advance()
            }
        }
        chars.reset()
        return when {
            chars[0] == null -> null
            peek("[A-Za-z]") -> lexIdentifier()
            peek("[0-9]") -> lexNumber()
            peek("\'") -> lexString()
            peek("\"") -> lexString()
            else -> lexOperator()
            )
        }
    }

    private fun lexIdentifier(): Token<RhovaSQLTokenType> { 
        
    }
}