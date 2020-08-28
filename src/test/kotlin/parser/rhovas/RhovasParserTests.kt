package dev.willbanders.rhovas.x.parser.rhovas

import dev.willbanders.rhovas.x.parser.Diagnostic
import dev.willbanders.rhovas.x.parser.ParseException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RhovasParserTests {

    @ParameterizedTest
    @MethodSource
    fun test(name: String, input: String) {
        try {
            RhovasParser(input).parse()
        } catch (e: ParseException) {
            Assertions.fail(Diagnostic(name, input, e.error).toString())
        }
    }

    fun test(): Stream<Arguments> {
        return Files.walk(Paths.get("src/main/resources"))
            .filter { it.fileName.toString().endsWith(".rho") }
            .map { Arguments.of(it.fileName.toString(), it.toFile().readText()) }
    }

}
