package dev.willbanders.rhovas.x.parser.rhovas

sealed class RhovasAst {

    data class Source(
        val impts: List<Import>,
        val mbrs: List<Member>,
    ) : RhovasAst()

    data class Import(
        val path: List<String>,
        val name: String?,
    ) : RhovasAst()

    open class Member : RhovasAst()

    open class Component : Member()

    data class ClassCmpt(
        val name: String,
        val mbrs: List<Member>,
    ) : Component()

    data class InterfaceCmpt(
        val name: String,
        val mbrs: List<Member>,
    ) : Component()

    data class PropertyMbr(
        val mut: Boolean,
        val name: String,
        val value: Expression?,
    ) : Member()

    data class ConstructorMbr(
        val params: List<String>,
        val body: Statement,
    ) : Member()

    data class FunctionMbr(
        val name: String,
        val params: List<String>,
        val body: Statement,
    ) : Member()

    open class Statement : RhovasAst()

    data class ExpressionStmt(
        val expr: Expression,
    ) : Statement()

    data class BlockStmt(
        val stmts: List<Statement>,
    ) : Statement()

    data class DeclarationStmt(
        val mut: Boolean,
        val name: String,
        val value: Expression?,
    ) : Statement()

    data class AssignmentStmt(
        val rec: Expression,
        val value: Expression
    ) : Statement()

    data class IfStmt(
        val cond: Expression,
        val ifStmt: Statement,
        val elseStmt: Statement?,
    ) : Statement()

    data class MatchStmt(
        val args: List<Expression>,
        val cases: List<Pair<List<Expression>, Statement>>,
    ) : Statement()

    data class ForStmt(
        val name: String,
        val expr: Expression,
        val body: Statement,
    ) : Statement()

    data class WhileStmt(
        val cond: Expression,
        val body: Statement,
    ) : Statement()

    data class ReturnStmt(
        val value: Expression,
    ) : Statement()

    open class Expression : RhovasAst()

    data class ScalarLiteralExpr(
        val obj: Any?,
    ) : Expression()

    data class AtomLiteralExpr(
        val name: String,
    ) : Expression()

    data class ListLiteralExpr(
        val list: List<Expression>,
    ) : Expression()

    data class MapLiteralExpr(
        val map: Map<String, Expression>,
    ) : Expression()

    data class GroupExpr(
        val expr: Expression,
    ) : Expression()

    data class UnaryExpr(
        val op: String,
        val expr: Expression,
    ) : Expression()

    data class BinaryExpr(
        val left: Expression,
        val op: String,
        val right: Expression,
    ) : Expression()

    data class AccessExpr(
        val rec: Expression?,
        val name: String,
    ) : Expression()

    data class FunctionExpr(
        val rec: Expression?,
        val name: String,
        val args: List<Expression>,
    ) : Expression()

    data class LambdaExpr(
        val params: List<String>,
        val body: Statement,
    ) : Expression()

    data class DslExpr(
        val name: String,
        val ast: Any?,
    ) : Expression()

}
