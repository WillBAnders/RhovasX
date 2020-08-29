package dev.willbanders.rhovas.x.parser.rhovas

import dev.willbanders.rhovas.x.parser.embed.EmbedAst

sealed class RhovasAst {

    data class Source(
        val impts: List<Import>,
        val mbrs: List<Mbr>,
    ) : RhovasAst()

    data class Import(
        val path: List<String>,
        val name: String?,
    ) : RhovasAst()

    data class Type(
        val name: String,
        val generics: List<Type>,
    ) : RhovasAst()

    data class Parameter(
        val name: String,
        val type: Type,
    ) : RhovasAst()

    sealed class Mbr : RhovasAst() {

        sealed class Cmpt : Mbr() {

            data class Class(
                val name: String,
                val generics: List<Type>,
                val extends: List<Type>,
                val mbrs: List<Mbr>,
            ) : Cmpt()

            data class Interface(
                val name: String,
                val generics: List<Type>,
                val extends: List<Type>,
                val mbrs: List<Mbr>,
            ) : Cmpt()

        }

        data class Property(
            val mut: Boolean,
            val name: String,
            val type: Type?,
            val value: Expr?,
        ) : Mbr()

        data class Constructor(
            val params: List<Parameter>,
            val body: Stmt,
        ) : Mbr()

        data class Function(
            val op: String?,
            val name: String,
            val params: List<Parameter>,
            val ret: Type?,
            val body: Stmt,
        ) : Mbr()

    }

    sealed class Stmt : RhovasAst() {

        data class Expression(
            val expr: Expr,
        ) : Stmt()

        data class Block(
            val stmts: List<Stmt>,
        ) : Stmt()

        data class Label(
            val label: String,
            val stmt: Stmt,
        ) : Stmt()

        data class Declaration(
            val mut: Boolean,
            val name: String,
            val type: Type?,
            val value: Expr?,
        ) : Stmt()

        data class Assignment(
            val rec: Expr,
            val value: Expr
        ) : Stmt()

        data class If(
            val cond: Expr,
            val ifStmt: Stmt,
            val elseStmt: Stmt?,
        ) : Stmt()

        data class Match(
            val args: List<Expr>,
            val cases: List<Pair<List<Expr>, Stmt>>,
        ) : Stmt()

        data class For(
            val name: String,
            val expr: Expr,
            val body: Stmt,
        ) : Stmt()

        data class While(
            val cond: Expr,
            val body: Stmt,
        ) : Stmt()

        data class Try(
            val body: Stmt,
            val catches: List<Catch>,
            val finally: Stmt?,
        ) : Stmt() {

            data class Catch(
                val name: String,
                val type: Type,
                val body: Stmt,
            )

        }

        data class Break(
            val label: String?,
        ) : Stmt()

        data class Continue(
            val label: String?,
        ) : Stmt()

        data class Throw(
            val value: Expr,
        ) : Stmt()

        data class Return(
            val value: Expr?,
        ) : Stmt()

    }

    sealed class Expr : RhovasAst() {

        data class Literal(
            val literal: Any?,
        ) : Expr() {

            data class Atom(val name: String)

        }

        data class Group(
            val expr: Expr,
        ) : Expr()

        data class Unary(
            val op: String,
            val expr: Expr,
        ) : Expr()

        data class Binary(
            val left: Expr,
            val op: String,
            val right: Expr,
        ) : Expr()

        data class Access(
            val rec: Expr?,
            val name: String,
        ) : Expr()

        data class Index(
            val rec: Expr,
            val args: List<Expr>,
        ) : Expr()

        data class Function(
            val rec: Expr?,
            val name: String,
            val args: List<Expr>,
        ) : Expr()

        data class Lambda(
            val params: List<String>,
            val body: Stmt,
        ) : Expr()

        data class Dsl(
            val name: String,
            val ast: EmbedAst,
        ) : Expr()

    }

    abstract class Visitor<T> {

        fun visit(ast: RhovasAst): T {
            return when (ast) {
                is Source -> visit(ast)
                is Import -> visit(ast)
                is Type -> visit(ast)
                is Parameter -> visit(ast)
                is Mbr.Cmpt.Class -> visit(ast)
                is Mbr.Cmpt.Interface -> visit(ast)
                is Mbr.Property -> visit(ast)
                is Mbr.Constructor -> visit(ast)
                is Mbr.Function -> visit(ast)
                is Stmt.Expression -> visit(ast)
                is Stmt.Block -> visit(ast)
                is Stmt.Declaration -> visit(ast)
                is Stmt.Label -> visit(ast)
                is Stmt.Assignment -> visit(ast)
                is Stmt.If -> visit(ast)
                is Stmt.Match -> visit(ast)
                is Stmt.For -> visit(ast)
                is Stmt.While -> visit(ast)
                is Stmt.Try -> visit(ast)
                is Stmt.Break -> visit(ast)
                is Stmt.Continue -> visit(ast)
                is Stmt.Throw -> visit(ast)
                is Stmt.Return -> visit(ast)
                is Expr.Literal -> visit(ast)
                is Expr.Group -> visit(ast)
                is Expr.Unary -> visit(ast)
                is Expr.Binary -> visit(ast)
                is Expr.Access -> visit(ast)
                is Expr.Index -> visit(ast)
                is Expr.Function -> visit(ast)
                is Expr.Lambda -> visit(ast)
                is Expr.Dsl -> visit(ast)
            }
        }

        protected abstract fun visit(ast: Source): T

        protected abstract fun visit(ast: Import): T

        protected abstract fun visit(ast: Type): T

        protected abstract fun visit(ast: Parameter): T

        protected abstract fun visit(ast: Mbr.Cmpt.Class): T

        protected abstract fun visit(ast: Mbr.Cmpt.Interface): T

        protected abstract fun visit(ast: Mbr.Property): T

        protected abstract fun visit(ast: Mbr.Constructor): T

        protected abstract fun visit(ast: Mbr.Function): T

        protected abstract fun visit(ast: Stmt.Expression): T

        protected abstract fun visit(ast: Stmt.Block): T

        protected abstract fun visit(ast: Stmt.Label): T

        protected abstract fun visit(ast: Stmt.Declaration): T

        protected abstract fun visit(ast: Stmt.Assignment): T

        protected abstract fun visit(ast: Stmt.If): T

        protected abstract fun visit(ast: Stmt.Match): T

        protected abstract fun visit(ast: Stmt.For): T

        protected abstract fun visit(ast: Stmt.While): T

        protected abstract fun visit(ast: Stmt.Try): T

        protected abstract fun visit(ast: Stmt.Break): T

        protected abstract fun visit(ast: Stmt.Continue): T

        protected abstract fun visit(ast: Stmt.Throw): T

        protected abstract fun visit(ast: Stmt.Return): T

        protected abstract fun visit(ast: Expr.Literal): T

        protected abstract fun visit(ast: Expr.Group): T

        protected abstract fun visit(ast: Expr.Unary): T

        protected abstract fun visit(ast: Expr.Binary): T

        protected abstract fun visit(ast: Expr.Access): T

        protected abstract fun visit(ast: Expr.Index): T

        protected abstract fun visit(ast: Expr.Function): T

        protected abstract fun visit(ast: Expr.Lambda): T

        protected abstract fun visit(ast: Expr.Dsl): T

    }

}
