package dev.willbanders.rhovas.x.interpreter.rhovas

import dev.willbanders.rhovas.x.interpreter.Interpreter
import dev.willbanders.rhovas.x.interpreter.Environment
import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst

class RhovasInterpreter(private val env: Environment) : Interpreter<RhovasAst>() {

    private var scope = Environment.Scope(env.scope)
    private var type: Environment.Type? = null

    override fun eval(ast: RhovasAst): Any? {
        return when (ast) {
            is RhovasAst.Source -> eval(ast)
            is RhovasAst.Import -> eval(ast)
            is RhovasAst.ClassCmpt -> eval(ast)
            is RhovasAst.InterfaceCmpt -> eval(ast)
            is RhovasAst.PropertyMbr -> eval(ast)
            is RhovasAst.ConstructorMbr -> eval(ast)
            is RhovasAst.FunctionMbr -> eval(ast)
            is RhovasAst.ExpressionStmt -> eval(ast)
            is RhovasAst.BlockStmt -> eval(ast)
            is RhovasAst.DeclarationStmt -> eval(ast)
            is RhovasAst.AssignmentStmt -> eval(ast)
            is RhovasAst.IfStmt -> eval(ast)
            is RhovasAst.MatchStmt -> eval(ast)
            is RhovasAst.ForStmt -> eval(ast)
            is RhovasAst.WhileStmt -> eval(ast)
            is RhovasAst.ReturnStmt -> eval(ast)
            is RhovasAst.LiteralExpr -> eval(ast)
            is RhovasAst.GroupExpr -> eval(ast)
            is RhovasAst.UnaryExpr -> eval(ast)
            is RhovasAst.BinaryExpr -> eval(ast)
            is RhovasAst.AccessExpr -> eval(ast)
            is RhovasAst.FunctionExpr -> eval(ast)
            is RhovasAst.DslExpr -> eval(ast)
            else -> throw AssertionError()
        }
    }

    private fun eval(src: RhovasAst.Source) {
        src.impts.forEach { eval(it) }
        src.mbrs.forEach { eval(it) }
        src.mbrs.filterIsInstance<RhovasAst.PropertyMbr>().forEach { prop ->
            scope.lookupVariable(prop.name)!!.value = prop.value?.let { eval(it) }
        }
        val main = src.mbrs.find {it is RhovasAst.FunctionMbr && it.name == "main" }
        if (main != null) {
            eval((main as RhovasAst.FunctionMbr).body)
        }
    }

    private fun eval(impt: RhovasAst.Import) {
        TODO()
    }

    private fun eval(cmpt: RhovasAst.ClassCmpt) {
        env.defType(cmpt.name, scope) { t ->
            val current = type
            type = t
            cmpt.mbrs.forEach { eval(it) }
            type = current
        }
    }

    private fun eval(cmpt: RhovasAst.InterfaceCmpt) {
        TODO()
    }

    private fun eval(mbr: RhovasAst.PropertyMbr) {
        if (type != null) {
            type!!.instance.defineVariable(mbr.name, null)
        } else {
            scope.defineVariable(mbr.name, null)
        }
    }

    private fun eval(mbr: RhovasAst.ConstructorMbr) {
        val closure = type!!
        scope.defineFunction(closure.name, mbr.params.size) { args ->
            val instance = Environment.Object(closure, Environment.Scope(closure.instance))
            instance.scope.defineVariable("this", instance)
            closure.instance.variables.forEach { instance.scope.defineVariable(it.key, it.value.value) }
            scoped(Environment.Scope(instance.scope)) {
                args.indices.forEach { scope.defineVariable(mbr.params[it], args[it]) }
                try {
                    eval(mbr.body)
                } catch (ignored: Return) {}
            }
            instance
        }
    }

    private fun eval(mbr: RhovasAst.FunctionMbr) {
        if (type != null) {
            type!!.instance.defineFunction(mbr.name, mbr.params.size) { args ->
                val instance = args[0] as Environment.Object
                scoped(Environment.Scope(instance.scope)) {
                    mbr.params.indices.forEach { scope.defineVariable(mbr.params[it], args[it + 1]) }
                    try {
                        eval(mbr.body)
                    } catch (e: Return) {
                        e.value
                    }
                }
            }
        } else {
            val closure = scope
            closure.defineFunction(mbr.name, mbr.params.size) { args ->
                scoped(Environment.Scope(closure)) {
                    mbr.params.indices.forEach { scope.defineVariable(mbr.params[it], args[it]) }
                    try {
                        eval(mbr.body)
                    } catch (e: Return) {
                        e.value
                    }
                }
            }
        }
    }

    private fun eval(ast: RhovasAst.ExpressionStmt) {
        eval(ast.expr)
    }

