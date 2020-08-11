package dev.willbanders.rhovas.x.parser.rhovas

sealed class RhovasAst {

    open class Statement : RhovasAst()

    data class ExpressionStmt(
        val expr: Expression
    ) : Statement()

    data class BlockStmt(
        val stmts: List<Statement>
    ) : Statement()

    data class DeclarationStmt(
        val mut: Boolean,
        val name: String,
        val value: Expression?
    ) : Statement()

    data class AssignmentStmt(
        val rec: Expression,
        val value: Expression
    ) : Statement()

    data class IfStmt(
        val cond: Expression,
        val ifStmt: Statement,
        val elseStmt: Statement?
    ) : Statement()

    data class ForStmt(
        val name: String,
        val expr: Expression,
        val body: Statement
    ) : Statement()

    data class WhileStmt(
        val cond: Expression,
        val body: Statement
    ) : Statement()

    open class Expression : RhovasAst()

    data class LiteralExpr(
        val obj: Any?
    ) : Expression()

    data class GroupExpr(
        val expr: Expression
    ) : Expression()

    data class UnaryExpr(
        val op: String,
        val expr: Expression
    ) : Expression()

    data class BinaryExpr(
        val left: Expression,
        val op: String,
        val right: Expression
    ) : Expression()

    data class AccessExpr(
        val rec: Expression?,
        val name: String
    ) : Expression()

    data class FunctionExpr(
        val rec: Expression?,
        val name: String,
        val args: List<Expression>
    ) : Expression()

}
