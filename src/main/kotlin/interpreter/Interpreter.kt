package dev.willbanders.rhovas.x.interpreter

import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst.*

object Interpreter : Visitor<Any?>() {

    var scope: Scope? = null
    private var label: String? = null
    private var match: Environment.Object? = null

    override fun visit(ast: Stmt.Expression) {
        visit(ast.expr)
    }

    override fun visit(ast: Stmt.Block) {
        scoped(Scope(scope)) {
            ast.stmts.forEach { visit(it) }
        }
    }

    override fun visit(ast: Stmt.Label): Any? {
        val current = label
        label = ast.label
        try {
            return visit(ast.stmt)
        } finally {
            label = current
        }
    }

    override fun visit(ast: Stmt.Declaration) {
        scope!!.vars[ast.name] = Environment.Variable(ast.name, ast.value
            ?.let { visit(it) as Environment.Object }
            ?: ENV.init("Null", null))
    }

    override fun visit(ast: Stmt.Assignment) {
        if (ast.rec is Expr.Access) {
            if (ast.rec.rec == null) {
                val variable = scope!!.reqVar(ast.rec.name)
                variable.value = visit(ast.value) as Environment.Object
            } else {
                val rec = visit(ast.rec.rec) as Environment.Object
                val prop = rec.reqProp(ast.rec.name)
                prop.set(listOf(rec, visit(ast.value) as Environment.Object))
            }
        } else if (ast.rec is Expr.Index) {
            val rec = visit(ast.rec.rec) as Environment.Object
            val args = ast.rec.args + listOf(ast.value)
            val method = rec.reqMthd("[]=", args.size)
            method.invoke(listOf(rec) + args.map { visit(it) as Environment.Object })
        } else {
            throw Exception("Assignment receiver must be an access expression or index expression.")
        }
    }

    override fun visit(ast: Stmt.If) {
        val cond = visit(ast.cond) as Environment.Object
        when (cond.value) {
            true -> visit(ast.ifStmt)
            false -> ast.elseStmt?.let { visit(it) }
            else -> throw Exception("If condition must evaluate to a Boolean, received ${cond.type}.")
        }
    }

    override fun visit(ast: Stmt.Match) {
        val match = this.match
        val args = ast.args.map { visit(it) as Environment.Object }
        val case = when (args.size) {
            0 -> ast.cases.firstOrNull { it.patterns.any { (visit((it as Stmt.Match.Pattern.Expression).expr) as Environment.Object).value as Boolean } } ?: return
            1 -> {
                this.match = args[0]
                ast.cases.firstOrNull { it.patterns.any { visit(it) as Boolean } }
                    ?: throw Exception("Structural match must cover all cases or have an `else` case (missed ${args[0].reqMthd("toString", 0).invoke(listOf(args[0])).value})")
            }
            else -> TODO("Semantics for match with multiple arguments.")
        }
        scoped(Scope(scope)) {
            if (args.isNotEmpty()) {
                val value = if (args.size == 1) args[0] else ENV.init("List", args)
                scope!!.vars["match"] = Environment.Variable("match", ENV.init("KeywordVariable", value))
            }
            visit(case.stmt)
        }
        this.match = match
    }

    override fun visit(ast: Stmt.Match.Case): Boolean {
        return ast.patterns.any { visit(it) as Boolean }
    }

    override fun visit(ast: Stmt.Match.Pattern.Expression): Boolean {
        return match!!.reqMthd("==", 1).invoke(listOf(match!!, visit(ast.expr) as Environment.Object)).value == true
    }

    override fun visit(ast: Stmt.Match.Pattern.Variable) : Boolean {
        ast.name?.let { scope!!.vars[it] = Environment.Variable(it, match!!) }
        return true
    }

    override fun visit(ast: Stmt.Match.Pattern.List): Boolean {
        val match = this.match
        val list = match?.value as? List<Environment.Object> ?: if (match?.type?.isSubtypeOf(ENV.reqType("Struct")) == true) {
            val fields = match!!.reqProp("fields").get(listOf(match!!))
            fields.reqProp("values").get(listOf(fields)).value as List<Environment.Object>
        } else return false
        if (list.size < ast.elmts.size || list.size > ast.elmts.size && ast.rest == null) return false
        val res = ast.elmts.withIndex().all { pattern ->
            list.getOrNull(pattern.index)?.let {
                this.match = it
                visit(pattern.value) as Boolean
            } == true
        }
        if (res && ast.rest != null) {
            scope!!.vars[ast.rest] = Environment.Variable(ast.rest, ENV.init("List", list.subList(ast.elmts.size, list.size)))
        }
        this.match = match
        return res
    }

