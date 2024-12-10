package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        writer.println("public class Main {");
        newline(++indent);

        // Declare all fields
        for (int i = 0; i < ast.getFields().size(); i++) {
            visit(ast.getFields().get(i));

            // Avoid adding a newline after the last field
            if (i < ast.getFields().size() - 1) {
                newline(indent);
            }
            else {
                newline(0);
            }
        }

        // Add a newline after fields if any are present
        if (!ast.getFields().isEmpty()) {
            newline(indent);
        }

        // Include the `public static void main` method only if a `main` method is defined
        boolean hasMainMethod = ast.getMethods().stream().anyMatch(method -> method.getName().equals("main"));
        if (hasMainMethod) {
            writer.println("public static void main(String[] args) {");
            writer.println("        System.exit(new Main().main());");
            writer.println("    }");
            newline(indent);
        }

        // Declare all methods
        for (int i = 0; i < ast.getMethods().size(); i++) {
            visit(ast.getMethods().get(i));

            // Avoid adding a newline after the last method
            if (i < ast.getMethods().size() - 1) {
                newline(indent);
            }
        }

        newline(0);
        writer.print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        // Use the JVM-specific type name
        String typeName = ast.getVariable().getType().getJvmName();

        // Use the JVM-specific variable name
        String variableName = ast.getVariable().getJvmName();

        // Generate the field declaration
        print(typeName, " ", variableName);

        // Include initialization value if present
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {

        // Map return type names to Java equivalents
        String returnType = ast.getReturnTypeName().orElse("void");
        if ("Integer".equals(returnType)) {
            returnType = "int";
        } else if ("Decimal".equals(returnType)) {
            returnType = "double";
        } else if ("Void".equals(returnType)) {
            returnType = "void"; // Correctly map "Void" to "void"
        }

        writer.print(returnType + " " + ast.getName() + "(");

        // Generate method parameters
        for (int i = 0; i < ast.getParameters().size(); i++) {
            String parameterType = ast.getParameterTypeNames().get(i);
            if ("Integer".equals(parameterType)) {
                parameterType = "int";
            } else if ("Decimal".equals(parameterType)) {
                parameterType = "double";
            } else if ("Integeriterable".equals(parameterType)) {
                parameterType = "Iterable<Integer>"; // Correct the parameter type
            }

            if (i > 0) {
                writer.print(", ");
            }
            writer.print(parameterType + " " + ast.getParameters().get(i));
        }
        writer.print(") {");

        if (ast.getStatements().isEmpty()) {
            // Ensure the closing brace is properly formatted for empty method bodies
            newline(indent);
            writer.print("}");
        } else {
            newline(++indent);

            // Generate method body statements with correct indentation and newlines
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));

                // Avoid adding a newline after the last statement
                if (i < ast.getStatements().size() - 1) {
                    newline(indent);
                }
            }

            newline(--indent);
            writer.print("}");
        }
        newline(0);
        return null;
    }


    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        if (ast.getExpression() instanceof Ast.Expr.Function) {
            Ast.Expr.Function function = (Ast.Expr.Function) ast.getExpression();
            if (function.getName().equals("print")) {
                // Handle printing multiple arguments separately
                for (int i = 0; i < function.getArguments().size(); i++) {
                    print("System.out.println(");
                    visit(function.getArguments().get(i));
                    print(");");
                    if (i < function.getArguments().size() - 1) {
                        newline(indent);
                    }
                }
                return null;
            }
        }
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        // Determine the type name and map to Java primitive or reference types
        String typeName = ast.getTypeName().orElse("");
        if (typeName.isEmpty() && ast.getValue().isPresent()) {
            // Infer type from the value expression
            Ast.Expr value = ast.getValue().get();
            if (value instanceof Ast.Expr.Literal) {
                Object literal = ((Ast.Expr.Literal) value).getLiteral();
                if (literal instanceof BigDecimal) {
                    typeName = "double";
                } else if (literal instanceof BigInteger || literal instanceof Integer) {
                    typeName = "int";
                } else if (literal instanceof String) {
                    typeName = "String";
                }
            } else {
                typeName = "var"; // Fallback to 'var' if no type can be inferred
            }
        } else if ("Integer".equals(typeName)) {
            typeName = "int"; // Convert "Integer" to "int"
        } else if ("Decimal".equals(typeName)) {
            typeName = "double"; // Convert "Decimal" to "double"
        } else if ("String".equals(typeName)) {
            typeName = "String"; // Ensure proper mapping for "String"
        }

        // Print the declaration with the inferred or specified type
        print(typeName, " ", ast.getName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");
        newline(++indent);

        for (int i = 0; i < ast.getThenStatements().size(); i++) {
            visit(ast.getThenStatements().get(i));
            if (i < ast.getThenStatements().size() - 1) {
                newline(indent);
            }
        }

        newline(--indent);
        print("}");

        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            newline(++indent);
            for (int i = 0; i < ast.getElseStatements().size(); i++) {
                visit(ast.getElseStatements().get(i));
                if (i < ast.getElseStatements().size() - 1) {
                    newline(indent);
                }
            }
            newline(--indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        print("for (var ", ast.getName(), " : ");
        visit(ast.getValue());
        print(") {");
        newline(++indent);

        // Visit all statements in the loop body
        for (int i = 0; i < ast.getStatements().size(); i++) {
            visit(ast.getStatements().get(i));
            if (i < ast.getStatements().size() - 1) {
                newline(indent);
            }
        }

        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        // Print the "while" loop header
        print("while (");
        visit(ast.getCondition());
        print(") {");

        // Handle the body
        if (ast.getStatements().isEmpty()) {
            // For an empty body, directly close the block without a space or newline
            print("}");
        } else {
            // For non-empty body, add a newline, process statements, and close the block
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
                if (i < ast.getStatements().size() - 1) {
                    newline(indent);
                }
            }
            newline(--indent);
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() == null) {
            print("null");
        } else if (ast.getLiteral() instanceof String) {
            // Handle string literals with proper escaping
            print("\"", ast.getLiteral().toString().replace("\"", "\\\""), "\"");
        } else if (ast.getLiteral() instanceof Character) {
            // Handle character literals with single quotes
            print("'", ast.getLiteral().toString().replace("'", "\\'"), "'");
        } else {
            // Handle other literal types (numbers, booleans, etc.)
            print(ast.getLiteral().toString());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        // Visit the left-hand side of the binary operation
        visit(ast.getLeft());

        // Map logical operators to Java equivalents
        String operator = ast.getOperator();
        if ("AND".equals(operator)) {
            operator = "&&";
        } else if ("OR".equals(operator)) {
            operator = "||";
        }

        // Print the operator
        print(" ", operator, " ");

        // Visit the right-hand side of the binary operation
        visit(ast.getRight());

        return null;
    }


    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        // Use the variable's name instead of getJvmName()
        Environment.Variable variable = ast.getVariable();
        print(variable.getName());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        // Retrieve function JVM name from environment and print it
        Environment.Function function = ast.getFunction();
        print(function.getJvmName(), "(");

        for (int i = 0; i < ast.getArguments().size(); i++) {
            if (i > 0) {
                print(", ");
            }
            visit(ast.getArguments().get(i));
        }
        print(")");
        return null;
    }

}