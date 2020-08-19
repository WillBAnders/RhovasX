package dev.willbanders.rhovas.x.interpreter

import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst
import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst.*

class Interpreter(private val env: Environment) : Visitor<Any?>() {

    var scope = Environment.Scope(env.scope)
    private var type: Environment.Type? = null

    override fun visit(ast: Source) {
        ast.impts.forEach { visit(it) }
        ast.mbrs.forEach { visit(it) }
        ast.mbrs.filterIsInstance<Mbr.Property>().forEach { prop ->
            scope.lookupVariable(prop.name)!!.value = prop.value?.let { visit(it) }
        }
        val main = ast.mbrs.find {it is Mbr.Function && it.name == "main" }
        if (main != null) {
            visit((main as Mbr.Function).body)
        }
    }

    override fun visit(ast: Import) {
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
        if (type != null) {
            type!!.instance.defineVariable(ast.name, null)
        } else {
            scope.defineVariable(ast.name, null)
        }
    }

    override fun visit(ast: Mbr.Constructor) {
        val closure = type!!
        scope.defineFunction(closure.name, ast.params.size) { args ->
            val instance = Environment.Object(closure, Environment.Scope(closure.instance))
            instance.scope.defineVariable("this", instance)
            closure.instance.variables.forEach { instance.scope.defineVariable(it.key, it.value.value) }
            scoped(Environment.Scope(instance.scope)) {
                args.indices.forEach { scope.defineVariable(ast.params[it], args[it]) }
                try {
                    visit(ast.body)
                } catch (ignored: Return) {}
            }
            instance
        }
    }

