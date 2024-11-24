package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class GeneratorTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, Ast.Source ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Hello, World!",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                        new Ast.Stmt.Expression(init(new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(
                                                init(new Ast.Expr.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                        new Ast.Stmt.Return(init(new Ast.Expr.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                                )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))))
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int main() {",
                                "        System.out.println(\"Hello, World!\");",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Multiple Fields & Methods",
                        new Ast.Source(
                                Arrays.asList(
                                        init(new Ast.Field("x", "Integer", Optional.of(
                                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                                        )), ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, Environment.NIL))),
                                        init(new Ast.Field("y", "Decimal", Optional.of(
                                                init(new Ast.Expr.Literal(new BigDecimal("1.5")), ast -> ast.setType(Environment.Type.DECIMAL))
                                        )), ast -> ast.setVariable(new Environment.Variable("y", "y", Environment.Type.DECIMAL, Environment.NIL))),
                                        init(new Ast.Field("z", "String", Optional.of(
                                                init(new Ast.Expr.Literal("Hello"), ast -> ast.setType(Environment.Type.STRING))
                                        )), ast -> ast.setVariable(new Environment.Variable("z", "z", Environment.Type.STRING, Environment.NIL)))
                                ),
                                Arrays.asList(
                                        init(new Ast.Method("f", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Stmt.Return(init(new Ast.Expr.Access(Optional.empty(), "x"), ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, Environment.NIL))))
                                        )), ast -> ast.setFunction(new Environment.Function("f", "f", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))),
                                        init(new Ast.Method("g", Arrays.asList(), Arrays.asList(), Optional.of("Decimal"), Arrays.asList(
                                                new Ast.Stmt.Return(init(new Ast.Expr.Access(Optional.empty(), "y"), ast -> ast.setVariable(new Environment.Variable("y", "y", Environment.Type.DECIMAL, Environment.NIL))))
                                        )), ast -> ast.setFunction(new Environment.Function("g", "g", Arrays.asList(), Environment.Type.DECIMAL, args -> Environment.NIL))),
                                        init(new Ast.Method("h", Arrays.asList(), Arrays.asList(), Optional.of("String"), Arrays.asList(
                                                new Ast.Stmt.Return(init(new Ast.Expr.Access(Optional.empty(), "z"), ast -> ast.setVariable(new Environment.Variable("z", "z", Environment.Type.STRING, Environment.NIL))))
                                        )), ast -> ast.setFunction(new Environment.Function("h", "h", Arrays.asList(), Environment.Type.STRING, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    int x = 10;",
                                "    double y = 1.5;",
                                "    String z = \"Hello\";",
                                "",
                                "    int f() {",
                                "        return x;",
                                "    }",
                                "",
                                "    double g() {",
                                "        return y;",
                                "    }",
                                "",
                                "    String h() {",
                                "        return z;",
                                "    }",
                                "",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMethod(String test, Ast.Source ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
                Arguments.of("Square Method",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Method(
                                                "square",
                                                Arrays.asList("num"),
                                                Arrays.asList("Decimal"),
                                                Optional.of("Decimal"),
                                                Arrays.asList(
                                                        new Ast.Stmt.Return(
                                                                init(new Ast.Expr.Binary("*",
                                                                        init(new Ast.Expr.Access(Optional.empty(), "num"), ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.DECIMAL, Environment.NIL))),
                                                                        init(new Ast.Expr.Access(Optional.empty(), "num"), ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.DECIMAL, Environment.NIL)))
                                                                ), ast -> {})
                                                        )
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("square", "square", Arrays.asList(Environment.Type.DECIMAL), Environment.Type.DECIMAL, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    double square(double num) {",
                                "        return num * num;",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Multiple Statements",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Method(
                                                "func",
                                                Arrays.asList("x", "y", "z"),
                                                Arrays.asList("Integer", "Decimal", "String"),
                                                Optional.of("Void"),
                                                Arrays.asList(
                                                        // Print x
                                                        new Ast.Stmt.Expression(init(new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(
                                                                init(new Ast.Expr.Access(Optional.empty(), "x"), ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, Environment.NIL)))
                                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                                        // Print y
                                                        new Ast.Stmt.Expression(init(new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(
                                                                init(new Ast.Expr.Access(Optional.empty(), "y"), ast -> ast.setVariable(new Environment.Variable("y", "y", Environment.Type.DECIMAL, Environment.NIL)))
                                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                                        // Print z
                                                        new Ast.Stmt.Expression(init(new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(
                                                                init(new Ast.Expr.Access(Optional.empty(), "z"), ast -> ast.setVariable(new Environment.Variable("z", "z", Environment.Type.STRING, Environment.NIL)))
                                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("func", "func", Arrays.asList(Environment.Type.INTEGER, Environment.Type.DECIMAL, Environment.Type.STRING), Environment.Type.NIL, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    void func(int x, double y, String z) {",
                                "        System.out.println(x);",
                                "        System.out.println(y);",
                                "        System.out.println(z);",
                                "    }",
                                "",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAdditionalMethods(String test, Ast.Source ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testAdditionalMethods() {
        return Stream.of(
                // Zero Arguments: DEF func(): String DO stmt; END
                Arguments.of("Zero Arguments Method",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Method(
                                                "func",
                                                Arrays.asList(),
                                                Arrays.asList(),
                                                Optional.of("String"),
                                                Arrays.asList(
                                                        new Ast.Stmt.Return(
                                                                init(new Ast.Expr.Literal("Test"), ast -> ast.setType(Environment.Type.STRING))
                                                        )
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("func", "func", Arrays.asList(), Environment.Type.STRING, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    String func() {",
                                "        return \"Test\";",
                                "    }",
                                "",
                                "}"
                        )
                ),

                // Single Argument: DEF func(x: String): String DO stmt; END
                Arguments.of("Single Argument Method",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Method(
                                                "func",
                                                Arrays.asList("x"),
                                                Arrays.asList("String"),
                                                Optional.of("String"),
                                                Arrays.asList(
                                                        new Ast.Stmt.Return(
                                                                init(new Ast.Expr.Access(Optional.empty(), "x"), ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.STRING, Environment.NIL)))
                                                        )
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("func", "func", Arrays.asList(Environment.Type.STRING), Environment.Type.STRING, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    String func(String x) {",
                                "        return x;",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Multiple Arguments Method",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Method(
                                                "func",
                                                Arrays.asList("x", "y", "z"),
                                                Arrays.asList("String", "String", "String"),
                                                Optional.of("String"),
                                                Arrays.asList(
                                                        new Ast.Stmt.Return(init(new Ast.Expr.Literal("stmt"), ast -> ast.setType(Environment.Type.STRING)))
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("func", "func",
                                                Arrays.asList(Environment.Type.STRING, Environment.Type.STRING, Environment.Type.STRING),
                                                Environment.Type.STRING, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    String func(String x, String y, String z) {",
                                "        return \"stmt\";",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Zero Statements Method",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Method(
                                                "func",
                                                Arrays.asList(),
                                                Arrays.asList(),
                                                Optional.of("String"),
                                                Arrays.asList()
                                        ), ast -> ast.setFunction(new Environment.Function("func", "func",
                                                Arrays.asList(),
                                                Environment.Type.STRING, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    String func() {",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Method with Argument",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Method(
                                                "func",
                                                Arrays.asList("num"),
                                                Arrays.asList("Decimal"),
                                                Optional.of("String"),
                                                Arrays.asList(
                                                        new Ast.Stmt.Return(
                                                                init(new Ast.Expr.Literal("result"), ast -> ast.setType(Environment.Type.STRING))
                                                        )
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("func", "func", Arrays.asList(Environment.Type.DECIMAL), Environment.Type.STRING, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    String func(double num) {",
                                "        return \"result\";",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Method without Argument",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Method(
                                                "func",
                                                Arrays.asList(),
                                                Arrays.asList(),
                                                Optional.of("Decimal"),
                                                Arrays.asList(
                                                        new Ast.Stmt.Return(
                                                                init(new Ast.Expr.Literal(new BigDecimal("3.14")), ast -> ast.setType(Environment.Type.DECIMAL))
                                                        )
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("func", "func", Arrays.asList(), Environment.Type.DECIMAL, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    double func() {",
                                "        return 3.14;",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Empty Return Type",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Method(
                                                "func",
                                                Arrays.asList(),
                                                Arrays.asList(),
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Stmt.Expression(
                                                                init(new Ast.Expr.Function(
                                                                        Optional.empty(),
                                                                        "stmt",
                                                                        Arrays.asList()
                                                                ), ast -> ast.setFunction(new Environment.Function("stmt", "stmt", Arrays.asList(), Environment.Type.NIL, args -> Environment.NIL)))
                                                        )
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("func", "func", Arrays.asList(), Environment.Type.NIL, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    void func() {",
                                "        stmt();",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Nested Statements",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Method(
                                                "printOdds",
                                                Arrays.asList("list"),
                                                Arrays.asList("Integeriterable"),
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Stmt.For(
                                                                "num",
                                                                init(new Ast.Expr.Access(Optional.empty(), "list"), ast -> ast.setVariable(new Environment.Variable("list", "list", Environment.Type.ANY, Environment.NIL))),
                                                                Arrays.asList(
                                                                        new Ast.Stmt.If(
                                                                                init(new Ast.Expr.Binary("!=",
                                                                                        init(new Ast.Expr.Binary("*",
                                                                                                init(new Ast.Expr.Binary("/",
                                                                                                        init(new Ast.Expr.Access(Optional.empty(), "num"), ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, Environment.NIL))),
                                                                                                        init(new Ast.Expr.Literal(2), ast -> ast.setType(Environment.Type.INTEGER))
                                                                                                ), ast -> ast.setType(Environment.Type.INTEGER)),
                                                                                                init(new Ast.Expr.Literal(2), ast -> ast.setType(Environment.Type.INTEGER))
                                                                                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                                                                                        init(new Ast.Expr.Access(Optional.empty(), "num"), ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, Environment.NIL)))
                                                                                ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                                                                Arrays.asList(
                                                                                        new Ast.Stmt.Expression(
                                                                                                init(new Ast.Expr.Function(
                                                                                                        Optional.empty(),
                                                                                                        "print",
                                                                                                        Arrays.asList(init(new Ast.Expr.Access(Optional.empty(), "num"), ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, Environment.NIL))))
                                                                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                                                                        )
                                                                                ),
                                                                                Arrays.asList()
                                                                        )
                                                                )
                                                        )
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("printOdds", "printOdds", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    void printOdds(Iterable<Integer> list) {",
                                "        for (var num : list) {",
                                "            if ((num / 2) * 2 != num) {",
                                "                System.out.println(num);",
                                "            }",
                                "        }",
                                "    }",
                                "",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationStatement(String test, Ast.Stmt.Declaration ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        init(new Ast.Stmt.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, Environment.NIL))),
                        "int name;"
                ),
                Arguments.of("Initialization",
                        // LET name = 1.0;
                        init(new Ast.Stmt.Declaration("name", Optional.empty(), Optional.of(
                                init(new Ast.Expr.Literal(new BigDecimal("1.0")),ast -> ast.setType(Environment.Type.DECIMAL))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, Environment.NIL))),
                        "double name = 1.0;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, Ast.Stmt.If ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        new Ast.Stmt.If(
                                init(new Ast.Expr.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, Environment.NIL))),
                                Arrays.asList(new Ast.Stmt.Expression(init(new Ast.Expr.Access(Optional.empty(), "stmt"), ast -> ast.setVariable(new Environment.Variable("stmt", "stmt", Environment.Type.NIL, Environment.NIL))))),
                                Arrays.asList()
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt;",
                                "}"
                        )
                ),
                Arguments.of("Else",
                        new Ast.Stmt.If(
                                init(new Ast.Expr.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, Environment.NIL))),
                                Arrays.asList(new Ast.Stmt.Expression(init(new Ast.Expr.Access(Optional.empty(), "stmt1"), ast -> ast.setVariable(new Environment.Variable("stmt1", "stmt1", Environment.Type.NIL, Environment.NIL))))),
                                Arrays.asList(new Ast.Stmt.Expression(init(new Ast.Expr.Access(Optional.empty(), "stmt2"), ast -> ast.setVariable(new Environment.Variable("stmt2", "stmt2", Environment.Type.NIL, Environment.NIL)))))
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt1;",
                                "} else {",
                                "    stmt2;",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFor(String test, Ast.Stmt.For ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testFor() {
        return Stream.of(
                Arguments.of("For",
                        // FOR num IN list DO print(num); END
                        new Ast.Stmt.For(
                                "num",
                                init(new Ast.Expr.Access(Optional.empty(), "list"),
                                        ast -> ast.setVariable(new Environment.Variable("list", "list", Environment.Type.ANY, Environment.NIL))),
                                Arrays.asList(
                                        new Ast.Stmt.Expression(
                                                init(new Ast.Expr.Function(
                                                                Optional.empty(),
                                                                "print",
                                                                Arrays.asList(
                                                                        init(new Ast.Expr.Access(Optional.empty(), "num"),
                                                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.ANY, Environment.NIL)))
                                                                )
                                                        ),
                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "for (var num : list) {",
                                "    System.out.println(num);",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testWhile(String test, Ast.Stmt.While ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testWhile() {
        return Stream.of(
                // Test for While with Empty Body
                Arguments.of("While with Empty Body",
                        // WHILE cond DO END
                        new Ast.Stmt.While(
                                init(new Ast.Expr.Access(Optional.empty(), "cond"),
                                        ast -> ast.setVariable(new Environment.Variable("cond", "cond", Environment.Type.BOOLEAN, Environment.NIL))),
                                Arrays.asList()
                        ),
                        String.join(System.lineSeparator(),
                                "while (cond) {}"
                        )
                ),
                // Test for While with Multiple Statements
                Arguments.of("While with Multiple Statements",
                        // WHILE cond DO print(1); print(2); print(3); END
                        new Ast.Stmt.While(
                                init(new Ast.Expr.Access(Optional.empty(), "cond"),
                                        ast -> ast.setVariable(new Environment.Variable("cond", "cond", Environment.Type.BOOLEAN, Environment.NIL))),
                                Arrays.asList(
                                        new Ast.Stmt.Expression(
                                                init(new Ast.Expr.Function(
                                                                Optional.empty(),
                                                                "print",
                                                                Arrays.asList(init(new Ast.Expr.Literal(BigInteger.ONE),
                                                                        ast -> ast.setType(Environment.Type.INTEGER)))
                                                        ),
                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        ),
                                        new Ast.Stmt.Expression(
                                                init(new Ast.Expr.Function(
                                                                Optional.empty(),
                                                                "print",
                                                                Arrays.asList(init(new Ast.Expr.Literal(BigInteger.valueOf(2)),
                                                                        ast -> ast.setType(Environment.Type.INTEGER)))
                                                        ),
                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        ),
                                        new Ast.Stmt.Expression(
                                                init(new Ast.Expr.Function(
                                                                Optional.empty(),
                                                                "print",
                                                                Arrays.asList(init(new Ast.Expr.Literal(BigInteger.valueOf(3)),
                                                                        ast -> ast.setType(Environment.Type.INTEGER)))
                                                        ),
                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "while (cond) {",
                                "    System.out.println(1);",
                                "    System.out.println(2);",
                                "    System.out.println(3);",
                                "}"
                        )
                ),
                Arguments.of("Nested While",
                        new Ast.Stmt.While(
                                // WHILE cond1 DO
                                init(new Ast.Expr.Access(Optional.empty(), "cond1"),
                                        ast -> ast.setVariable(new Environment.Variable("cond1", "cond1", Environment.Type.BOOLEAN, Environment.NIL))),
                                Arrays.asList(
                                        new Ast.Stmt.While(
                                                // WHILE cond2 DO print(1); END
                                                init(new Ast.Expr.Access(Optional.empty(), "cond2"),
                                                        ast -> ast.setVariable(new Environment.Variable("cond2", "cond2", Environment.Type.BOOLEAN, Environment.NIL))),
                                                Arrays.asList(
                                                        new Ast.Stmt.Expression(
                                                                init(new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(
                                                                        init(new Ast.Expr.Literal(BigInteger.ONE),
                                                                                ast -> ast.setType(Environment.Type.INTEGER)
                                                                        )
                                                                )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                                        )
                                                )
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "while (cond1) {",
                                "    while (cond2) {",
                                "        System.out.println(1);",
                                "    }",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testLiteral(String test, Ast.Expr.Literal ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testLiteral() {
        return Stream.of(
                Arguments.of("Nil Literal",
                        init(new Ast.Expr.Literal(null), ast -> ast.setType(Environment.Type.NIL)),
                        "null"
                ),
                Arguments.of("Boolean Literal",
                        init(new Ast.Expr.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "true"
                ),
                Arguments.of("Integer Literal",
                        init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                        "1"
                ),
                Arguments.of("Decimal Literal",
                        init(new Ast.Expr.Literal(new BigDecimal("123.456")), ast -> ast.setType(Environment.Type.DECIMAL)),
                        "123.456"
                ),
                Arguments.of("Character Literal",
                        init(new Ast.Expr.Literal('a'), ast -> ast.setType(Environment.Type.CHARACTER)),
                        "'a'"
                ),
                Arguments.of("String Literal",
                        init(new Ast.Expr.Literal("Hello World"), ast -> ast.setType(Environment.Type.STRING)),
                        "\"Hello World\""
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGroup(String test, Ast.Expr.Group ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testGroup() {
        return Stream.of(
                Arguments.of("Literal Value (1)",
                        // (1)
                        init(new Ast.Expr.Group(
                                init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> {}),
                        "(1)"
                ),
                Arguments.of("Binary Value (1 + 10)",
                        // (1 + 10)
                        init(new Ast.Expr.Group(
                                init(new Ast.Expr.Binary("+",
                                        init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                        init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                                ), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> {}),
                        "(1 + 10)" // Corrected expected value
                ),
                Arguments.of("Nested Groups ((((1))))",
                        // ((((1))))
                        init(new Ast.Expr.Group(
                                init(new Ast.Expr.Group(
                                        init(new Ast.Expr.Group(
                                                init(new Ast.Expr.Group(
                                                        init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                                                ), ast -> {})
                                        ), ast -> {})
                                ), ast -> {})
                        ), ast -> {}),
                        "((((1))))"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpression(String test, Ast.Expr.Binary ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        // TRUE AND FALSE
                        init(new Ast.Expr.Binary("AND",
                                init(new Ast.Expr.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                init(new Ast.Expr.Literal(false), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "true && false"
                ),
                Arguments.of("Or",
                        // TRUE OR FALSE
                        init(new Ast.Expr.Binary("OR",
                                init(new Ast.Expr.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                init(new Ast.Expr.Literal(false), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "true || false"
                ),
                Arguments.of("Equals",
                        // 1 == 10
                        init(new Ast.Expr.Binary("==",
                                init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "1 == 10"
                ),
                Arguments.of("Not Equals",
                        // 1 != 10
                        init(new Ast.Expr.Binary("!=",
                                init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "1 != 10"
                ),
                Arguments.of("Concatenation",
                        // "Ben" + 10
                        init(new Ast.Expr.Binary("+",
                                init(new Ast.Expr.Literal("Ben"), ast -> ast.setType(Environment.Type.STRING)),
                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.STRING)),
                        "\"Ben\" + 10"
                ),
                Arguments.of("Greater Than",
                        // 1 > 10
                        init(new Ast.Expr.Binary(">",
                                init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "1 > 10"
                ),
                Arguments.of("Less Than or Equal To",
                        // 1 <= 10
                        init(new Ast.Expr.Binary("<=",
                                init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "1 <= 10"
                ),
                Arguments.of("Addition",
                        // 1 + 10
                        init(new Ast.Expr.Binary("+",
                                init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "1 + 10"
                ),
                Arguments.of("Subtraction",
                        // 10 - 1
                        init(new Ast.Expr.Binary("-",
                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "10 - 1"
                ),
                Arguments.of("Multiplication",
                        // 10 * 100
                        init(new Ast.Expr.Binary("*",
                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.valueOf(100)), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "10 * 100"
                ),
                Arguments.of("Division",
                        // 10 / 100
                        init(new Ast.Expr.Binary("/",
                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.valueOf(100)), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "10 / 100"
                ),
                Arguments.of("Chained Addition/Subtraction",
                        // 1 + 2 - 3
                        init(new Ast.Expr.Binary("-",
                                init(new Ast.Expr.Binary("+",
                                        init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                        init(new Ast.Expr.Literal(BigInteger.valueOf(2)), ast -> ast.setType(Environment.Type.INTEGER))
                                ), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.valueOf(3)), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "(1 + 2) - 3"
                ),
                Arguments.of("Chained Multiplication/Division",
                        // 1 * 2 / 3
                        init(new Ast.Expr.Binary("/",
                                init(new Ast.Expr.Binary("*",
                                        init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                        init(new Ast.Expr.Literal(BigInteger.valueOf(2)), ast -> ast.setType(Environment.Type.INTEGER))
                                ), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.valueOf(3)), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "(1 * 2) / 3"
                ),
                Arguments.of("Priority Subtraction/Multiplication",
                        // (1 - 2) * 3
                        init(new Ast.Expr.Binary("*",
                                init(new Ast.Expr.Group(
                                        init(new Ast.Expr.Binary("-",
                                                init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                                init(new Ast.Expr.Literal(BigInteger.valueOf(2)), ast -> ast.setType(Environment.Type.INTEGER))
                                        ), ast -> ast.setType(Environment.Type.INTEGER))
                                ), ast -> {}),
                                init(new Ast.Expr.Literal(BigInteger.valueOf(3)), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "((1 - 2) * 3)"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpression(String test, Ast.Expr.Function ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Print",
                        // print("Hello, World!")
                        init(new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(
                                init(new Ast.Expr.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))),
                        "System.out.println(\"Hello, World!\")"
                ),
                Arguments.of("String Slice",
                        // "string".slice(1, 5)
                        init(new Ast.Expr.Function(Optional.of(
                                init(new Ast.Expr.Literal("string"), ast -> ast.setType(Environment.Type.STRING))
                        ), "slice", Arrays.asList(
                                init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.valueOf(5)), ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setFunction(new Environment.Function("slice", "substring", Arrays.asList(Environment.Type.ANY, Environment.Type.INTEGER, Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL))),
                        "\"string\".substring(1, 5)"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testField(String test, Ast.Field ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testField() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        init(new Ast.Field("name", "Integer", Optional.empty()),
                                ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, Environment.NIL))),
                        "int name;"
                ),
                Arguments.of("Initialization",
                        // LET name: Decimal = 1.0;
                        init(new Ast.Field("name", "Decimal", Optional.of(
                                init(new Ast.Expr.Literal(new BigDecimal("1.0")), ast -> ast.setType(Environment.Type.DECIMAL))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, Environment.NIL))),
                        "double name = 1.0;"
                ),
                Arguments.of("Supertype Declaration",
                        // LET name: Comparable = "string";
                        init(new Ast.Field("name", "Comparable", Optional.of(
                                init(new Ast.Expr.Literal("string"), ast -> ast.setType(Environment.Type.STRING))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.COMPARABLE, Environment.NIL))),
                        "Comparable name = \"string\";"
                )/*,
                Arguments.of("JVM Name Declaration",
                        // LET name: Type;
                        init(new Ast.Field("name", "Type", Optional.empty()),
                                ast -> ast.setVariable(new Environment.Variable("name", "name", new Environment.Type("Type", "Type", new Environment.Scope(null)), Environment.NIL))),
                        "Type name;"
                )*/
        );
    }

    /**
     * Helper function for tests, using a StringWriter as the output stream.
     */
    private static void test(Ast ast, String expected) {
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(ast);
        Assertions.assertEquals(expected, writer.toString());
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}