package dev.willbanders.rhovas.x.parser.rhovas

import dev.willbanders.rhovas.x.parser.Diagnostic
import dev.willbanders.rhovas.x.parser.ParseException
import dev.willbanders.rhovas.x.parser.Parser
import dev.willbanders.rhovas.x.parser.embed.EmbedParser

class RhovasParser(input: String) : Parser<RhovasTokenType>(RhovasLexer(input)) {

    override fun parse(): RhovasAst {
        return parseSource()
    }

    private fun parseSource(): RhovasAst.Source {
        val impts = generateSequence {
            if (peek("import")) parseImport() else null
        }.toList()
        val mbrs = generateSequence {
            if (tokens[0] != null) parseMember() else null
        }.toList()
        return RhovasAst.Source(impts, mbrs)
    }

    private fun parseImport(): RhovasAst.Import {
        require(match("import"))
        val path = mutableListOf<String>()
        do {
            path.add(parseIdentifier { "An import path is a sequence of identifiers separated by periods `.`, as in `import A.B.C;`." })
        } while (match("."))
        val name = if (match("as")) parseIdentifier { "An import alias should be an identifier, as in `import A.B.C as Abc;`." } else null
        requireSemicolon { "An import must be followed by a semicolon `;`, as in `import x.y.z;`." }
        return RhovasAst.Import(path, name)
    }

    private fun parseMember(): RhovasAst.Member {
        return when (tokens[0]?.literal) {
            "class" -> parseClassCmpt()
            "interface" -> parseInterfaceCmpt()
            "var", "val" -> parsePropertyMbr()
            "ctor" -> parseConstructorMbr()
            "func" -> parseFunctionMbr()
            else -> throw ParseException(error(
                "Expected member declaration.",
                "A member is either a class, interface, property, constructor, or function, such as `class Name { ... }` or `func name() { ... }`."
            ))
        }
    }

    private fun parseClassCmpt(): RhovasAst.ClassCmpt {
        require(match("class"))
        context.push(tokens[-1]!!.range)
        val name = parseIdentifier { "A class declaration requires a name after `class`, as in `class Name { ... }`." }
        require(match("{")) { error(
            "Expected opening brace.",
            "The body of a class must be surrounded by braces `{}`, as in `class Name { ... }`."
        )}
        val mbrs = generateSequence {
            if (!match("}")) parseMember() else null
        }.toList()
        context.pop()
        return RhovasAst.ClassCmpt(name, mbrs)
    }

    private fun parseInterfaceCmpt(): RhovasAst.InterfaceCmpt {
        require(match("interface"))
        context.push(tokens[-1]!!.range)
        val name = parseIdentifier { "An interface declaration requires a name after `interface`, as in `interface Name { ... }`." }
        require(match("{")) { error(
            "Expected opening brace.",
            "The body of an interface must be surrounded by braces `{}`, as in `interface Name { ... }`."
        )}
        val mbrs = generateSequence {
            if (!match("}")) parseMember() else null
        }.toList()
        context.pop()
        return RhovasAst.InterfaceCmpt(name, mbrs)
    }

    private fun parsePropertyMbr(): RhovasAst.PropertyMbr {
        require(match(listOf("var", "val")))
        context.push(tokens[-1]!!.range)
        val mut = tokens[-1]!!.literal == "var"
        val name = parseIdentifier { "A property declaration requires a name after `var`/`val`, as in `var x;` and `val y = 0;`." }
        val expr = if (match("=")) parseExpression() else null
        requireSemicolon {"A property must be followed by a semicolon `;`, as in `var x;` and `val y = 0;`." }
        context.pop()
        return RhovasAst.PropertyMbr(mut, name, expr)
    }

    private fun parseConstructorMbr(): RhovasAst.ConstructorMbr {
        require(match("ctor"))
        context.push(tokens[-1]!!.range)
        require(match("(")) { error(
            "Expected opening parenthesis.",
            "A constructor declaration requires parentheses `()` after the name, as in `ctor() { ... }` and `ctor(x, y, z) { ... }`."
        )}
        val params = mutableListOf<String>()
        while (!match(")")) {
            params.add(parseIdentifier { "A constructor parameter requires a name, as in `ctor(name) { ... }`. This can also be caused by missing a closing parenthesis `)`." })
            require(peek(")") || match(",")) { error(
                "Expected closing parenthesis or comma.",
                "A constructor parameter must be followed by a closing parenthesis `)` or comma `,` for additional parameters, as in `ctor() { ... }` and `ctor(x, y, z) { ... }`."
            )}
        }
        val body = parseStatement()
        context.pop()
        return RhovasAst.ConstructorMbr(params, body)
    }

