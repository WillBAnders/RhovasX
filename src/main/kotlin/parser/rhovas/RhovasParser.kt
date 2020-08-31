package dev.willbanders.rhovas.x.parser.rhovas

import dev.willbanders.rhovas.x.parser.Diagnostic
import dev.willbanders.rhovas.x.parser.ParseException
import dev.willbanders.rhovas.x.parser.Parser
import dev.willbanders.rhovas.x.parser.embed.EmbedParser

class RhovasParser(input: String) : Parser<RhovasTokenType>(RhovasLexer(input)) {

    override fun parse(): RhovasAst.Source {
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

    private fun parseType(): RhovasAst.Type {
        val mut = when {
            match("+") -> RhovasAst.Type.Mutability.MUTABLE
            match("-") -> RhovasAst.Type.Mutability.IMMUTABLE
            else -> RhovasAst.Type.Mutability.VIEWABLE
        }
        val name = parseIdentifier { "A type must have a name, as in `String` and `List<String>`." }
        val generics = mutableListOf<RhovasAst.Type>()
        if (match("<")) {
            do {
                generics.add(parseType())
                require(peek(">") || match(",")) { error(
                    "Expected closing angle bracket or comma.",
                    "A generic type must be followed by a closing angle bracket `>` or comma `,` for additional arguments, as in `List<String>` or `Map<String, Integer>`."
                )}
            } while (!match(">"))
        }
        return RhovasAst.Type(mut, name, generics)
    }

    private fun parseParameter(): RhovasAst.Parameter {
        val name = parseIdentifier { "A parameter must have a name, as in `func f(name: Type)`." }
        require(match(":")) { error(
            "Expected colon.",
            "A parameter name must be followed by a type, as in `func f(name: Type)`."
        )}
        val type = parseType()
        return RhovasAst.Parameter(name, type)
    }

    private fun parseModifiers(): RhovasAst.Modifiers {
        val visibility = when {
            match("public") -> RhovasAst.Modifiers.Visibility.PUBLIC
            match("package") -> RhovasAst.Modifiers.Visibility.PACKAGE
            match("protected") -> RhovasAst.Modifiers.Visibility.PROTECTED
            match("private") -> RhovasAst.Modifiers.Visibility.PRIVATE
            else -> null
        }
        val virtual = match("virtual")
        val abstract = match("abstract")
        val override = match("override")
        return RhovasAst.Modifiers(visibility, virtual, abstract, override)
    }

    private fun parseMember(): RhovasAst.Mbr {
        val modifiers = parseModifiers()
        return when (tokens[0]?.literal) {
            "class" -> parseClassCmpt(modifiers)
            "interface" -> parseInterfaceCmpt(modifiers)
            "var", "val" -> parsePropertyMbr(modifiers)
            "ctor" -> parseConstructorMbr(modifiers)
            "func" -> parseFunctionMbr(modifiers)
            else -> throw ParseException(error(
                "Expected member declaration.",
                "A member is either a class, interface, property, constructor, or function, such as `class Name { ... }` or `func name() { ... }`."
            ))
        }
    }

    private fun parseClassCmpt(modifiers: RhovasAst.Modifiers): RhovasAst.Mbr.Cmpt.Class {
        require(match("class"))
        context.push(tokens[-1]!!.range)
        val type = parseType()
        val extends = if (match(":")) {
            val types = mutableListOf<RhovasAst.Type>()
            do {
                types.add(parseType())
            } while (match(","))
            types
        } else listOf()
        require(match("{")) { error(
            "Expected opening brace.",
            "The body of a class must be surrounded by braces `{}`, as in `class Name { ... }`."
        )}
        val mbrs = generateSequence {
            if (!match("}")) parseMember() else null
        }.toList()
        context.pop()
        return RhovasAst.Mbr.Cmpt.Class(modifiers, type, extends, mbrs)
    }

    private fun parseInterfaceCmpt(modifiers: RhovasAst.Modifiers): RhovasAst.Mbr.Cmpt.Interface {
        require(match("interface"))
        context.push(tokens[-1]!!.range)
        val type = parseType()
        val extends = if (match(":")) {
            val types = mutableListOf<RhovasAst.Type>()
            do {
                types.add(parseType())
            } while (match(","))
            types
        } else listOf()
        require(match("{")) { error(
            "Expected opening brace.",
            "The body of an interface must be surrounded by braces `{}`, as in `interface Name { ... }`."
        )}
        val mbrs = generateSequence {
            if (!match("}")) parseMember() else null
        }.toList()
        context.pop()
        return RhovasAst.Mbr.Cmpt.Interface(modifiers, type, extends, mbrs)
    }

    private fun parsePropertyMbr(modifiers: RhovasAst.Modifiers): RhovasAst.Mbr.Property {
        require(match(listOf("var", "val")))
        context.push(tokens[-1]!!.range)
        val mut = tokens[-1]!!.literal == "var"
        val name = parseIdentifier { "A property declaration requires a name after `var`/`val`, as in `var x;` and `val y = 0;`." }
        val type = if (match(":")) parseType() else null
        val expr = if (match("=")) parseExpr() else null
        requireSemicolon {"A property must be followed by a semicolon `;`, as in `var x;` and `val y = 0;`." }
        context.pop()
        return RhovasAst.Mbr.Property(modifiers, mut, name, type, expr)
    }

    private fun parseConstructorMbr(modifiers: RhovasAst.Modifiers): RhovasAst.Mbr.Constructor {
        require(match("ctor"))
        val ex = match("!")
        context.push(tokens[-1]!!.range)
        require(match("(")) { error(
            "Expected opening parenthesis.",
            "A constructor declaration requires parentheses `()` after the name, as in `ctor() { ... }` and `ctor(x, y, z) { ... }`."
        )}
        val params = mutableListOf<RhovasAst.Parameter>()
        while (!match(")")) {
            params.add(parseParameter())
            require(peek(")") || match(",")) { error(
                "Expected closing parenthesis or comma.",
                "A constructor parameter must be followed by a closing parenthesis `)` or comma `,` for additional parameters, as in `ctor() { ... }` and `ctor(x, y, z) { ... }`."
            )}
        }
        val throws = if (match("throws")) {
            val throws = mutableListOf<RhovasAst.Type>()
            do {
                throws.add(parseType())
            } while (match(","))
            throws
        } else listOf()
        val body = parseStatement()
        context.pop()
        return RhovasAst.Mbr.Constructor(modifiers, ex, params, throws, body)
    }

    private fun parseFunctionMbr(modifiers: RhovasAst.Modifiers): RhovasAst.Mbr.Function {
        require(match("func"))
        context.push(tokens[-1]!!.range)
        val op = if (match("op")) {
            generateSequence {
                if (match(RhovasTokenType.OPERATOR)) tokens[-1]!!.literal else null
            }.joinToString("")
        } else null
        val mut = match("+")
        val pure = match("-")
        val name = parseIdentifier { "A function declaration requires a name after `func`, as in `func name() { ... }`." }
        val ex = match("!")
        require(match("(")) { error(
            "Expected opening parenthesis.",
            "A function declaration requires parentheses `()` after the name, as in `func name() { ... }` and `func name(x, y, z) { ... }`."
        )}
        val params = mutableListOf<RhovasAst.Parameter>()
        while (!match(")")) {
            params.add(parseParameter())
            require(peek(")") || match(",")) { error(
                "Expected closing parenthesis or comma.",
                "A function parameter must be followed by a closing parenthesis `)` or comma `,` for additional parameters, as in `func name() { ... }` and `func name(x, y, z) { ... }`."
            )}
        }
        val ret = if (match(":")) parseType() else null
        val throws = if (match("throws")) {
            val throws = mutableListOf<RhovasAst.Type>()
            do {
                throws.add(parseType())
            } while (match(","))
            throws
        } else listOf()
        val body = parseStatement()
        context.pop()
        return RhovasAst.Mbr.Function(modifiers, op, mut, pure, name, ex, params, ret, throws, body)
    }

    private fun parseStatement(): RhovasAst.Stmt {
        require(tokens[0] != null) { error(
            "Expected statement.",
            "The parser reached the end of the input, but expected to parse a statement such as `if`, `for`, or `return`."
        )}
        return when {
            peek("{") -> parseBlockStmt()
            peek(listOf("var", "val")) && tokens[1]?.literal !in listOf(".", "[") -> parseDeclarationStmt()
            peek("if") -> parseIfStmt()
            peek("match") -> parseMatchStmt()
            peek("for") && tokens[1]?.literal != "." -> parseForStmt()
            peek("while") -> parseWhileStmt()
            peek("try") -> parseTryStmt()
            peek("with") && tokens[1]?.literal != "." -> parseWithStmt()
            peek("break") -> parseBreakStmt()
            peek("continue") -> parseContinueStmt()
            peek("throw") -> parseThrowStmt()
            peek("return") -> parseReturnStmt()
            peek("assert") -> parseAssertStmt()
            peek("require") -> parseRequireStmt()
            peek("ensure") -> parseEnsureStmt()
            else -> {
                context.push(tokens[0]!!.range)
                val stmt = if (match(RhovasTokenType.IDENTIFIER, ":")) {
                    val label = tokens[-2]!!.literal
                    val stmt = parseStatement()
                    RhovasAst.Stmt.Label(label, stmt)
                } else {
                    val expr = parseExpr()
                    if (match("=")) {
                        val value = parseExpr()
                        requireSemicolon { "An assignment statement must be followed by a semicolon `;`, as in `x = 0`." }
                        RhovasAst.Stmt.Assignment(expr, value)
                    } else {
                        requireSemicolon { "An expression statement must be followed by a semicolon `;`, as in `function();`." }
                        RhovasAst.Stmt.Expression(expr)
                    }
                }
                context.pop()
                stmt
            }
        }
    }

    private fun parseBlockStmt(): RhovasAst.Stmt.Block {
        require(match("{"))
        context.push(tokens[-1]!!.range)
        val stmts = generateSequence {
            if (!match("}")) parseStatement() else null
        }.toList()
        context.pop()
        return RhovasAst.Stmt.Block(stmts)
    }

    private fun parseDeclarationStmt(): RhovasAst.Stmt.Declaration {
        require(match(listOf("var", "val")))
        context.push(tokens[-1]!!.range)
        val mut = tokens[-1]!!.literal == "var"
        val name = parseIdentifier { "A variable declaration requires a name following var/val, as in `var x;` and `val y = 0`." }
        val type = if (match(":")) parseType() else null
        val expr = if (match("=")) parseExpr() else null
        requireSemicolon { "A variable declaration must be followed by a semicolon `;`, as in `var x;` and `val y = 0;`." }
        context.pop()
        return RhovasAst.Stmt.Declaration(mut, name, type, expr)
    }

    private fun parseIfStmt(): RhovasAst.Stmt.If {
        require(match("if"))
        context.push(tokens[-1]!!.range)
        require(match("(")) { error(
            "Expected opening parenthesis.",
            "The condition of an if statement must be surrounded by parentheses `()`, as in `if (cond) { ... }`."
        )}
        val cond = parseExpr()
        require(match(")")) { error(
            "Expected closing parenthesis.",
            "The condition of an if statement must be surrounded by parentheses `()`, as in `if (cond) { ... }`."
        )}
        val ifStmt = parseStatement()
        val elseStmt = if (match("else")) parseStatement() else null
        context.pop()
        return RhovasAst.Stmt.If(cond, ifStmt, elseStmt)
    }

    private fun parseMatchStmt(): RhovasAst.Stmt.Match {
        require(match("match"))
        context.push(tokens[-1]!!.range)
        val args = mutableListOf<RhovasAst.Expr>()
        if (match("(")) {
            while (!match(")")) {
                args.add(parseExpr())
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
        val cases = mutableListOf<RhovasAst.Stmt.Match.Case>()
        while (!match("}")) {
            val patterns = mutableListOf<RhovasAst.Stmt.Match.Pattern>()
            do {
                patterns.add(when {
                    match("else") -> RhovasAst.Stmt.Match.Pattern.Else(if (!peek(":")) parseMatchPattern() else null)
                    args.isEmpty() -> RhovasAst.Stmt.Match.Pattern.Expression(parseExpr())
                    else -> parseMatchPattern()
                })
                context.push(tokens[-1]!!.range)
            } while (match(","))
            require(match(":")) { error(
                "Expected colon.",
                "The condition of a match case must be followed by a colon `:`, as in `match { cond: ... }`."
            )}
            val stmt = parseStatement()
            repeat(patterns.size) { context.pop() }
            cases.add(RhovasAst.Stmt.Match.Case(patterns, stmt))
        }
        context.pop()
        return RhovasAst.Stmt.Match(args, cases)
    }

    private fun parseMatchPattern(): RhovasAst.Stmt.Match.Pattern {
        return when {
            match(RhovasTokenType.IDENTIFIER) -> RhovasAst.Stmt.Match.Pattern.Variable(tokens[-1]!!.literal)
            match("[") -> {
                val elmts = mutableListOf<RhovasAst.Stmt.Match.Pattern>()
                var rest: String? = null
                while (!match("]")) {
                    val pattern = parseMatchPattern()
                    if (match(".", ".")) {
                        require(pattern is RhovasAst.Stmt.Match.Pattern.Variable) { error(
                            "Invalid sequence target.",
                            "A list sequence pattern must be applied to a variable, as in `[head, tail..]`."
                        )}
                        rest = (pattern as RhovasAst.Stmt.Match.Pattern.Variable).name
                        require(peek("]")) { error(
                            "Expected closing square bracket.",
                            "A list sequence pattern must be in the tail position of the list, as in `[head, tail..]`."
                        )}
                    } else {
                        elmts.add(pattern)
                        require(peek("]") || match(",")) { error(
                            "Expected closing square bracket or comma.",
                            "A list pattern argument must be followed by a closing square bracket `]` or comma `,` for additional arguments, as in `[head, tail..]`."
                        )}
                    }

                }
                RhovasAst.Stmt.Match.Pattern.List(elmts, rest)
            }
            match("{") -> {
                val elmts = mutableMapOf<String, RhovasAst.Stmt.Match.Pattern>()
                var rest: String? = null
                while (!match("}")) {
                    val name = parseIdentifier { "A map entry pattern must start with a name, as in `{x, y = 5, rest..}`." }
                    elmts[name] = when {
                        match("=") -> parseMatchPattern()
                        match(".", ".") -> {
                            rest = name
                            require(peek("}")) { error(
                                "Expected closing brace.",
                                "A sequence pattern must be in the tail position of the map, as in `{x, y = 5, rest..}`."
                            )}
                            continue
                        }
                        else -> RhovasAst.Stmt.Match.Pattern.Variable(name)
                    }
                    require(peek("}") || match(",")) { error(
                        "Expected closing brace or comma.",
                        "A map pattern argument must be followed by a closing brace `}` or comma `,` for additional arguments, as in `{x, y = 5, rest..}`."
                    )}
                }
                RhovasAst.Stmt.Match.Pattern.Map(elmts, rest)
            }
            else -> RhovasAst.Stmt.Match.Pattern.Expression(parseExpr())
        }
    }

    private fun parseForStmt(): RhovasAst.Stmt.For {
        require(match("for"))
        context.push(tokens[-1]!!.range)
        require(match("(")) { error(
            "Expected opening parenthesis.",
            "The variable of a for statement must be surrounded by parentheses `()`, as in `for (elem in list) { ... }`."
        )}
        val name = if (match(RhovasTokenType.IDENTIFIER, "in")) tokens[-2]!!.literal else null
        val expr = parseExpr()
        require(match(")")) { error(
            "Expected closing parenthesis.",
            "The variable of a for statement must be surrounded by parentheses `()`, as in `for (element in array) { ... }`."
        )}
        val body = parseStatement()
        context.pop()
        return RhovasAst.Stmt.For(name, expr, body)
    }

    private fun parseWhileStmt(): RhovasAst.Stmt.While {
        require(match("while"))
        context.push(tokens[-1]!!.range)
        require(match("(")) { error(
            "Expected opening parenthesis.",
            "The condition of a while statement must be surrounded by parentheses `()`, as in `while (cond) { ... }`."
        )}
        val cond = parseExpr()
        require(match(")")) { error(
            "Expected closing parenthesis.",
            "The condition of a while statement must be surrounded by parentheses `()`, as in `while (cond) { ... }`."
        )}
        val body = parseStatement()
        context.pop()
        return RhovasAst.Stmt.While(cond, body)
    }

    private fun parseTryStmt(): RhovasAst.Stmt.Try {
        require(match("try"))
        context.push(tokens[-1]!!.range)
        val body = parseStatement()
        context.pop()
        val catches = generateSequence {
            if (match("catch")) {
                context.push(tokens[-1]!!.range)
                require(match("(")) { error(
                    "Expected opening parenthesis.",
                    "The condition of a while statement must be surrounded by parentheses `()`, as in `while (cond) { ... }`."
                )}
                val name = parseIdentifier { "The variable of a catch block requires a name, as in `try { ... } catch (name: Type) { ... }`." }
                require(match(":")) { error(
                    "Expected colon.",
                    "The variable name of a catch block must be followed by a colon `:`, as in `try { ... } catch (name: Type) { ... }`."
                )}
                val type = parseType()
                require(match(")")) { error(
                    "Expected closing parenthesis.",
                    "The condition of a while statement must be surrounded by parentheses `()`, as in `while (cond) { ... }`."
                )}
                val body = parseStatement()
                context.pop()
                RhovasAst.Stmt.Try.Catch(name, type, body)
            } else null
        }.toList()
        val finally = if (match("finally")) {
            context.push(tokens[-1]!!.range)
            val stmt = parseStatement()
            context.pop()
            stmt
        } else null
        return RhovasAst.Stmt.Try(body, catches, finally)
    }

    private fun parseWithStmt(): RhovasAst.Stmt.With {
        require(match("with"))
        context.push(tokens[-1]!!.range)
        require(match("(")) { error(
            "Expected opening parenthesis.",
            "The variable of a with statement must be surrounded by parentheses `()`, as in `with (name = expr) { ... }`."
        )}
        val name = if (match(RhovasTokenType.IDENTIFIER, "=")) tokens[-2]!!.literal else null
        val expr = parseExpr()
        require(match(")")) { error(
            "Expected closing parenthesis.",
            "The variable of a with statement must be surrounded by parentheses `()`, as in `with (name = expr) { ... }`."
        )}
        val body = parseStatement()
        context.pop()
        return RhovasAst.Stmt.With(name, expr, body)
    }

    private fun parseBreakStmt(): RhovasAst.Stmt.Break {
        require(match("break"))
        val label = if (match("@")) parseIdentifier { "A break statement with a label uses the form break @label, as in `break @loop;`." } else null
        requireSemicolon { "A break statement must be followed by a semicolon `;`, as in `break;` and `break @label;`." }
        return RhovasAst.Stmt.Break(label)
    }

    private fun parseContinueStmt(): RhovasAst.Stmt.Continue {
        require(match("continue"))
        val label = if (match("@")) parseIdentifier { "A continue statement with a label uses the form continue @label, as in `continue @loop;`." } else null
        requireSemicolon { "A continue statement must be followed by a semicolon `;`, as in `continue;` and `continue @label;`." }
        return RhovasAst.Stmt.Continue(label)
    }

    private fun parseThrowStmt(): RhovasAst.Stmt.Throw {
        require(match("throw"))
        context.push(tokens[-1]!!.range)
        val value = parseExpr()
        requireSemicolon { "A throw statement must be followed by a semicolon `;`, as in `throw Exception();`." }
        context.pop()
        return RhovasAst.Stmt.Throw(value)
    }

    private fun parseReturnStmt(): RhovasAst.Stmt.Return {
        require(match("return"))
        context.push(tokens[-1]!!.range)
        val value = if (!peek(";")) parseExpr() else null
        requireSemicolon { "A return statement must be followed by a semicolon `;`, as in `return 0;`." }
        context.pop()
        return RhovasAst.Stmt.Return(value)
    }

    private fun parseAssertStmt(): RhovasAst.Stmt.Assert {
        require(match("assert"))
        context.push(tokens[-1]!!.range)
        val expr = parseExpr()
        requireSemicolon { "An assert statement must be followed by a semicolon `;`, as in `assert expr;`." }
        context.pop()
        return RhovasAst.Stmt.Assert(expr)
    }

    private fun parseRequireStmt(): RhovasAst.Stmt.Require {
        require(match("require"))
        context.push(tokens[-1]!!.range)
        val expr = parseExpr()
        requireSemicolon { "A require statement must be followed by a semicolon `;`, as in `require expr;`." }
        context.pop()
        return RhovasAst.Stmt.Require(expr)
    }

    private fun parseEnsureStmt(): RhovasAst.Stmt.Ensure {
        require(match("ensure"))
        context.push(tokens[-1]!!.range)
        val expr = parseExpr()
        requireSemicolon { "An ensure statement must be followed by a semicolon `;`, as in `ensure expr;`." }
        context.pop()
        return RhovasAst.Stmt.Ensure(expr)
    }

    private fun parseExpr(): RhovasAst.Expr {
        return parseLogicalOrExpr()
    }

    private fun parseLogicalOrExpr(): RhovasAst.Expr {
        return parseBinaryExpr(::parseLogicalAndExpr, "||")
    }

    private fun parseLogicalAndExpr(): RhovasAst.Expr {
        return parseBinaryExpr(::parseComparisonExpr, "&&")
    }

    private fun parseComparisonExpr(): RhovasAst.Expr {
        return parseBinaryExpr(::parseAdditiveExpr, "<", "<=", ">", ">=", "==", "===", "!=", "!==")
    }

    private fun parseAdditiveExpr(): RhovasAst.Expr {
        return parseBinaryExpr(::parseMultiplicativeExpr, "+", "-")
    }

    private fun parseMultiplicativeExpr(): RhovasAst.Expr {
        return parseBinaryExpr(::parseUnaryExpr, "*", "/")
    }

    private fun parseBinaryExpr(parser: () -> RhovasAst.Expr, vararg ops: String): RhovasAst.Expr {
        var expr = parser()
        context.push(tokens[-1]!!.range)
        while (true) {
            val op = ops.sorted().lastOrNull { o ->
                match(*o.toCharArray().map { it.toString() }.toTypedArray())
            } ?: break
            val right = parser()
            context.pop()
            expr = RhovasAst.Expr.Binary(expr, op, right)
            context.push(tokens[-1]!!.range)
        }
        context.pop()
        return expr
    }

    private fun parseUnaryExpr(): RhovasAst.Expr {
        if (match(listOf("+", "-", "!"))) {
            val op = tokens[-1]!!.literal
            val expr = parseUnaryExpr()
            return RhovasAst.Expr.Unary(op, expr)
        }
        return parseSecondaryExpr()
    }

    private fun parseSecondaryExpr(): RhovasAst.Expr {
        var expr = parsePrimaryExpr()
        repeat(2) { context.push(tokens[-1]!!.range) }
        while (true) {
            val token = tokens[0]
            expr = when {
                match(".") -> {
                    val name = parseIdentifier { "A field access or method call requires a name, as in `x.y` or `x.z()`." }
                    if (peek(listOf("(", "{")) || peek("!", listOf("(", "{"))) parseFunctionExpr(expr, name) else RhovasAst.Expr.Access(expr, name)
                }
                match("[") -> {
                    val args = mutableListOf<RhovasAst.Expr>()
                    while (!match("]")) {
                        args.add(parseExpr())
                        require(peek("]") || match(",")) { error(
                            "Expected closing square bracket or comma.",
                            "A function argument must be followed by a closing square bracket `]` or comma `,` for additional arguments, as in `array[0]` and `pixels[x, y]`."
                        )}
                    }
                    RhovasAst.Expr.Index(expr, args)
                }
                else -> break
            }
            context.pop()
            context.push(token!!.range)
        }
        repeat(2) { context.pop() }
        return expr
    }

    private fun parsePrimaryExpr(): RhovasAst.Expr {
        require(tokens[0] != null) { error(
            "Expected expression.",
            "The parser reached the end of the input, but expected to parse an expression such as `0`, `x`, or `f()`."
        )}
        return when {
            match("null") -> RhovasAst.Expr.Literal(null)
            match(listOf("true", "false")) -> RhovasAst.Expr.Literal(tokens[-1]!!.literal.toBoolean())
            match(RhovasTokenType.INTEGER) -> RhovasAst.Expr.Literal(tokens[-1]!!.literal.toInt())
            match(RhovasTokenType.DECIMAL) -> RhovasAst.Expr.Literal(tokens[-1]!!.literal.toDouble())
            match(RhovasTokenType.CHARACTER) -> RhovasAst.Expr.Literal(tokens[-1]!!.literal[1])
            match(RhovasTokenType.STRING) -> RhovasAst.Expr.Literal(tokens[-1]!!.literal.removeSurrounding("\""))
            match(".") -> {
                val name = parseIdentifier { "Context access requires an identifier, as in `.name`." }
                val rec = RhovasAst.Expr.Access(null, "this")
                if (peek(listOf("(", "{")) || peek("!", listOf("(", "{"))) parseFunctionExpr(rec, name) else RhovasAst.Expr.Access(rec, name)
            }
            match(":", RhovasTokenType.IDENTIFIER) -> RhovasAst.Expr.Literal(RhovasAst.Expr.Literal.Atom(tokens[-1]!!.literal))
            match("[") -> {
                val list = mutableListOf<RhovasAst.Expr>()
                while (!match("]")) {
                    list.add(parseExpr())
                    require(peek("]") || match(",")) { error(
                        "Expected closing square bracket or comma.",
                        "A list literal must be followed by a closing square bracket `]` or comma `,` for additional arguments, as in `[]` or `[x, y, z]`."
                    )}
                }
                RhovasAst.Expr.Literal(list)
            }
            match("{") -> {
                val map = mutableMapOf<String, RhovasAst.Expr>()
                while (!match("}")) {
                    val key = parseIdentifier { "An entry is a map literal must start with a key, as in `{x: 0}`." }
                    context.push(tokens[-1]!!.range)
                    require(match("=")) { error(
                        "Expected equal sign.",
                        "The value of a map entry must be followed by an equal sign `=`, as in `{x: 0}`."
                    )}
                    map[key] = parseExpr()
                    require(peek("}") || match(",")) { error(
                        "Expected closing brace or comma.",
                        "A map literal must be followed by a closing brace `}` or comma `,` for additional arguments, as in `{}` or `{x: 0, y: 1, z: 2}`."
                    )}
                    context.pop()
                }
                RhovasAst.Expr.Literal(map)
            }
            match(RhovasTokenType.IDENTIFIER) -> {
                val name = tokens[-1]!!.literal
                if (peek(listOf("(", "{")) || peek("!", listOf("(", "{"))) parseFunctionExpr(null, name) else RhovasAst.Expr.Access(null, name)
            }
            match("(") -> {
                val expr = parseExpr()
                require(match(")")) { error(
                    "Expected closing parenthesis",
                    "A group expression must be surrounded by parentheses `()`, as in `(variable)` and `(function())`."
                )}
                RhovasAst.Expr.Group(expr)
            }
            peek("#") -> parseDslExpr()
            else -> throw ParseException(error(
                "Unexpected token `${tokens[0]!!.literal}`.",
                "The parser expected to parse an expression, but received an unexpected token instead."
            ))
        }
    }

    private fun parseFunctionExpr(rec: RhovasAst.Expr?, name: String): RhovasAst.Expr.Function {
        val ex = match("!")
        require(peek(listOf("(", "{")))
        context.push(tokens[-1]!!.range)
        val args = mutableListOf<RhovasAst.Expr>()
        if (match("(")) {
            while (!match(")")) {
                args.add(parseExpr())
                require(peek(")") || match(",")) { error(
                    "Expected closing parenthesis or comma.",
                    "A function argument must be followed by a closing parenthesis `)` or comma `,` for additional arguments, as in `function()` or `function(x, y, z)`."
                )}
            }
        }
        if (match("{")) {
            val params = mutableListOf<String>()
            if (peek(RhovasTokenType.IDENTIFIER, "-", ">") || peek(RhovasTokenType.IDENTIFIER, ",")) {
                do {
                    require(match(RhovasTokenType.IDENTIFIER))
                    params.add(tokens[-1]!!.literal)
                    require(peek("-", ">") || match(",")) { error(
                        "Expected lambda arrow or comma",
                        "Lambda parameters must be followed by an arrow `->` or comma `,` for additional parameters, as in `lambda { x, y -> ... }`."
                    )}
                } while (!match("-", ">"))
            }
            val stmts = generateSequence {
                if (!match("}")) parseStatement() else null
            }.toList()
            args.add(RhovasAst.Expr.Lambda(params, RhovasAst.Stmt.Block(stmts)))
        }
        context.pop()
        return RhovasAst.Expr.Function(rec, name, ex, args)
    }

    private fun parseDslExpr(): RhovasAst.Expr.Dsl {
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
        return RhovasAst.Expr.Dsl(name, ast)
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
