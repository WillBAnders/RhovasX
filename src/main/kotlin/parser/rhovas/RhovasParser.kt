package dev.willbanders.rhovas.x.parser.rhovas

import dev.willbanders.rhovas.x.parser.Parser

class RhovasParser(input: String) : Parser<RhovasTokenType>(RhovasLexer(input)) {

    override fun parse(): RhovasAst {
        return parseStatement()
    }

    private fun parseStatement(): RhovasAst.Statement {
        require(tokens[0] != null) { "Expected a statement." }
        return when (tokens[0]!!.literal) {
            "{" -> parseBlockStmt()
            "var", "val" -> parseDeclarationStmt()
            "if" -> parseIfStmt()
            "for" -> parseForStmt()
            "while" -> parseWhileStmt()
            else -> {
                val expr = parseExpression()
                val stmt = if (match("=")) {
                    val value = parseExpression()
                    RhovasAst.AssignmentStmt(expr, value)
                } else {
                    RhovasAst.ExpressionStmt(expr)
                }
                require(match(";")) { "Expected semicolon." }
                stmt
            }
        }
    }

    private fun parseBlockStmt(): RhovasAst.BlockStmt {
        val stmts = parseSeq("{", null, "}", ::parseStatement)
        return RhovasAst.BlockStmt(stmts)
    }

    private fun parseDeclarationStmt(): RhovasAst.DeclarationStmt {
        require(match(listOf("var", "val")))
        val mut = tokens[-1]!!.literal == "var"
        require(match(RhovasTokenType.IDENTIFIER)) { "Expected variable identifier." }
        val name = tokens[-1]!!.literal
        val expr = if (match("=")) parseExpression() else null
        match(";")
        return RhovasAst.DeclarationStmt(mut, name, expr)
    }

    private fun parseIfStmt(): RhovasAst.IfStmt {
        require(match("if"))
        require(match("(")) { "Expected opening parentheses." }
        val cond = parseExpression()
        require(match(")")) { "Expected closing parentheses." }
        val ifStmt = parseStatement()
        val elseStmt = if (match("else")) parseStatement() else null
        return RhovasAst.IfStmt(cond, ifStmt, elseStmt)
    }

    private fun parseForStmt(): RhovasAst.ForStmt {
        require(match("for"))
        require(match("(")) { "Expected opening parentheses." }
        require(match(RhovasTokenType.IDENTIFIER)) { "Expected variable identifier." }
        val name = tokens[-1]!!.literal
        require(match("in")) { "Expected literal 'in'." }
        val expr = parseExpression()
        require(match(")")) { "Expected closing parentheses." }
        val body = parseStatement()
        return RhovasAst.ForStmt(name, expr, body)
    }

    private fun parseWhileStmt(): RhovasAst.WhileStmt {
        require(match("while"))
        require(match("(")) { "Expected opening parentheses." }
        val cond = parseExpression()
        require(match(")")) { "Expected closing parentheses." }
        val body = parseStatement()
        return RhovasAst.WhileStmt(cond, body)
    }

    private fun parseExpression(): RhovasAst.Expression {
        return parseLogicalOrExpr()
    }

    private fun parseLogicalOrExpr(): RhovasAst.Expression {
        return parseBinaryExpr(::parseLogicalAndExpr, "||")
    }

    private fun parseLogicalAndExpr(): RhovasAst.Expression {
        return parseBinaryExpr(::parseComparisonExpr, "&&")
    }

    private fun parseComparisonExpr(): RhovasAst.Expression {
        return parseBinaryExpr(::parseAdditiveExpr, "<", "<=", ">", ">=", "==", "===", "!=", "!==")
    }

    private fun parseAdditiveExpr(): RhovasAst.Expression {
        return parseBinaryExpr(::parseMultiplicativeExpr, "+", "-")
    }

    private fun parseMultiplicativeExpr(): RhovasAst.Expression {
        return parseBinaryExpr(::parseUnaryExpr, "*", "/")
    }

    private fun parseBinaryExpr(parser: () -> RhovasAst.Expression, vararg ops: String): RhovasAst.Expression {
        var expr = parser()
        while (true) {
            val op = ops.sorted().lastOrNull { o ->
                match(*o.toCharArray().map { it.toString() }.toTypedArray())
            } ?: break
            val right = parser()
            expr = RhovasAst.BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun parseUnaryExpr(): RhovasAst.Expression {
        if (match(listOf("+", "-", "!"))) {
            val op = tokens[-1]!!.literal
            val expr = parseUnaryExpr()
            return RhovasAst.UnaryExpr(op, expr)
        }
        return parseSecondaryExpr()
    }

    private fun parseSecondaryExpr(): RhovasAst.Expression {
        var expr = parsePrimaryExpr()
        while (match(".")) {
            require(match(RhovasTokenType.IDENTIFIER)) { "Expected a name follow period." }
            val name = tokens[-1]!!.literal
            expr = if (peek("(")) {
                val args = parseSeq("(", ",", ")", ::parseExpression)
                RhovasAst.FunctionExpr(expr, name, args)
            } else {
                RhovasAst.AccessExpr(expr, name)
            }
        }
        return expr
    }

    private fun parsePrimaryExpr(): RhovasAst.Expression {
        require(tokens[0] != null) { "Expected an expression." }
        return when {
            match("null") -> RhovasAst.LiteralExpr(null)
            match(listOf("true", "false")) -> RhovasAst.LiteralExpr(tokens[-1]!!.literal.toBoolean())
            match(RhovasTokenType.INTEGER) -> RhovasAst.LiteralExpr(tokens[-1]!!.literal.toInt())
            match(RhovasTokenType.DECIMAL) -> RhovasAst.LiteralExpr(tokens[-1]!!.literal.toDouble())
            match(RhovasTokenType.CHARACTER) -> RhovasAst.LiteralExpr(tokens[-1]!!.literal[1])
            match(RhovasTokenType.STRING) -> RhovasAst.LiteralExpr(tokens[-1]!!.literal.removeSurrounding("\""))
            match(RhovasTokenType.IDENTIFIER) -> {
                val name = tokens[-1]!!.literal
                if (peek("(")) {
                    val args = parseSeq("(", ",", ")", ::parseExpression)
                    RhovasAst.FunctionExpr(null, name, args)
                } else {
                    RhovasAst.AccessExpr(null, name)
                }
            }
            match("(") -> {
                val expr = parseExpression()
                require(match(")")) { "Expected closing parentheses" }
                RhovasAst.GroupExpr(expr)
            }
            else -> throw Exception("Unexpected token " + tokens[0]!!)
        }
    }

    private fun <T> parseSeq(start: String, sep: String?, end: String, parser: () -> T): List<T> {
        require(match(start))
        val list = mutableListOf<T>()
        while (!match(end)) {
            list.add(parser())
            if (sep != null && !peek(end)) {
                require(match(sep)) { "Missing separator $sep." }
            }
        }
        return list
    }

}