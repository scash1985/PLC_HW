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
final class ParserExpressionTests {

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Stmt.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Stmt.Expression(new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList()))
                ),

                Arguments.of("Variable Expression",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, ";", 5) // Assuming semicolon is necessary
                        ),
                        new Ast.Stmt.Expression(new Ast.Expr.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Function Expression",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "func", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6) // Assuming semicolon is necessary
                        ),
                        new Ast.Stmt.Expression(new Ast.Expr.Function(Optional.empty(), "func", Arrays.asList()))
                ),
                Arguments.of("Missing Semicolon",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "x", 0) // Missing semicolon case
                        ),
                        null // Expect ParseException for missing semicolon
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Stmt.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Stmt.Assignment(
                                new Ast.Expr.Access(Optional.empty(), "name"),
                                new Ast.Expr.Access(Optional.empty(), "value")
                        )
                ),

                Arguments.of("Variable Assignment",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.OPERATOR, ";", 11)
                        ),
                        new Ast.Stmt.Assignment(
                                new Ast.Expr.Access(Optional.empty(), "name"),
                                new Ast.Expr.Access(Optional.empty(), "expr")
                        )
                ),
                Arguments.of("Field Assignment",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "field", 4),
                                new Token(Token.Type.OPERATOR, "=", 10),
                                new Token(Token.Type.IDENTIFIER, "expr", 12),
                                new Token(Token.Type.OPERATOR, ";", 16)
                        ),
                        new Ast.Stmt.Assignment(
                                new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj")), "field"),
                                new Ast.Expr.Access(Optional.empty(), "expr")
                        )
                ),
                Arguments.of("Complex Value Assignment",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "expr1", 7),
                                new Token(Token.Type.OPERATOR, "+", 13),
                                new Token(Token.Type.IDENTIFIER, "expr2", 15),
                                new Token(Token.Type.OPERATOR, ";", 21)
                        ),
                        new Ast.Stmt.Assignment(
                                new Ast.Expr.Access(Optional.empty(), "name"),
                                new Ast.Expr.Binary("+",
                                        new Ast.Expr.Access(Optional.empty(), "expr1"),
                                        new Ast.Expr.Access(Optional.empty(), "expr2")
                                )
                        )
                ),
                Arguments.of("Missing Value in Assignment",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.OPERATOR, ";", 7) // Missing value
                        ),
                        null // Expect ParseException for missing value
                ),
                Arguments.of("Missing Semicolon in Assignment",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "expr", 7) // Missing semicolon
                        ),
                        null // Expect ParseException for missing semicolon
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expr.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expr.Literal(Boolean.TRUE)
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expr.Literal(new BigInteger("1"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expr.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expr.Literal('c')
                ),
                Arguments.of("String Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expr.Literal("string")
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expr.Literal("Hello,\nWorld!")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expr.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expr.Group(new Ast.Expr.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                //(expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expr.Group(new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        ))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expr.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                //expr1 AND expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expr.Binary("AND",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expr.Binary("==",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary("*",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expr.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expr.Access(Optional.empty(), "name")
                ),
                Arguments.of("Field Access",
                        Arrays.asList(
                                //obj.field
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "field", 4)
                        ),
                        new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj")), "field")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expr.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                //name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList(
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2"),
                                new Ast.Expr.Access(Optional.empty(), "expr3")
                        ))
                ),
                Arguments.of("Method Call",
                        Arrays.asList(
                                //obj.method()
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "method", 4),
                                new Token(Token.Type.OPERATOR, "(", 10),
                                new Token(Token.Type.OPERATOR, ")", 11)
                        ),
                        new Ast.Expr.Function(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj")), "method", Arrays.asList())
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

    @ParameterizedTest
    @MethodSource
    void testMissingSemicolonStatement(String test, List<Token> tokens, Ast.Stmt expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testMissingSemicolonStatement() {
        return Stream.of(
                Arguments.of("Missing Semicolon",
                        Arrays.asList(
                                // f
                                new Token(Token.Type.IDENTIFIER, "f", 0)
                        ),
                        null // Expect ParseException for missing semicolon
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentExpression(String test, List<Token> tokens, Ast.Stmt.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentExpression() {
        return Stream.of(
                Arguments.of("Variable Assignment",
                        Arrays.asList(
                                //name = expr;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.OPERATOR, ";", 11)
                        ),
                        new Ast.Stmt.Assignment(
                                new Ast.Expr.Access(Optional.empty(), "name"),
                                new Ast.Expr.Access(Optional.empty(), "expr")
                        )
                ),
                Arguments.of("Field Assignment",
                        Arrays.asList(
                                //obj.field = expr;
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "field", 4),
                                new Token(Token.Type.OPERATOR, "=", 10),
                                new Token(Token.Type.IDENTIFIER, "expr", 12),
                                new Token(Token.Type.OPERATOR, ";", 16)
                        ),
                        new Ast.Stmt.Assignment(
                                new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj")), "field"),
                                new Ast.Expr.Access(Optional.empty(), "expr")
                        )
                ),
                Arguments.of("Missing Value in Assignment",
                        Arrays.asList(
                                //name = ;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.OPERATOR, ";", 7)
                        ),
                        null // Expect ParseException for missing value
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpressions(String test, List<Token> tokens, Ast.Expr.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpressions() {
        return Stream.of(
                Arguments.of("Nil Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "NIL", 0)),
                        new Ast.Expr.Literal(null)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpressionErrors(String test, List<Token> tokens, Ast.Expr.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpressionErrors() {
        return Stream.of(
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                //(expr
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                                // Missing closing parenthesis
                        ),
                        null // Expect ParseException for missing closing parenthesis
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpressionErrors(String test, List<Token> tokens, Ast.Expr.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpressionErrors() {
        return Stream.of(
                Arguments.of("Binary Missing Operand",
                        Arrays.asList(
                                // expr -
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, "-", 5)
                                // Missing second operand
                        ),
                        null // Expect ParseException for missing operand
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpressionErrors(String test, List<Token> tokens, Ast.Expr.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpressionErrors() {
        return Stream.of(
                Arguments.of("Invalid Field Access Name",
                        Arrays.asList(
                                // obj.5
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.INTEGER, "5", 4)
                        ),
                        null // Expect ParseException for invalid field access name
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpressionErrors(String test, List<Token> tokens, Ast.Expr.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpressionErrors() {
        return Stream.of(
                Arguments.of("Trailing Comma in Function Call",
                        Arrays.asList(
                                //name(expr1,)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.OPERATOR, ")", 11)
                        ),
                        null // Expect ParseException for trailing comma
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testPriorityExpressions(String test, List<Token> tokens, Ast.Expr.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testPriorityExpressions() {
        return Stream.of(
                Arguments.of("Addition and Multiplication",
                        Arrays.asList(
                                //expr1 + expr2 * expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "*", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Binary("*",
                                        new Ast.Expr.Access(Optional.empty(), "expr2"),
                                        new Ast.Expr.Access(Optional.empty(), "expr3")
                                )
                        )
                ),
                Arguments.of("And and Or",
                        Arrays.asList(
                                //expr1 AND expr2 OR expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10),
                                new Token(Token.Type.IDENTIFIER, "OR", 16),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19)
                        ),
                        new Ast.Expr.Binary("OR",
                                new Ast.Expr.Binary("AND",
                                        new Ast.Expr.Access(Optional.empty(), "expr1"),
                                        new Ast.Expr.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expr.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Equals and Not Equals",
                        Arrays.asList(
                                //expr1 == expr2 != expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, "!=", 15),
                                new Token(Token.Type.IDENTIFIER, "expr3", 18)
                        ),
                        new Ast.Expr.Binary("!=",
                                new Ast.Expr.Binary("==",
                                        new Ast.Expr.Access(Optional.empty(), "expr1"),
                                        new Ast.Expr.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expr.Access(Optional.empty(), "expr3")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInvalidExpression(String test, List<Token> tokens) {
        test(tokens, null, Parser::parseExpression);
    }

    private static Stream<Arguments> testInvalidExpression() {
        return Stream.of(
                Arguments.of("Invalid Expression",
                        Arrays.asList(
                                // ?
                                new Token(Token.Type.OPERATOR, "?", 0)
                        )
                ),
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                // (expr
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        )
                ),
                Arguments.of("Invalid Closing Parenthesis",
                        Arrays.asList(
                                // (expr]
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, "]", 5)
                        )
                ),

                Arguments.of("Invalid Expression",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "!", 0) // Invalid expression
                        )
                ),
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                                // Missing closing parenthesis
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testEdgeCases(String test, List<Token> tokens) {
        test(tokens, null, Parser::parseExpression);
    }

    private static Stream<Arguments> testEdgeCases() {
        return Stream.of(
                // Empty input should fail
                Arguments.of("Empty Input",
                        Arrays.asList()),

                // Single operator without operands should fail
                Arguments.of("Single Operator",
                        Arrays.asList(
                                // +
                                new Token(Token.Type.OPERATOR, "+", 0)
                        )
                ),

                // Unclosed group (multiple parentheses)
                Arguments.of("Unclosed Nested Group",
                        Arrays.asList(
                                //( (expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.OPERATOR, "(", 1),
                                new Token(Token.Type.IDENTIFIER, "expr", 2),
                                new Token(Token.Type.OPERATOR, ")", 6)
                                // Missing final closing parenthesis
                        )
                ),

                // Function call with no closing parenthesis
                Arguments.of("Function Call Missing Closing Parenthesis",
                        Arrays.asList(
                                //name(expr
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr", 5)
                                // Missing closing parenthesis
                        )
                ),

                // Mismatched brackets should fail
                Arguments.of("Mismatched Brackets",
                        Arrays.asList(
                                //[expr)
                                new Token(Token.Type.OPERATOR, "[", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        )
                )
        );
    }
}
