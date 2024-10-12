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

final class P2bParserTests {

    // Category: Source (5 Tests)

    @ParameterizedTest
    @MethodSource
    void testSource(String test, List<Token> tokens, Ast.Source expected) {
        test(tokens, expected, Parser::parseSource);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Empty", Arrays.asList(), new Ast.Source(Arrays.asList(), Arrays.asList())),
                Arguments.of("Field",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "LET", 0), new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9), new Token(Token.Type.IDENTIFIER, "expr", 11), new Token(Token.Type.OPERATOR, ";", 15)),
                        new Ast.Source(Arrays.asList(new Ast.Field("name", Optional.of(new Ast.Expr.Access(Optional.empty(), "expr")))), Arrays.asList())
                ),
                Arguments.of("Method",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "DEF", 0), new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 8), new Token(Token.Type.OPERATOR, ")", 9), new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14), new Token(Token.Type.OPERATOR, ";", 18), new Token(Token.Type.IDENTIFIER, "END", 20)),
                        new Ast.Source(Arrays.asList(), Arrays.asList(new Ast.Method("name", Arrays.asList(), Arrays.asList(new Ast.Stmt.Expression(new Ast.Expr.Access(Optional.empty(), "stmt"))))))
                ),
                Arguments.of("Field Method",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0), new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9), new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.OPERATOR, ";", 15), new Token(Token.Type.IDENTIFIER, "DEF", 16),
                                new Token(Token.Type.IDENTIFIER, "name", 20), new Token(Token.Type.OPERATOR, "(", 24),
                                new Token(Token.Type.OPERATOR, ")", 25), new Token(Token.Type.IDENTIFIER, "DO", 27),
                                new Token(Token.Type.IDENTIFIER, "stmt", 30), new Token(Token.Type.OPERATOR, ";", 34),
                                new Token(Token.Type.IDENTIFIER, "END", 36)
                        ),
                        new Ast.Source(Arrays.asList(new Ast.Field("name", Optional.of(new Ast.Expr.Access(Optional.empty(), "expr")))),
                                Arrays.asList(new Ast.Method("name", Arrays.asList(), Arrays.asList(new Ast.Stmt.Expression(new Ast.Expr.Access(Optional.empty(), "stmt"))))))
                ),
                Arguments.of("Method Field",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "DEF", 0), new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 8), new Token(Token.Type.OPERATOR, ")", 9),
                                new Token(Token.Type.IDENTIFIER, "DO", 11), new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18), new Token(Token.Type.IDENTIFIER, "END", 20),
                                new Token(Token.Type.IDENTIFIER, "LET", 21), new Token(Token.Type.IDENTIFIER, "name", 25),
                                new Token(Token.Type.OPERATOR, "=", 30), new Token(Token.Type.IDENTIFIER, "expr", 32),
                                new Token(Token.Type.OPERATOR, ";", 36)
                        ),
                        new Ast.Source(Arrays.asList(new Ast.Field("name", Optional.of(new Ast.Expr.Access(Optional.empty(), "expr")))),
                                Arrays.asList(new Ast.Method("name", Arrays.asList(), Arrays.asList(new Ast.Stmt.Expression(new Ast.Expr.Access(Optional.empty(), "stmt"))))))
                )
        );
    }

    // Category: Stmt (19 Tests)

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Stmt.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Variable Expression",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "expr", 0), new Token(Token.Type.OPERATOR, ";", 4)),
                        new Ast.Stmt.Expression(new Ast.Expr.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Function Call",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0), new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5), new Token(Token.Type.OPERATOR, ";", 6)),
                        new Ast.Stmt.Expression(new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList()))
                ),
                Arguments.of("Missing Semicolon",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "f", 0)),
                        null  // Expected to throw ParseException
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, List<Token> tokens, Ast.Stmt.Declaration expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Variable Declaration",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "LET", 0), new Token(Token.Type.IDENTIFIER, "name", 4), new Token(Token.Type.OPERATOR, ";", 9)),
                        new Ast.Stmt.Declaration("name", Optional.empty())
                ),
                Arguments.of("Variable Initialization",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "LET", 0), new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9), new Token(Token.Type.IDENTIFIER, "expr", 11), new Token(Token.Type.OPERATOR, ";", 15)),
                        new Ast.Stmt.Declaration("name", Optional.of(new Ast.Expr.Access(Optional.empty(), "expr")))
                ),
                Arguments.of("Type Annotation",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "LET", 0), new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ":", 9), new Token(Token.Type.IDENTIFIER, "Type", 11),
                                new Token(Token.Type.OPERATOR, "=", 16), new Token(Token.Type.IDENTIFIER, "expr", 18),
                                new Token(Token.Type.OPERATOR, ";", 22)),
                        new Ast.Stmt.Declaration("name", Optional.of(new Ast.Expr.Access(Optional.empty(), "expr")))
                )
        );
    }

    // Additional test cases for other categories like If, For, While, Return, Expr, Literal, Group, Binary, Access, Function, Priority, Baseline, Exception...

    // Utility method for testing
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }
}
