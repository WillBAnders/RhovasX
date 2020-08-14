package dev.willbanders.rhovas.x.parser.rhovas

import dev.willbanders.rhovas.x.parser.Token
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RhovasLexerTests {

    @ParameterizedTest
    @MethodSource
    fun testIdentifier(test: String, input: String) {
        test(input, RhovasTokenType.IDENTIFIER)
    }

    fun testIdentifier(): Stream<Arguments> {
        return Stream.of(
            Arguments.of("Lowercase", "abc"),
            Arguments.of("Uppercase", "ABC"),
            Arguments.of("Underscores", "_abc_"),
            Arguments.of("Hyphens", "a-b-c")
        )
    }

    @ParameterizedTest
    @MethodSource
    fun testInteger(test: String, input: String) {
        test(input, RhovasTokenType.INTEGER)
    }

    fun testInteger(): Stream<Arguments> {
        return Stream.of(
            Arguments.of("Single Digit", "1"),
            Arguments.of("Multiple Digits", "123"),
            Arguments.of("Leading Zero", "007")
        )
    }

    @ParameterizedTest
    @MethodSource
    fun testDecimal(test: String, input: String) {
        test(input, RhovasTokenType.DECIMAL)
    }

    fun testDecimal(): Stream<Arguments> {
        return Stream.of(
            Arguments.of("Single Digit", "1.0"),
            Arguments.of("Multiple Digits", "123.456"),
            Arguments.of("External Zeros", "007.700")
        )
    }

    @ParameterizedTest
    @MethodSource
    fun testCharacter(test: String, input: String) {
        test(input, RhovasTokenType.CHARACTER)
    }

    fun testCharacter(): Stream<Arguments> {
        return Stream.of(
            Arguments.of("Single Character", "\'c\'")
        )
    }

    @ParameterizedTest
    @MethodSource
    fun testString(test: String, input: String) {
        test(input, RhovasTokenType.STRING)
    }

    fun testString(): Stream<Arguments> {
        return Stream.of(
            Arguments.of("Empty", "\"\""),
            Arguments.of("Single Character", "\"c\""),
            Arguments.of("Multiple Characters", "\"abc\"")
        )
    }

    @ParameterizedTest
    @MethodSource
    fun testOperator(test: String, input: String) {
        test(input, input.toCharArray().withIndex().map {
            Token(RhovasTokenType.OPERATOR, it.value.toString(), it.index)
        })
    }

    fun testOperator(): Stream<Arguments> {
        return Stream.of(
            Arguments.of("Separator", ",.:;"),
            Arguments.of("Nesting", "{}()[]<>"),
            Arguments.of("Binary", "+-*/!=&|")
        )
    }

    private fun test(input: String, expected: RhovasTokenType) {
        test(input, listOf(Token(expected, input, 0)))
    }

    private fun test(input: String, expected: List<Token<RhovasTokenType>>) {
        val tokens = RhovasLexer(input).lex()
        Assertions.assertEquals(expected, tokens)
    }

}