    private fun parseFunctionMbr(): RhovasAst.FunctionMbr {
        require(match("func"))
        context.push(tokens[-1]!!.range)
        val name = parseIdentifier { "A function declaration requires a name after `func`, as in `func name() { ... }`." }
        require(match("(")) { error(
            "Expected opening parenthesis.",
            "A function declaration requires parentheses `()` after the name, as in `func name() { ... }` and `func name(x, y, z) { ... }`."
        )}
        val params = mutableListOf<String>()
        while (!match(")")) {
            params.add(parseIdentifier { "A function parameter requires a name, as in `func f(name) { ... }`. This can also be caused by missing a closing parenthesis." })
            require(peek(")") || match(",")) { error(
                "Expected closing parenthesis or comma.",
                "A function parameter must be followed by a closing parenthesis `)` or comma `,` for additional parameters, as in `func name() { ... }` and `func name(x, y, z) { ... }`."
            )}
        }
        val body = parseStatement()
        context.pop()
        return RhovasAst.FunctionMbr(name, params, body)
    }

    private fun parseStatement(): RhovasAst.Statement {
        require(tokens[0] != null) { error(
            "Expected statement.",
            "The parser reached the end of the input, but expected to parse a statement such as `if`, `for`, or `return`."
        )}
        return when (tokens[0]!!.literal) {
            "{" -> parseBlockStmt()
            "var", "val" -> parseDeclarationStmt()
            "if" -> parseIfStmt()
            "match" -> parseMatchStmt()
            "for" -> parseForStmt()
            "while" -> parseWhileStmt()
            "return" -> parseReturnStmt()
            "#" -> RhovasAst.ExpressionStmt(parseDsl())
            else -> {
                context.push(tokens[0]!!.range)
                val expr = parseExpression()
                val stmt = if (match("=")) {
                    val value = parseExpression()
                    requireSemicolon { "An assignment statement must be followed by a semicolon `;`, as in `x = 0`." }
                    RhovasAst.AssignmentStmt(expr, value)
                } else {
                    requireSemicolon { "An expression statement must be followed by a semicolon `;`, as in `function();`." }
                    RhovasAst.ExpressionStmt(expr)
                }
                context.pop()
                stmt
            }
        }
    }

    private fun parseBlockStmt(): RhovasAst.BlockStmt {
        require(match("{"))
        context.push(tokens[-1]!!.range)
        val stmts = generateSequence {
            if (!match("}")) parseStatement() else null
        }.toList()
        context.pop()
        return RhovasAst.BlockStmt(stmts)
    }

    private fun parseDeclarationStmt(): RhovasAst.DeclarationStmt {
        require(match(listOf("var", "val")))
        context.push(tokens[-1]!!.range)
        val mut = tokens[-1]!!.literal == "var"
        val name = parseIdentifier { "A variable declaration requires a name following var/val, as in `var x;` and `val y = 0`." }
        val expr = if (match("=")) parseExpression() else null
        requireSemicolon { "A variable declaration must be followed by a semicolon `;`, as in `var x;` and `val y = 0;`." }
        context.pop()
        return RhovasAst.DeclarationStmt(mut, name, expr)
    }

    private fun parseIfStmt(): RhovasAst.IfStmt {
        require(match("if"))
        context.push(tokens[-1]!!.range)
        require(match("(")) { error(
            "Expected opening parenthesis.",
            "The condition of an if statement must be surrounded by parentheses `()`, as in `if (cond) { ... }`."
        )}
        val cond = parseExpression()
        require(match(")")) { error(
            "Expected closing parenthesis.",
            "The condition of an if statement must be surrounded by parentheses `()`, as in `if (cond) { ... }`."
        )}
        val ifStmt = parseStatement()
        val elseStmt = if (match("else")) parseStatement() else null
        context.pop()
        return RhovasAst.IfStmt(cond, ifStmt, elseStmt)
    }

