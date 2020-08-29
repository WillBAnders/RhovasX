package dev.willbanders.rhovas.x.interpreter

import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst.*

class Interpreter(private val env: Environment) : Visitor<Any?>() {

    var scope = Scope(null).also { it.funcs.putAll(env.reqType("Kernel").funcs) }
    private var type: Environment.Type? = null
    private var label: String? = null

    override fun visit(ast: Source) {
        ast.impts.forEach { visit(it) }
        ast.mbrs.forEach { visit(it) }
        ast.mbrs.filterIsInstance<Mbr.Property>().forEach { prop ->
            scope.reqVar(prop.name).value = prop.value?.let { visit(it) } as Environment.Object
        }
        val main = ast.mbrs.find {it is Mbr.Function && it.name == "main" }
        if (main != null) {
            try {
                visit((main as Mbr.Function).body)
            } catch (e: Throw) {
                println("Uncaught exception: " + e.value.reqMthd("toString", 0).invoke(listOf(e.value)).value)
            }
        }
    }

    override fun visit(ast: Import) {
        TODO()
    }

    override fun visit(ast: Type): Any? {
        TODO()
    }

    override fun visit(ast: Parameter): Any? {
        TODO()
    }

    override fun visit(ast: Mbr.Cmpt.Class) {
        env.defType(ast.name) { t ->
            val current = type
            type = t
            ast.mbrs.forEach { visit(it) }
            ast.mbrs.filterIsInstance<Mbr.Property>().forEach { prop ->
                //TODO: Move to constructor
                t.flds[prop.name]!!.value = prop.value
                    ?.let { visit(it) as Environment.Object }
                    ?: env.init("Null", null)
            }
            type = current
        }
    }

    override fun visit(ast: Mbr.Cmpt.Interface) {
        TODO()
    }

    override fun visit(ast: Mbr.Property) {
        val value = env.init("Null", null)
        if (type != null) {
            type!!.flds[ast.name] = Environment.Variable(ast.name, value)
            type!!.props[ast.name] = Environment.Property(ast.name,
                { (it[0].value as Scope).reqVar(ast.name).value },
                { (it[0].value as Scope).reqVar(ast.name).value = it[1] }
            )
        } else {
            scope.vars[ast.name] = Environment.Variable(ast.name, value)
        }
    }

    override fun visit(ast: Mbr.Constructor) {
        val type = this.type!!
        val closure = scope
        val func = Environment.Function(type.name, ast.params.size) { args ->
            scoped(Scope(closure)) {
                val obj = Environment.Object(type, scope)
                scope.vars["this"] = Environment.Variable("this", obj)
                type.flds.values.forEach {
                    scope.vars[it.name] = Environment.Variable(it.name, it.value)
                }
                scoped(Scope(scope)) {
                    ast.params.withIndex().forEach {
                        scope.vars[it.value.name] = Environment.Variable(it.value.name, args[it.index])
                    }
                    try {
                        visit(ast.body)
                    } catch (ignored: Return) {}
                }
                obj
            }
        }
        scope.funcs[Pair(func.name, func.arity)] = func
    }