    override fun visit(ast: Mbr.Function) {
        if (type != null) {
            type!!.instance.defineFunction(ast.name, ast.params.size) { args ->
                val instance = args[0] as Environment.Object
                scoped(Environment.Scope(instance.scope)) {
                    ast.params.indices.forEach { scope.defineVariable(ast.params[it], args[it + 1]) }
                    try {
                        visit(ast.body)
                    } catch (e: Return) {
                        e.value
                    }
                }
            }
        } else {
            val closure = scope
            closure.defineFunction(ast.name, ast.params.size) { args ->
                scoped(Environment.Scope(closure)) {
                    ast.params.indices.forEach { scope.defineVariable(ast.params[it], args[it]) }
                    try {
                        visit(ast.body)
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
        scope.defineVariable(ast.name, ast.value?.let { visit(it) })
    }

    override fun visit(ast: Stmt.Assignment) {
        if (ast.rec is Expr.Access) {
            if (ast.rec.rec == null) {
                val variable = scope.lookupVariable(ast.rec.name) ?: throw Exception("Undefined variable ${ast.rec.name}.")
                variable.value = visit(ast.value)
            } else {
                val rec = visit(ast.rec.rec)
                if (rec is Environment.Object) {
                    val variable = rec.scope.lookupVariable(ast.rec.name) ?: throw Exception("Undefined variable ${ast.rec.name} on receiver of type ${rec.type.name}.")
                    variable.value = visit(ast.value)
                } else {
                    TODO("Assignment on builtin object?")
                }
            }
        } else if (ast.rec is Expr.Index) {
            when (val rec = visit(ast.rec.rec)) {
                is List<*> -> {
                    if (ast.rec.args.size != 1) throw Exception("Undefined function index[]=/${ast.rec.args.size}.")
                    (rec as MutableList<Any?>)[visit(ast.rec.args[0]) as Int] = visit(ast.value)
                }
                else -> TODO("Index with receiver.")
            }
        } else {
            throw Exception("Assignment receiver must be an access expression.")
        }
    }

    override fun visit(ast: Stmt.If) {
        when (val cond = visit(ast.cond)) {
            true -> visit(ast.ifStmt)
            false -> ast.elseStmt?.let { visit(it) }
            else -> throw Exception("If condition must evaluate to a Boolean, received $cond.")
        }
    }

    override fun visit(ast: Stmt.Match) {
        val args = ast.args.map { visit(it) }
        val case = when (args.size) {
            0 -> ast.cases.firstOrNull { c ->
                c.first.any { it is Expr.Access && it.rec == null && it.name == "else" || visit(it) == true }
            } ?: return
            1 -> ast.cases.firstOrNull { c ->
                c.first.any { it is Expr.Access && it.rec == null && it.name == "else" || visit(it) == args[0] }
            } ?: throw Exception("Structural match must cover all cases or have an `else` case (missed ${args[0]})")
            else -> TODO("Semantics for match with multiple arguments.")
        }
        visit(case.second)
    }

    override fun visit(ast: Stmt.For) {
        when (val expr = visit(ast.expr)) {
            is Iterable<*> -> scoped(Environment.Scope(scope)) { expr.forEach {
                scope.defineVariable(ast.name, it)
                visit(ast.body)
            } }
            else -> throw Exception("For loop expression must evalute to an Iterable, received $expr.")
        }
    }

    override fun visit(ast: Stmt.While) {
        while (true) {
            when (val cond = visit(ast.cond)) {
                true -> visit(ast.body)
                false -> break
                else -> throw Exception("If condition must evaluate to a Boolean, received $cond.")
            }
        }
    }

    override fun visit(ast: Stmt.Return) {
        throw Return(visit(ast.value))
    }

    override fun visit(ast: Expr.Literal): Any? {
        return when (ast.literal) {
            is List<*> -> (ast.literal as List<Expr>).map { visit(it) }
            is Map<*, *> -> (ast.literal as Map<String, Expr>).mapValues { visit(it.value) }
            else -> ast.literal
        }
    }

    override fun visit(ast: Expr.Group): Any? {
        return visit(ast.expr)
    }

    override fun visit(ast: Expr.Unary): Any? {
        val value = visit(ast.expr)
        return when(ast.op) {
            "+" -> when(value) {
                is Int -> value.unaryPlus()
                is Double -> value.unaryPlus()
                else -> null
            }
            "-" -> when(value) {
                is Int -> value.unaryMinus()
                is Double -> value.unaryMinus()
                else -> null
            }
            "!" -> when(value) {
                is Boolean -> value.not()
                else -> null
            }
            else -> null
        } ?: throw Exception("Operator " + ast.op + " cannot be applied to " + value?.javaClass?.name + ".")
    }

    override fun visit(ast: Expr.Binary): Any? {
        val left = visit(ast.left)
        val right = { visit(ast.right) }
        return when(ast.op) {
            "||" -> when(left) {
                true -> true
                false -> right() as? Boolean
                else -> null
            }
            "&&" -> when(left) {
                true -> right() as? Boolean
                false -> false
                else -> null
            }
            "<" -> when(left) {
                is Int -> (right() as? Int)?.let { left < it }
                is Double -> (right() as? Double)?.let { left < it }
                else -> null
            }
            "<=" -> when(left) {
                is Int -> (right() as? Int)?.let { left <= it }
                is Double -> (right() as? Double)?.let { left <= it }
                else -> null
            }
            ">" -> when(left) {
                is Int -> (right() as? Int)?.let { left > it }
                is Double -> (right() as? Double)?.let { left > it }
                else -> null
            }
            ">=" -> when(left) {
                is Int -> (right() as? Int)?.let { left >= it }
                is Double -> (right() as? Double)?.let { left >= it }
                else -> null
            }
            "==" -> left == right()
            "!=" -> left != right()
            "===" -> left === right()
            "!==" -> left !== right()
            "+" -> when(left) {
                is Int -> (right() as? Int)?.let { left + it }
                is Double -> (right() as? Double)?.let { left + it }
                is String -> left + right()
                else -> null
            }
            "-" -> when(left) {
                is Int -> (right() as? Int)?.let { left - it }
                is Double -> (right() as? Double)?.let { left - it }
                else -> null
            }
            "*" -> when(left) {
                is Int -> (right() as? Int)?.let { left * it }
                is Double -> (right() as? Double)?.let { left * it }
                else -> null
            }
            "/" -> when(left) {
                is Int -> (right() as? Int)?.let { left / it }
                is Double -> (right() as? Double)?.let { left / it }
                else -> null
            }
            else -> null
        } ?: throw Exception("Operator " + ast.op + " cannot be applied to " + left?.javaClass?.name + " and " + right()?.javaClass?.name + ".")
    }

    override fun visit(ast: Expr.Access): Any? {
        return when (val rec = ast.rec?.let { visit(it) }) {
            null -> scope.lookupVariable(ast.name)?.value
                ?: throw Exception("Undefined variable ${ast.name}.")
            is Environment.Object -> rec.scope.lookupVariable(ast.name)?.value
                ?: throw Exception("Undefined variable ${ast.name}.")
            else -> TODO("Access with receiver.")
        }
    }

    override fun visit(ast: Expr.Index): Any? {
        return when (val rec = visit(ast.rec)) {
            is List<*> -> {
                if (ast.args.size != 1) throw Exception("Undefined function index[]/${ast.args.size}.")
                rec[visit(ast.args[0]) as Int]
            }
            else -> TODO("Index with receiver.")
        }
    }

    override fun visit(ast: Expr.Function): Any? {
        return when (val rec = ast.rec?.let { visit(it) }) {
            null -> scope.lookupFunction(ast.name, ast.args.size)?.invoke?.invoke(ast.args.map { visit(it) })
                ?: throw Exception("Undefined function ${ast.name}/${ast.args.size}.")
            is Environment.Object -> {
                val func = rec.scope.lookupFunction(ast.name, ast.args.size)
                    ?: throw Exception("Undefined function ${ast.name}/${ast.args.size}.")
                val args = listOf(rec) + ast.args.map { visit(it) }
                scoped(rec.scope) { func.invoke(args) }
            }
            else -> TODO("Function with receiver.")
        }
    }

    override fun visit(ast: Expr.Lambda): Any? {
        //TODO: Closures
        return ast
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

    data class Return(val value: Any?) : Exception()

}
