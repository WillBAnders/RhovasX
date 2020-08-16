package dev.willbanders.rhovas.x.interpreter.rhovas

import dev.willbanders.rhovas.x.interpreter.Interpreter
import dev.willbanders.rhovas.x.interpreter.Scope
import dev.willbanders.rhovas.x.parser.rhovas.RhovasAst
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*

class RhovasInterpreter : Interpreter<RhovasAst>() {

    private var scope = Scope()

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
            scope.variables[prop.name]!!.value = prop.value?.let { eval(it) }
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
        TODO()
    }

    private fun eval(cmpt: RhovasAst.InterfaceCmpt) {
        TODO()
    }

    private fun eval(mbr: RhovasAst.PropertyMbr) {
        scope.variables[mbr.name] = Scope.Variable(
            mbr.mut,
            mbr.name,
            null
        )
    }

    private fun eval(mbr: RhovasAst.ConstructorMbr) {
        TODO()
    }

    private fun eval(mbr: RhovasAst.FunctionMbr) {
        scope.functions[Pair(mbr.name, mbr.params.size)] = Scope.Function(mbr.name, mbr.params.size) {
            scope = Scope(scope)
            for (i in it.indices) {
                scope.variables[mbr.params[i]] = Scope.Variable(
                    false,
                    mbr.params[i],
                    it[i]
                )
            }
            val res = try {
                eval(mbr.body)
            } catch (e: Return) {
                e.value
            }
            scope = scope.parent!!
            res
        }
    }

    private fun eval(ast: RhovasAst.ExpressionStmt) {
        eval(ast.expr)
    }

    private fun eval(ast: RhovasAst.BlockStmt) {
        scope = Scope(scope)
        ast.stmts.forEach { eval(it) }
        scope = scope.parent!!
    }

    private fun eval(ast: RhovasAst.DeclarationStmt) {
        scope.variables[ast.name] = Scope.Variable(
            ast.mut,
            ast.name,
            ast.value?.let { eval(it) }
        )
    }

    private fun eval(ast: RhovasAst.AssignmentStmt) {
        if (ast.rec is RhovasAst.AccessExpr) {
            if (ast.rec.rec == null) {
                scope.getVariable(ast.rec.name).value = eval(ast.value)
            } else {
                val rec = eval(ast.rec.rec) ?: "Cannot access field ${ast.rec.name} on null"
                val prop = rec::class.declaredMemberProperties
                    .find { it.name == ast.rec.name }
                    ?: throw Exception("Undefined field ${ast.rec.name} on receiver $rec.")
                (prop as KMutableProperty1<Any, Any?>).set(rec, eval(ast.value))
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
        scope = Scope(scope)
        when (val expr = eval(ast.expr)) {
            is Iterable<*> -> expr.forEach {
                scope.variables[ast.name] = Scope.Variable(false, ast.name, it)
                eval(ast.body)
            }
            else -> throw Exception("For loop expression must evalute to an Iterable, received $expr.")
        }
        scope = scope.parent!!
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
            null -> scope.getVariable(ast.name).value
            else -> {
                val prop = rec::class.declaredMemberProperties
                    .find { it.name == ast.name }
                    ?: throw Exception("Undefined field ${ast.name} on receiver $rec.")
                (prop as KProperty1<Any, Any?>).get(rec)
            }
        }
    }

    private fun eval(ast: RhovasAst.FunctionExpr): Any? {
        return when (val rec = ast.rec?.let { eval(it) }) {
            null -> {
                val func = scope.getFunction(ast.name, ast.args.size)
                func.apply(ast.args.map { eval(it) })
            }
            else -> {
                val func = rec::class.declaredFunctions
                    .find { it.name == ast.name && (it.parameters.size == ast.args.size || it.instanceParameter != null && it.parameters.size == ast.args.size + 1) }
                if (func != null) {
                    if (func.instanceParameter != null) {
                        func.call()
                    } else {
                        func.call(*((listOf(rec) + ast.args.map { eval(it) }).toTypedArray()))
                    }
                } else {
                    val method = rec.javaClass.declaredMethods
                        .find { it.name == ast.name && it.parameters.size == ast.args.size }
                        ?: throw Exception("Undefined function ${ast.name} on receiver $rec.")
                    method.invoke(rec, *(ast.args.map { eval(it) }.toTypedArray()))
                }
            }
        }
    }

    private fun eval(ast: RhovasAst.DslExpr): Any? {
        TODO()
    }

    data class Return(val value: Any?) : Exception()

    init {
        scope.functions[Pair("print", 1)] = Scope.Function("print", 1) {
            println(it[0])
        }
    }

}