    override fun visit(ast: Mbr.Function) {
        if (type != null) {
            type!!.mthds[Pair(ast.name, ast.params.size)] = Environment.Function(ast.name, ast.params.size) { args ->
                val instance = args[0]
                scoped(Scope(instance.value as Scope)) {
                    ast.params.withIndex().forEach {
                        scope.vars[it.value.name] = Environment.Variable(it.value.name, args[it.index + 1])
                    }
                    try {
                        visit(ast.body)
                        env.init("Null", null)
                    } catch (e: Return) {
                        e.value ?: env.init("Null", null)
                    }
                }
            }
        } else {
            val closure = scope
            val func = Environment.Function(ast.name, ast.params.size) { args ->
                scoped(Scope(closure)) {
                    ast.params.withIndex().forEach {
                        scope.vars[it.value.name] = Environment.Variable(it.value.name, args[it.index])
                    }
                    try {
                        visit(ast.body)
                        env.init("Null", null)
                    } catch (e: Return) {
                        e.value ?: env.init("Null", null)
                    }
                }
            }
            scope.funcs[Pair(func.name, func.arity)] = func
        }
    }

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
        scope.vars[ast.name] = Environment.Variable(ast.name, ast.value
            ?.let { visit(it) as Environment.Object }
            ?: env.init("Null", null))
    }

    override fun visit(ast: Stmt.Assignment) {
        if (ast.rec is Expr.Access) {
            if (ast.rec.rec == null) {
                val variable = scope.reqVar(ast.rec.name)
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
        val args = ast.args.map { visit(it) }
        val case = when (args.size) {
            0 -> ast.cases.firstOrNull { c ->
                c.first.any { it is Expr.Access && it.rec == null && it.name == "else" || (visit(it) as Environment.Object).value == true }
            } ?: return
            1 -> ast.cases.firstOrNull { c ->
                val arg = visit(ast.args[0]) as Environment.Object
                val method = arg.reqMthd("==", 1)
                c.first.any { it is Expr.Access && it.rec == null && it.name == "else" || method.invoke(listOf(arg, visit(it) as Environment.Object)).value == true }
            } ?: throw Exception("Structural match must cover all cases or have an `else` case (missed ${args[0]})")
            else -> TODO("Semantics for match with multiple arguments.")
        }
        visit(case.second)
    }

    override fun visit(ast: Stmt.For) {
        //TODO: Iterators
        val label = this.label
        for (value in (visit(ast.expr) as Environment.Object).value as Iterable<Environment.Object>) {
            try {
                scoped(Scope(scope)) {
                    scope.vars[ast.name] = Environment.Variable(ast.name, value)
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
                e.value.type.isSubtypeOf(env.reqType(it.type.name))
            } ?: throw e
            scoped(Scope(scope)) {
                scope.vars[catch.name] = Environment.Variable(catch.name, e.value)
                visit(catch.body)
            }
        } finally {
            ast.finally?.let { visit(it) }
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

    override fun visit(ast: Expr.Literal): Environment.Object {
        return when(ast.literal) {
            null -> env.init("Null", null)
            is Boolean -> env.init("Boolean", ast.literal)
            is Int -> env.init("Integer", ast.literal)
            is Double -> env.init("Decimal", ast.literal)
            is Char -> env.init("Character", ast.literal)
            is String -> env.init("String", ast.literal)
            is Expr.Literal.Atom -> env.init("Atom", ast.literal.name)
            is List<*> -> env.init("List", (ast.literal as List<Expr>).map { visit(it) })
            is Map<*, *> -> env.init("Map", (ast.literal as Map<String, Expr>).mapValues { visit(it.value) })
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
                env.init("Boolean", if (ast.op == "&&") l && r() else l || r())
            }
            "<", "<=", ">", ">=" -> {
                val compare = left.reqMthd("compare", 1).invoke(listOf(left, right()))
                env.init("Boolean", when (ast.op) {
                    "<" -> compare.value == "lt"
                    "<=" -> compare.value == "lt" || compare.value == "eq"
                    ">" -> compare.value == "gt"
                    ">=" -> compare.value == "gt" || compare.value == "eq"
                    else -> throw AssertionError()
                })
            }
            "==", "!=" -> {
                val eq = left.reqMthd("==", 1).invoke(listOf(left, right())).value as Boolean
                env.init("Boolean", if (ast.op == "==") eq else !eq)
            }
            "===", "!==" -> {
                val r = right()
                val eq = left === r || left.type.name == r.type.name && left.value === r.value
                env.init("Boolean", if (ast.op == "===") eq else !eq)
            }
            else -> left.reqMthd(ast.op, 1).invoke(listOf(left, right()))
        }
    }

    override fun visit(ast: Expr.Access): Environment.Object {
        return if (ast.rec == null) {
            scope.reqVar(ast.name).value
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
            scope.reqFunc(ast.name, ast.args.size).invoke(ast.args.map { visit(it) as Environment.Object })
        } else {
            val rec = visit(ast.rec) as Environment.Object
            rec.reqMthd(ast.name, ast.args.size).invoke(listOf(rec) + ast.args.map { visit(it) as Environment.Object })
        }
    }

    override fun visit(ast: Expr.Lambda): Environment.Object {
        val closure = scope
        return env.init("Lambda", Environment.Function("invoke", 1) { args ->
            scoped(Scope(closure)) {
                ast.params.withIndex().forEach {
                    scope.vars[it.value] = Environment.Variable(it.value, args[it.index])
                }
                try {
                    visit(ast.body)
                    env.init("Null", null)
                } catch (e: Return) {
                    e.value ?: env.init("Null", null)
                }
            }
        })
    }

    override fun visit(ast: Expr.Dsl): Any? {
        TODO()
    }

    private fun <T> scoped(scope: Scope, block: () -> T): T {
        val current = this.scope
        this.scope = scope
        try {
            return block()
        } finally {
            this.scope = current
        }
    }

    data class Break(val label: String?) : Exception()

    data class Continue(val label: String?) : Exception()

    data class Throw(val value: Environment.Object) : Exception()

    data class Return(val value: Environment.Object?) : Exception()

}