    private fun eval(ast: RhovasAst.BlockStmt) {
        scoped(Environment.Scope(scope)) {
            ast.stmts.forEach { eval(it) }
        }
    }

    private fun eval(ast: RhovasAst.DeclarationStmt) {
        scope.defineVariable(ast.name, ast.value?.let { eval(it) })
    }

    private fun eval(ast: RhovasAst.AssignmentStmt) {
        if (ast.rec is RhovasAst.AccessExpr) {
            if (ast.rec.rec == null) {
                val variable = scope.lookupVariable(ast.rec.name) ?: throw Exception("Undefined variable ${ast.rec.name}.")
                variable.value = eval(ast.value)
            } else {
                val rec = eval(ast.rec.rec)
                if (rec is Environment.Object) {
                    val variable = rec.scope.lookupVariable(ast.rec.name) ?: throw Exception("Undefined variable ${ast.rec.name} on receiver of type ${rec.type.name}.")
                    variable.value = eval(ast.value)
                } else {
                    TODO("Assignment on builtin object?")
                }
            }
        } else {
            throw Exception("Assignment receiver must be an access expression.")
        }
    }

    private fun eval(ast: RhovasAst.IfStmt) {
        when (val cond = eval(ast.cond)) {
            true -> eval(ast.ifStmt)
            false -> ast.elseStmt?.let { eval(it) }
            else -> throw Exception("If condition must evaluate to a Boolean, received $cond.")
        }
    }

    private fun eval(ast: RhovasAst.MatchStmt) {
        val args = ast.args.map { eval(it) }
        val case = when (args.size) {
            0 -> ast.cases.firstOrNull { c ->
                c.first.any { it is RhovasAst.AccessExpr && it.rec == null && it.name == "else" || eval(it) == true }
            } ?: return
            1 -> ast.cases.firstOrNull { c ->
                c.first.any { it is RhovasAst.AccessExpr && it.rec == null && it.name == "else" || eval(it) == args[0] }
            } ?: throw Exception("Structural match must cover all cases or have an `else` case (missed ${args[0]})")
            else -> TODO("Semantics for match with multiple arguments.")
        }
        eval(case.second)
    }

    private fun eval(ast: RhovasAst.ForStmt) {
        when (val expr = eval(ast.expr)) {
            is Iterable<*> -> scoped(Environment.Scope(scope)) { expr.forEach {
                scope.defineVariable(ast.name, it)
                eval(ast.body)
            } }
            else -> throw Exception("For loop expression must evalute to an Iterable, received $expr.")
        }
    }

    private fun eval(ast: RhovasAst.WhileStmt) {
        loop@while (true) {
            when (val cond = eval(ast.cond)) {
                true -> eval(ast.body)
                false -> break@loop
                else -> throw Exception("If condition must evaluate to a Boolean, received $cond.")
            }
        }
    }

    private fun eval(ast: RhovasAst.ReturnStmt) {
        throw Return(eval(ast.value))
    }

    private fun eval(ast: RhovasAst.LiteralExpr): Any? {
        return ast.obj
    }

    private fun eval(ast: RhovasAst.GroupExpr): Any? {
        return eval(ast.expr)
    }

    private fun eval(ast: RhovasAst.UnaryExpr): Any? {
        val value = eval(ast.expr)
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

    private fun eval(ast: RhovasAst.BinaryExpr): Any? {
        val left = eval(ast.left)
        val right = { eval(ast.right) }
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

    private fun eval(ast: RhovasAst.AccessExpr): Any? {
        return when (val rec = ast.rec?.let { eval(it) }) {
            null -> scope.lookupVariable(ast.name)?.value
                ?: throw Exception("Undefined variable ${ast.name}.")
            is Environment.Object -> rec.scope.lookupVariable(ast.name)?.value
                ?: throw Exception("Undefined variable ${ast.name}.")
            else -> TODO("Access with receiver.")
        }
    }

    private fun eval(ast: RhovasAst.FunctionExpr): Any? {
        return when (val rec = ast.rec?.let { eval(it) }) {
            null -> scope.lookupFunction(ast.name, ast.args.size)?.invoke?.invoke(ast.args.map { eval(it) })
                ?: throw Exception("Undefined function ${ast.name}/${ast.args.size}.")
            is Environment.Object -> {
                val func = rec.scope.lookupFunction(ast.name, ast.args.size)
                    ?: throw Exception("Undefined function ${ast.name}/${ast.args.size}.")
                val args = listOf(rec) + ast.args.map { eval(it) }
                scoped(rec.scope) { func.invoke(args) }
            }
            else -> TODO("Function with receiver.")
        }
    }

    private fun eval(ast: RhovasAst.DslExpr): Any? {
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
