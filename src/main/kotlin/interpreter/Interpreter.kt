package dev.willbanders.rhovas.x.interpreter

import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst.*

class Interpreter(private val env: Environment) : Visitor<Any?>() {

    var scope = Environment.Scope(env.scope)
    private var type: Environment.Type? = null

    override fun visit(ast: Source) {
        ast.impts.forEach { visit(it) }
        ast.mbrs.forEach { visit(it) }
        ast.mbrs.filterIsInstance<Mbr.Property>().forEach { prop ->
            scope.getVar(prop.name)!!.value = prop.value?.let { visit(it) } as Environment.Object
        }
        val main = ast.mbrs.find {it is Mbr.Function && it.name == "main" }
        if (main != null) {
            visit((main as Mbr.Function).body)
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
        env.defType(ast.name, scope) { t ->
            val current = type
            type = t
            ast.mbrs.forEach { visit(it) }
            type = current
        }
    }

    override fun visit(ast: Mbr.Cmpt.Interface) {
        TODO()
    }

    override fun visit(ast: Mbr.Property) {
        val value = env.reqType("Null").init(null)
        if (type != null) {
            type!!.instance.defVar(ast.name, value)
        } else {
            scope.defVar(ast.name, value)
        }
    }

    override fun visit(ast: Mbr.Constructor) {
        val closure = type!!
        scope.defFunc(closure.name, ast.params.size) { args ->
            val instance = closure.init(null)
            instance.scope.defVar("this", instance)
            instance.scope.variables.putAll(closure.instance.variables)
            scoped(Environment.Scope(instance.scope)) {
                args.indices.forEach { scope.defVar(ast.params[it].name, args[it]) }
                try {
                    visit(ast.body)
                } catch (ignored: Return) {}
            }
            instance
        }
    }

    override fun visit(ast: Mbr.Function) {
        if (type != null) {
            type!!.instance.defFunc(ast.name, ast.params.size) { args ->
                val instance = args[0]
                scoped(Environment.Scope(instance.scope)) {
                    ast.params.indices.forEach { scope.defVar(ast.params[it].name, args[it + 1]) }
                    try {
                        visit(ast.body)
                        env.reqType("Null").init(null)
                    } catch (e: Return) {
                        e.value
                    }
                }
            }
        } else {
            val closure = scope
            closure.defFunc(ast.name, ast.params.size) { args ->
                scoped(Environment.Scope(closure)) {
                    ast.params.indices.forEach { scope.defVar(ast.params[it].name, args[it]) }
                    try {
                        visit(ast.body)
                        env.reqType("Null").init(null)
                    } catch (e: Return) {
                        e.value
                    }
                }
            }
        }
    }

    override fun visit(ast: Stmt.Expression) {
        visit(ast.expr)
    }

    override fun visit(ast: Stmt.Block) {
        scoped(Environment.Scope(scope)) {
            ast.stmts.forEach { visit(it) }
        }
    }

    override fun visit(ast: Stmt.Declaration) {
        scope.defVar(ast.name, ast.value
            ?.let { visit(it) as Environment.Object }
            ?: env.reqType("Null").init(null))
    }

    override fun visit(ast: Stmt.Assignment) {
        if (ast.rec is Expr.Access) {
            if (ast.rec.rec == null) {
                val variable = scope.reqVar(ast.rec.name)
                variable.value = visit(ast.value) as Environment.Object
            } else {
                val rec = visit(ast.rec.rec) as Environment.Object
                val field = rec.reqProp(ast.rec.name)
                field.value = visit(ast.value) as Environment.Object
            }
        } else if (ast.rec is Expr.Index) {
            val rec = visit(ast.rec.rec) as Environment.Object
            val args = ast.rec.args + listOf(ast.value)
            val method = rec.reqMthd("[]=", args.size)
            method.invoke(args.map { visit(it) as Environment.Object })
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
                c.first.any { it is Expr.Access && it.rec == null && it.name == "else" || method.invoke(listOf(visit(it) as Environment.Object)).value == true }
            } ?: throw Exception("Structural match must cover all cases or have an `else` case (missed ${args[0]})")
            else -> TODO("Semantics for match with multiple arguments.")
        }
        visit(case.second)
    }

    override fun visit(ast: Stmt.For) {
        (visit(ast.expr) as Environment.Object)
            .reqMthd("iterate", 1)
            .invoke(listOf(visit(Expr.Lambda(listOf(ast.name), ast.body))))
    }

