package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Decimal", "123.456", false),
                Arguments.of("Signed Decimal", "-1.0", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Integer", "1", false),
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "<=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                ))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWhitespace(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testWhitespace() {
        return Stream.of(
                Arguments.of("Space", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9) // Updated to include the semicolon
                )),
                Arguments.of("Tab", "LET\tx\t=\t5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9) // Updated to include the semicolon
                )),
                Arguments.of("Backspace", "LET\u0008x\u0008=\u00085;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9) // Updated to include the semicolon
                ))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperatorWhitespace(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testOperatorWhitespace() {
        return Stream.of(
                Arguments.of("Operator with Space", "= ", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "=", 0)
                )),
                Arguments.of("Operator with Tab", "=\t", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "=", 0)
                )),
                Arguments.of("Operator with Backspace", "=\u0008", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "=", 0)
                )),
                Arguments.of("Operator Comparison", "<=", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "<=", 0)
                )),
                Arguments.of("Operator Parenthesis", "(", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "(", 0)))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWhitespaceHandling(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    @Test
    void testFooExample() {
        // Input source string
        String source = "LET i = -1;\nLET inc = 2;\nDEF foo() DO\n    WHILE i <= 1 DO\n"
                + "        IF i > 0 DO\n            print(\"bar\");\n        END\n"
                + "        i = i + inc;\n    END\nEND";

        // Expected tokens list
        List<Token> expectedTokens = Arrays.asList(
                //LET i = -1;
                new Token(Token.Type.IDENTIFIER, "LET", 0),
                new Token(Token.Type.IDENTIFIER, "i", 4),
                new Token(Token.Type.OPERATOR, "=", 6),
                new Token(Token.Type.INTEGER, "-1", 8),
                new Token(Token.Type.OPERATOR, ";", 10),

                //LET inc = 2;
                new Token(Token.Type.IDENTIFIER, "LET", 12),
                new Token(Token.Type.IDENTIFIER, "inc", 16),
                new Token(Token.Type.OPERATOR, "=", 20),
                new Token(Token.Type.INTEGER, "2", 22),
                new Token(Token.Type.OPERATOR, ";", 23),

                //DEF foo() DO
                new Token(Token.Type.IDENTIFIER, "DEF", 25),
                new Token(Token.Type.IDENTIFIER, "foo", 29),
                new Token(Token.Type.OPERATOR, "(", 32),
                new Token(Token.Type.OPERATOR, ")", 33),
                new Token(Token.Type.IDENTIFIER, "DO", 35),

                // WHILE i <= 1 DO
                new Token(Token.Type.IDENTIFIER, "WHILE", 42),
                new Token(Token.Type.IDENTIFIER, "i", 48),
                new Token(Token.Type.OPERATOR, "<=", 50),
                new Token(Token.Type.INTEGER, "1", 53),
                new Token(Token.Type.IDENTIFIER, "DO", 55),

                // IF i > 0 DO
                new Token(Token.Type.IDENTIFIER, "IF", 66),
                new Token(Token.Type.IDENTIFIER, "i", 69),
                new Token(Token.Type.OPERATOR, ">", 71),
                new Token(Token.Type.INTEGER, "0", 73),
                new Token(Token.Type.IDENTIFIER, "DO", 75),

                // print("bar");
                new Token(Token.Type.IDENTIFIER, "print", 90),
                new Token(Token.Type.OPERATOR, "(", 95),
                new Token(Token.Type.STRING, "\"bar\"", 96),
                new Token(Token.Type.OPERATOR, ")", 101),
                new Token(Token.Type.OPERATOR, ";", 102),

                // END
                new Token(Token.Type.IDENTIFIER, "END", 112),

                // i = i + inc;
                new Token(Token.Type.IDENTIFIER, "i", 124),
                new Token(Token.Type.OPERATOR, "=", 126),
                new Token(Token.Type.IDENTIFIER, "i", 128),
                new Token(Token.Type.OPERATOR, "+", 130),
                new Token(Token.Type.IDENTIFIER, "inc", 132),
                new Token(Token.Type.OPERATOR, ";", 135),

                // END
                new Token(Token.Type.IDENTIFIER, "END", 141),

                // END
                new Token(Token.Type.IDENTIFIER, "END", 145)
        );

        // Lex the input source
        Lexer lexer = new Lexer(source);
        List<Token> actualTokens = lexer.lex();

        // Assert that the generated tokens match the expected tokens
        Assertions.assertEquals(expectedTokens, actualTokens);
    }

}