    private fun parseMatchStmt(): RhovasAst.MatchStmt {
        require(match("match"))
        context.push(tokens[-1]!!.range)
        val args = mutableListOf<RhovasAst.Expression>()
        if (match("(")) {
            while (!match(")")) {
                args.add(parseExpression())
                require(peek(")") || match(",")) { error(
                    "Expected closing parenthesis or comma.",
                    "A match argument must be followed by a closing parenthesis `)` or comma `,` for additional arguments, as in `match (x, y, z) { cond: ... }`."
                )}
            }
        }
        require(match("{")) { error(
            "Expected opening brace.",
            "The cases of a match statement must be surrounded by braces `{}`, as in `match { cond: ... }`."
        )}
        val cases = mutableListOf<Pair<List<RhovasAst.Expression>, RhovasAst.Statement>>()
        while (!match("}")) {
            val exprs = mutableListOf<RhovasAst.Expression>()
            do {
                exprs.add(parseExpression())
                context.push(tokens[-1]!!.range)
            } while (match(","))
            require(match(":")) { error(
                "Expected colon.",
                "The condition of a match case must be followed by a colon `:`, as in `match { cond: ... }`."
            )}
            val stmt = parseStatement()
            repeat(exprs.size) { context.pop() }
            cases.add(Pair(exprs, stmt))
        }
        context.pop()
        return RhovasAst.MatchStmt(args, cases)
    }

    private fun parseForStmt(): RhovasAst.ForStmt {
        require(match("for"))
        context.push(tokens[-1]!!.range)
        require(match("(")) { error(
            "Expected opening parenthesis.",
            "The variable of a for statement must be surrounded by parentheses `()`, as in `for (elem in list) { ... }`."
        )}
        val name = parseIdentifier { "The variable of a for statement requires a name, as in `for (elem in list) { ... }`." }
        require(match("in")) { error(
            "Expected literal `in`.",
            "The variable of a for statement must be followed by `in`, as in `for (elem in list) { ... }`."
        )}
        val expr = parseExpression()
        require(match(")")) { error(
            "Expected closing parenthesis.",
            "The variable of a for statement must be surrounded by parentheses `()`, as in `for (element in array) { ... }`."
        )}
        val body = parseStatement()
        context.pop()
        return RhovasAst.ForStmt(name, expr, body)
    }

    private fun parseWhileStmt(): RhovasAst.WhileStmt {
        require(match("while"))
        context.push(tokens[-1]!!.range)
        require(match("(")) { error(
            "Expected opening parenthesis.",
            "The condition of a while statement must be surrounded by parentheses `()`, as in `while (cond) { ... }`."
        )}
        val cond = parseExpression()
        require(match(")")) { error(
            "Expected closing parenthesis.",
            "The condition of a while statement must be surrounded by parentheses `()`, as in `while (cond) { ... }`."
        )}
        val body = parseStatement()
        context.pop()
        return RhovasAst.WhileStmt(cond, body)
    }

    private fun parseReturnStmt(): RhovasAst.ReturnStmt {
        require(match("return"))
        context.push(tokens[-1]!!.range)
        val value = parseExpression()
        requireSemicolon { "A return statement must be followed by a semicolon `;`, as in `return 0;`." }
        context.pop()
        return RhovasAst.ReturnStmt(value)
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
        context.push(tokens[-1]!!.range)
        while (true) {
            val op = ops.sorted().lastOrNull { o ->
                match(*o.toCharArray().map { it.toString() }.toTypedArray())
            } ?: break
            val right = parser()
            context.pop()
            expr = RhovasAst.BinaryExpr(expr, op, right)
            context.push(tokens[-1]!!.range)
        }
        context.pop()
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
        val size = context.size
        context.push(tokens[-1]!!.range)
        while (match(".")) {
            val name = parseIdentifier { "A field access or method call requires a name, as in `x.y` or `x.z()`." }
            context.push(tokens[-1]!!.range)
            expr = if (match("(")) {
                val args = mutableListOf<RhovasAst.Expression>()
                while (!match(")")) {
                    args.add(parseExpression())
                    require(peek(")") || match(",")) { error(
                        "Expected closing parenthesis or comma.",
                        "A method argument must be followed by a closing parenthesis `)` or comma `,` for additional arguments, as in `obj.method()` or `obj.method(x, y, z)`."
                    )}
                }
                RhovasAst.FunctionExpr(expr, name, args)
            } else {
                RhovasAst.AccessExpr(expr, name)
            }
            context.pop()
            context.push(tokens[-1]!!.range)
        }
        repeat(context.size - size) { context.pop() }
        return expr
    }