    override fun visit(ast: Stmt.Match.Pattern.Map): Boolean {
        val match = this.match!!
        if (!(match.type.flds.keys - ast.elmts.keys).isEmpty() && ast.rest == null) return false
        val res = ast.elmts.all { pattern ->
            match.type.getProp(pattern.key)?.let {
                this.match = it.get(listOf(match))
                visit(pattern.value) as Boolean
            } == true
        }
        if (res && ast.rest != null) {
            scope!!.vars[ast.rest] = Environment.Variable(ast.rest, ENV.init("Map", (match.type.flds.keys - ast.elmts.keys).map { Pair(it, match.reqProp(it).get(listOf(match))) }.toMap()))
        }
        this.match = match
        return res
    }

    override fun visit(ast: Stmt.Match.Pattern.Else): Any? {
        return ast.pattern?.let { visit(it) as Boolean } ?: true
    }

    override fun visit(ast: Stmt.For) {
        //TODO: Iterators
        val label = this.label
        for (value in (visit(ast.expr) as Environment.Object).value as Iterable<Environment.Object>) {
            try {
                scoped(Scope(scope)) {
                    if (ast.name != null) {
                        scope!!.vars[ast.name] = Environment.Variable(ast.name, value)
                    } else {
                        scope!!.vars["for"] = Environment.Variable("for", ENV.init("KeywordVariable", value))
                    }
                    visit(ast.body)
                }
            } catch (e: Break) {
                if (e.label != null && e.label != label) {
                    throw e
                }
                break
            } catch (e: Continue) {
                if (e.label != null && e.label != label) {
                    throw e
                }
                continue
            }
        }
    }

    override fun visit(ast: Stmt.While) {
        val label = this.label
        while (true) {
            val cond = visit(ast.cond) as Environment.Object
            when (cond.value) {
                true -> {
                    try {
                        visit(ast.body)
                    } catch (e: Break) {
                        if (e.label != null && e.label != label) {
                            throw e
                        }
                        break
                    } catch (e: Continue) {
                        if (e.label != null && e.label != label) {
                            throw e
                        }
                        continue
                    }
                }
                false -> break
                else -> throw Exception("While condition must evaluate to a Boolean, received ${cond.type}.")
            }
        }
    }

    override fun visit(ast: Stmt.Try) {
        try {
            visit(ast.body)
        } catch (e: Throw) {
            val catch = ast.catches.find {
                e.value.type.isSubtypeOf(scope!!.reqType(it.type.name))
            } ?: throw e
            scoped(Scope(scope)) {
                scope!!.vars[catch.name] = Environment.Variable(catch.name, e.value)
                visit(catch.body)
            }
        } finally {
            ast.finally?.let { visit(it) }
        }
    }

    override fun visit(ast: Stmt.With) {
        val expr = visit(ast.expr) as Environment.Object
        try {
            scoped(Scope(scope)) {
                if (ast.name != null) {
                    scope!!.vars[ast.name] = Environment.Variable(ast.name, expr)
                } else {
                    scope!!.vars["for"] = Environment.Variable("for", ENV.init("KeywordVariable", expr))
                }
                visit(ast.body)
            }
        } finally {
            //TODO autocloseable
        }
    }

    override fun visit(ast: Stmt.Break) {
        throw Break(ast.label)
    }

    override fun visit(ast: Stmt.Continue) {
        throw Continue(ast.label)
    }

    override fun visit(ast: Stmt.Throw) {
        throw Throw(visit(ast.value) as Environment.Object)
    }

    override fun visit(ast: Stmt.Return) {
        throw Return(ast.value?.let { visit(it) as Environment.Object })
    }

    override fun visit(ast: Stmt.Assert) {
        if (!((visit(ast.cond) as Environment.Object).value as Boolean)) {
            throw Exception("Assert failure: " + ast.cond)
        }
    }

    override fun visit(ast: Stmt.Require) {
        if (!((visit(ast.cond) as Environment.Object).value as Boolean)) {
            throw Exception("Require failure: " + ast.cond)
        }
    }

    override fun visit(ast: Stmt.Ensure) {
        if (!((visit(ast.cond) as Environment.Object).value as Boolean)) {
            throw Exception("Ensure failure: " + ast.cond)
        }
    }