    override fun visit(ast: Stmt.While) {
        while (true) {
            val cond = visit(ast.cond) as Environment.Object
            when (cond.value) {
                true -> visit(ast.body)
                false -> break
                else -> throw Exception("While condition must evaluate to a Boolean, received ${cond.type}.")
            }
        }
    }

    override fun visit(ast: Stmt.Return) {
        throw Return(visit(ast.value) as Environment.Object)
    }

    override fun visit(ast: Expr.Literal): Environment.Object {
        return when(ast.literal) {
            null -> env.reqType("Null").init(null)
            is Boolean -> env.reqType("Boolean").init(ast.literal)
            is Int -> env.reqType("Integer").init(ast.literal)
            is Double -> env.reqType("Decimal").init(ast.literal)
            is Char -> env.reqType("Character").init(ast.literal)
            is String -> env.reqType("String").init(ast.literal)
            is Expr.Literal.Atom -> env.reqType("Atom").init(ast.literal.name)
            is List<*> -> env.reqType("List").init((ast.literal as List<Expr>).map { visit(it) })
            is Map<*, *> -> env.reqType("Map").init((ast.literal as Map<String, Expr>).mapValues { visit(it.value) })
            else -> throw AssertionError()
        }
    }

    override fun visit(ast: Expr.Group): Environment.Object {
        return visit(ast.expr) as Environment.Object
    }

    override fun visit(ast: Expr.Unary): Environment.Object {
        val value = visit(ast.expr) as Environment.Object
        return value.reqMthd(ast.op, 0).invoke(listOf())
    }

    override fun visit(ast: Expr.Binary): Any? {
        val left = (visit(ast.left)) as Environment.Object
        val right = { (visit(ast.right) as Environment.Object) }
        return when (ast.op) {
            "&&", "||" -> {
                val l = left.value as Boolean
                val r = { right().value as Boolean }
                env.reqType("Boolean").init(if (ast.op == "&&") l && r() else l || r())
            }
            "<", "<=", ">", ">=" -> {
                val compare = left.reqMthd("compare", 1).invoke(listOf(right()))
                env.reqType("Boolean").init(when (ast.op) {
                    "<" -> compare.value == "lt"
                    "<=" -> compare.value == "lt" || compare.value == "eq"
                    ">" -> compare.value == "gt"
                    ">=" -> compare.value == "gt" || compare.value == "eq"
                    else -> throw AssertionError()
                })
            }
            "==", "!=" -> {
                val eq = left.reqMthd("==", 1).invoke(listOf(right())).value as Boolean
                env.reqType("Boolean").init(if (ast.op == "==") eq else !eq)
            }
            "===", "!==" -> {
                val r = right()
                val eq = left === r || left.type.name == r.type.name && left.value === r.value
                env.reqType("Boolean").init(if (ast.op == "===") eq else !eq)
            }
            else -> left.reqMthd(ast.op, 1).invoke(listOf(right()))
        }
    }

    override fun visit(ast: Expr.Access): Environment.Object {
        return if (ast.rec == null) {
            scope.reqVar(ast.name).value
        } else {
            (visit(ast.rec) as Environment.Object).reqProp(ast.name).value
        }
    }

    override fun visit(ast: Expr.Index): Environment.Object {
        return (visit(ast.rec) as Environment.Object)
            .reqMthd("[]", ast.args.size)
            .invoke(ast.args.map { visit(it) as Environment.Object })
    }

    override fun visit(ast: Expr.Function): Environment.Object {
        val function = if (ast.rec == null) {
            scope.reqFunc(ast.name, ast.args.size)
        } else {
            (visit(ast.rec) as Environment.Object).reqMthd(ast.name, ast.args.size)
        }
        return function.invoke(ast.args.map { visit(it) as Environment.Object })
    }

    override fun visit(ast: Expr.Lambda): Environment.Object {
        val closure = scope
        return env.reqType("Lambda").init(Environment.Function("invoke", 1) { args ->
            scoped(Environment.Scope(closure)) {
                ast.params.withIndex().forEach { scope.defVar(it.value, args[it.index]) }
                try {
                    visit(ast.body)
                    env.reqType("Null").init(null)
                } catch (e: Return) {
                    e.value
                }
            }
        })
    }

    override fun visit(ast: Expr.Dsl): Any? {
        TODO()
    }

    private fun <T> scoped(scope: Environment.Scope, block: () -> T): T {
        val current = this.scope
        this.scope = scope
        val result = block()
        this.scope = current
        return result
    }

    data class Return(val value: Environment.Object) : Exception()

}