    private fun parsePrimaryExpr(): RhovasAst.Expression {
        require(tokens[0] != null) { error(
            "Expected expression.",
            "The parser reached the end of the input, but expected to parse an expression such as `0`, `x`, or `f()`."
        )}
        return when {
            match("null") -> RhovasAst.ScalarLiteralExpr(null)
            match(listOf("true", "false")) -> RhovasAst.ScalarLiteralExpr(tokens[-1]!!.literal.toBoolean())
            match(RhovasTokenType.INTEGER) -> RhovasAst.ScalarLiteralExpr(tokens[-1]!!.literal.toInt())
            match(RhovasTokenType.DECIMAL) -> RhovasAst.ScalarLiteralExpr(tokens[-1]!!.literal.toDouble())
            match(RhovasTokenType.CHARACTER) -> RhovasAst.ScalarLiteralExpr(tokens[-1]!!.literal[1])
            match(RhovasTokenType.STRING) -> RhovasAst.ScalarLiteralExpr(tokens[-1]!!.literal.removeSurrounding("\""))
            match(RhovasTokenType.IDENTIFIER) -> {
                val name = tokens[-1]!!.literal
                context.push(tokens[-1]!!.range)
                val expr = if (peek("(")) {
                    val args = mutableListOf<RhovasAst.Expression>()
                    if (match("(")) {
                        while (!match(")")) {
                            args.add(parseExpression())
                            require(peek(")") || match(",")) { error(
                                "Expected closing parenthesis or comma.",
                                "A function argument must be followed by a closing parenthesis `)` or comma `,` for additional arguments, as in `function()` or `function(x, y, z)`."
                            )}
                        }
                    }
                    RhovasAst.FunctionExpr(null, name, args)
                } else {
                    RhovasAst.AccessExpr(null, name)
                }
                context.pop()
                expr
            }
            match("(") -> {
                val expr = parseExpression()
                require(match(")")) { error(
                    "Expected closing parenthesis",
                    "A group expression must be surrounded by parentheses `()`, as in `(variable)` and `(function())`."
                )}
                RhovasAst.GroupExpr(expr)
            }
            match("[") -> {
                val list = mutableListOf<RhovasAst.Expression>()
                while (!match("]")) {
                    list.add(parseExpression())
                    require(peek("]") || match(",")) { error(
                        "Expected closing square bracket or comma.",
                        "A list literal must be followed by a closing square bracket `]` or comma `,` for additional arguments, as in `[]` or `[x, y, z]`."
                    )}
                }
                RhovasAst.ListLiteralExpr(list)
            }
            match("{") -> {
                val map = mutableMapOf<String, RhovasAst.Expression>()
                while (!match("}")) {
                    val key = parseIdentifier { "An entry is a map literal must start with a key, as in `{x: 0}`." }
                    context.push(tokens[-1]!!.range)
                    require(match("=")) { error(
                        "Expected equal sign.",
                        "The value of a map entry must be followed by an equal sign `=`, as in `{x: 0}`."
                    )}
                    map[key] = parseExpression()
                    require(peek("}") || match(",")) { error(
                        "Expected closing brace or comma.",
                        "A map literal must be followed by a closing brace `}` or comma `,` for additional arguments, as in `{}` or `{x: 0, y: 1, z: 2}`."
                    )}
                    context.pop()
                }
                RhovasAst.MapLiteralExpr(map)
            }
            peek("#") -> parseDsl()
            else -> throw ParseException(error(
                "Unexpected token `${tokens[0]!!.literal}`.",
                "The parser expected to parse an expression, but received an unexpected token instead."
            ))
        }
    }

    private fun parseDsl(): RhovasAst.DslExpr {
        require(match("#"))
        context.push(tokens[-1]!!.range)
        val name = parseIdentifier { "A DSL must start with a hashtag `#` and must be followed by a name, as in `#dsl { ... }`." }
        require(match("{")) { error(
            "Expected opening brace.",
            "DSL source must start with a opening brace `{`, as in `#regex { /abc/i }`."
        )}
        val parser = EmbedParser(lexer.chars.input)
        parser.lexer.chars.reset(with(lexer.chars.range) { copy(index = index - 1, column = column - 1, length = 0) })
        val ast = try {
            parser.parse()
        } catch (e: ParseException) {
            throw ParseException(e.error.copy(context = e.error.context + context)).initCause(e)
        }
        lexer.chars.reset(with(parser.lexer.chars.range) { copy(index = index - 1, column = column - 1, length = 0) })
        require(match("}"))
        context.pop()
        return RhovasAst.DslExpr(name, ast)
    }

    private fun parseIdentifier(details: () -> String): String {
        require(match(RhovasTokenType.IDENTIFIER)) { error("Expected identifier.", details()) }
        return tokens[-1]!!.literal
    }

    private fun requireSemicolon(details: () -> String) {
        require(match(";")) {
            val range = with(tokens[-1]!!.range) { copy(index = index + length, column = column + length, length = 1) }
            Diagnostic.Error("Expected semicolon.", details(), range, context.toHashSet())
        }
    }

}