    override fun visit(ast: Expr.Literal): Environment.Object {
        return when(ast.literal) {
            null -> ENV.init("Null", null)
            is Boolean -> ENV.init("Boolean", ast.literal)
            is Int -> ENV.init("Integer", ast.literal)
            is Double -> ENV.init("Decimal", ast.literal)
            is Char -> ENV.init("Character", ast.literal)
            is String -> ENV.init("String", ast.literal)
            is Expr.Literal.Atom -> ENV.init("Atom", ast.literal.name)
            is List<*> -> ENV.init("List", (ast.literal as List<Expr>).map { visit(it) })
            is Map<*, *> -> ENV.init("Map", (ast.literal as Map<String, Expr>).mapValues { visit(it.value) })
            else -> throw AssertionError()
        }
    }

    override fun visit(ast: Expr.Group): Environment.Object {
        return visit(ast.expr) as Environment.Object
    }

    override fun visit(ast: Expr.Unary): Environment.Object {
        val value = visit(ast.expr) as Environment.Object
        return value.reqMthd(ast.op, 0).invoke(listOf(value))
    }

    override fun visit(ast: Expr.Binary): Any? {
        val left = (visit(ast.left)) as Environment.Object
        val right = { (visit(ast.right) as Environment.Object) }
        return when (ast.op) {
            "&&", "||" -> {
                val l = left.value as Boolean
                val r = { right().value as Boolean }
                ENV.init("Boolean", if (ast.op == "&&") l && r() else l || r())
            }
            "<", "<=", ">", ">=" -> {
                val compare = left.reqMthd("compare", 1).invoke(listOf(left, right()))
                ENV.init("Boolean", when (ast.op) {
                    "<" -> compare.value == "lt"
                    "<=" -> compare.value == "lt" || compare.value == "eq"
                    ">" -> compare.value == "gt"
                    ">=" -> compare.value == "gt" || compare.value == "eq"
                    else -> throw AssertionError()
                })
            }
            "==", "!=" -> {
                val eq = left.reqMthd("==", 1).invoke(listOf(left, right())).value as Boolean
                ENV.init("Boolean", if (ast.op == "==") eq else !eq)
            }
            "===", "!==" -> {
                val r = right()
                val eq = left === r || left.type.name == r.type.name && left.value === r.value
                ENV.init("Boolean", if (ast.op == "===") eq else !eq)
            }
            else -> left.reqMthd(ast.op, 1).invoke(listOf(left, right()))
        }
    }

    override fun visit(ast: Expr.Access): Environment.Object {
        return if (ast.rec == null) {
            scope!!.reqVar(ast.name).value
        } else {
            val rec = visit(ast.rec) as Environment.Object
            rec.reqProp(ast.name).get(listOf(rec))
        }
    }

    override fun visit(ast: Expr.Index): Environment.Object {
        val rec = visit(ast.rec) as Environment.Object
        return rec.reqMthd("[]", ast.args.size).invoke(listOf(rec) + ast.args.map { visit(it) as Environment.Object })
    }

    override fun visit(ast: Expr.Function): Environment.Object {
        return if (ast.rec == null) {
            scope!!.reqFunc(ast.name, ast.args.size).invoke(ast.args.map { visit(it) as Environment.Object })
        } else {
            val rec = visit(ast.rec) as Environment.Object
            rec.reqMthd(ast.name, ast.args.size).invoke(listOf(rec) + ast.args.map { visit(it) as Environment.Object })
        }
    }

    override fun visit(ast: Expr.Lambda): Environment.Object {
        val closure = scope
        return ENV.init("Lambda", Environment.Function("invoke", 1) { args ->
            scoped(Scope(closure)) {
                ast.params.withIndex().forEach {
                    scope!!.vars[it.value] = Environment.Variable(it.value, args[it.index])
                }
                if (ast.params.isEmpty() && args.isNotEmpty()) {
                    val value = if (args.size == 1) args[0] else ENV.init("List", args)
                    scope!!.vars["val"] = Environment.Variable("val", value)
                }
                try {
                    visit(ast.body)
                    ENV.init("Null", null)
                } catch (e: Return) {
                    e.value ?: ENV.init("Null", null)
                }
            }
        })
    }

    override fun visit(ast: Expr.Dsl): Any? {
        TODO()
    }

    fun <T> scoped(scope: Scope, block: () -> T): T {
        val current = this.scope
        this.scope = scope
        try {
            return block()
        } finally {
            this.scope = current
        }
    }

    fun <T> eval(scope: Scope, block: Interpreter.() -> T): T {
        val label = this.label
        val match = this.match
        try {
            return scoped(scope) { block(this) }
        } finally {
            this.label = label
            this.match = match
        }
    }

    data class Break(val label: String?) : Exception()

    data class Continue(val label: String?) : Exception()

    data class Throw(val value: Environment.Object) : Exception()

    data class Return(val value: Environment.Object?) : Exception()

}
