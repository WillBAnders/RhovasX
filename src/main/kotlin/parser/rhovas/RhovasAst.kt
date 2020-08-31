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
        val mut: Mutability,
        val name: String,
        val generics: List<Type>,
    ) : RhovasAst() {

        enum class Mutability { MUTABLE, IMMUTABLE, VIEWABLE }

    }

    data class Parameter(
        val name: String,
        val type: Type,
    ) : RhovasAst()

    data class Modifiers(
        val visibility: Visibility?,
        val virtual: Boolean,
        val abstract: Boolean,
        val override: Boolean,
    ) : RhovasAst() {

        enum class Visibility { PUBLIC, PACKAGE, PROTECTED, PRIVATE }

    }

    sealed class Mbr : RhovasAst() {

        sealed class Cmpt : Mbr() {

            data class Class(
                val modifiers: Modifiers,
                val type: Type,
                val extends: List<Type>,
                val mbrs: List<Mbr>,
            ) : Cmpt()

            data class Interface(
                val modifiers: Modifiers,
                val type: Type,
                val extends: List<Type>,
                val mbrs: List<Mbr>,
            ) : Cmpt()

        }

        data class Property(
            val modifiers: Modifiers,
            val mut: Boolean,
            val name: String,
            val type: Type?,
            val value: Expr?,
        ) : Mbr()

        data class Constructor(
            val modifiers: Modifiers,
            val ex: Boolean,
            val params: List<Parameter>,
            val throws: List<Type>,
            val body: Stmt,
        ) : Mbr()

        data class Function(
            val modifiers: Modifiers,
            val op: String?,
            val mut: Boolean,
            val pure: Boolean,
            val name: String,
            val ex: Boolean,
            val params: List<Parameter>,
            val ret: Type?,
            val throws: List<Type>,
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
            val cases: List<Case>,
        ) : Stmt() {

            data class Case(
                val patterns: List<Pattern>,
                val stmt: Stmt,
            ) : RhovasAst()

            sealed class Pattern : RhovasAst() {

                data class Expression(
                    val expr: Expr,
                ) : Pattern()

                data class Variable(
                    val name: String?,
                ) : Pattern()

                data class List(
                    val elmts: kotlin.collections.List<Pattern>,
                    val rest: String?,
                ) : Pattern()

                data class Map(
                    val elmts: kotlin.collections.Map<String, Pattern>,
                    val rest: String?,
                ) : Pattern()

                data class Else(
                    val pattern: Pattern?
                ) : Pattern()

            }

        }

        data class For(
            val name: String?,
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

        data class With(
            val name: String?,
            val expr: Expr,
            val body: Stmt,
        ) : Stmt()

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

        data class Assert(
            val cond: Expr,
        ) : Stmt()

        data class Require(
            val cond: Expr,
        ) : Stmt()

        data class Ensure(
            val cond: Expr,
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
            val ex: Boolean,
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
                is Modifiers -> visit(ast)
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
                is Stmt.Match.Case -> visit(ast)
                is Stmt.Match.Pattern.Expression -> visit(ast)
                is Stmt.Match.Pattern.Variable -> visit(ast)
                is Stmt.Match.Pattern.List -> visit(ast)
                is Stmt.Match.Pattern.Map -> visit(ast)
                is Stmt.Match.Pattern.Else -> visit(ast)
                is Stmt.For -> visit(ast)
                is Stmt.While -> visit(ast)
                is Stmt.Try -> visit(ast)
                is Stmt.With -> visit(ast)
                is Stmt.Break -> visit(ast)
                is Stmt.Continue -> visit(ast)
                is Stmt.Throw -> visit(ast)
                is Stmt.Return -> visit(ast)
                is Stmt.Assert -> visit(ast)
                is Stmt.Require -> visit(ast)
                is Stmt.Ensure -> visit(ast)
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

        protected open fun visit(ast: Source): T {
            TODO()
        }

        protected open fun visit(ast: Import): T {
            TODO()
        }

        protected open fun visit(ast: Type): T {
            TODO()
        }

        protected open fun visit(ast: Parameter): T {
            TODO()
        }

        protected open fun visit(ast: Modifiers): T {
            TODO()
        }

        protected open fun visit(ast: Mbr.Cmpt.Class): T {
            TODO()
        }

        protected open fun visit(ast: Mbr.Cmpt.Interface): T {
            TODO()
        }

        protected open fun visit(ast: Mbr.Property): T {
            TODO()
        }

        protected open fun visit(ast: Mbr.Constructor): T {
            TODO()
        }

        protected open fun visit(ast: Mbr.Function): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Expression): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Block): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Label): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Declaration): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Assignment): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.If): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Match): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Match.Case): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Match.Pattern.Expression): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Match.Pattern.Variable): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Match.Pattern.List): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Match.Pattern.Map): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Match.Pattern.Else): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.For): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.While): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Try): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.With): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Break): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Continue): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Throw): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Return): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Assert): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Require): T {
            TODO()
        }

        protected open fun visit(ast: Stmt.Ensure): T {
            TODO()
        }

        protected open fun visit(ast: Expr.Literal): T {
            TODO()
        }

        protected open fun visit(ast: Expr.Group): T {
            TODO()
        }

        protected open fun visit(ast: Expr.Unary): T {
            TODO()
        }

        protected open fun visit(ast: Expr.Binary): T {
            TODO()
        }

        protected open fun visit(ast: Expr.Access): T {
            TODO()
        }

        protected open fun visit(ast: Expr.Index): T {
            TODO()
        }

        protected open fun visit(ast: Expr.Function): T {
            TODO()
        }

        protected open fun visit(ast: Expr.Lambda): T {
            TODO()
        }

        protected open fun visit(ast: Expr.Dsl): T {
            TODO()
        }

    }

}
