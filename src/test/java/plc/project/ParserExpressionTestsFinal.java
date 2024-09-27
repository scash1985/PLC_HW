package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserExpressionTestsFinal {

    @ParameterizedTest
    @MethodSource
    void testMissingSemicolon(String test, List<Token> tokens) {
        Assertions.assertThrows(ParseException.class, () -> {
            Parser parser = new Parser(tokens);
            parser.parseStatement();
        });
    }

    private static Stream<Arguments> testMissingSemicolon() {
        return Stream.of(
                Arguments.of("Missing Semicolon",
                        Arrays.asList(
                                //f
                                new Token(Token.Type.IDENTIFIER, "f", 0)
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInvalidExpression(String test, List<Token> tokens) {
        Assertions.assertThrows(ParseException.class, () -> {
            Parser parser = new Parser(tokens);
            parser.parseExpression();
        });
    }

    private static Stream<Arguments> testInvalidExpression() {
        return Stream.of(
                Arguments.of("Invalid Expression",
                        Arrays.asList(
                                // expr1 AND
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6)
                                // Missing second expression
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMissingClosingParenthesis(String test, List<Token> tokens) {
        Assertions.assertThrows(ParseException.class, () -> {
            Parser parser = new Parser(tokens);
            parser.parseExpression();
        });
    }

    private static Stream<Arguments> testMissingClosingParenthesis() {
        return Stream.of(
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                //(expr
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                                // Missing closing parenthesis
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testInvalidClosingParenthesis")
    void testInvalidClosingParenthesis(String description, List<Token> tokens) {
        ParseException exception = Assertions.assertThrows(ParseException.class, () -> {
            Parser parser = new Parser(tokens);
            parser.parseExpression();
        });
        Assertions.assertEquals(4, exception.getIndex()); // Ensure this matches the expected index
    }

    private static Stream<Arguments> testInvalidClosingParenthesis() {
        return Stream.of(
                Arguments.of("Invalid Closing Parenthesis",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, ")", 4) // Invalid closing parenthesis without matching opening
                        )
                )
        );
    }

    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}

