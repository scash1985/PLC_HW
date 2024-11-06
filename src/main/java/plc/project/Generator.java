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
        // Print class header
        print("public class Main {");
        newline(++indent);
        // Generate fields
        for (Ast.Field field : ast.getFields()) {
            visit(field);
            newline(indent);
        }
        // Generate methods
        for (Ast.Method method : ast.getMethods()) {
            newline(indent);
            visit(method);
        }
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        print("public static ", ast.getTypeName(), " ", ast.getName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        boolean isMainMethod = ast.getName().equals("main") && ast.getParameters().isEmpty();

        // Print the public static main method if it's the entry point
        if (isMainMethod) {
            print("public static void main(String[] args) {");
            newline(++indent);
            print("System.exit(new Main().main());");
            newline(--indent);
            print("}");
            newline(indent);
        }

        // Print method header for all other methods, including the int main() method
        String returnType = ast.getReturnTypeName().orElse("void");
        if (returnType.equals("Integer")) {
            returnType = "int"; // Ensure correct return type formatting
        }

        print("int main() {");
        newline(++indent);

        // Print method body statements
        for (Ast.Stmt statement : ast.getStatements()) {
            visit(statement);
            newline(indent);
        }

        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        if (ast.getExpression() instanceof Ast.Expr.Function) {
            Ast.Expr.Function function = (Ast.Expr.Function) ast.getExpression();
            if (function.getName().equals("print")) {
                print("System.out.println(");
                visit(function.getArguments().get(0)); // Assumes one argument for print
                print(")");
                print(";");
                return null;
            }
        }
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        String typeName = ast.getTypeName().orElse("");
        if (typeName.isEmpty() && ast.getValue().isPresent()) {
            // Infer type from the value expression
            Object value = ast.getValue().get();
            if (value instanceof Ast.Expr.Literal) {
                Object literal = ((Ast.Expr.Literal) value).getLiteral();
                if (literal instanceof BigDecimal) {
                    typeName = "double";
                } else if (literal instanceof BigInteger || literal instanceof Integer) {
                    typeName = "int";
                }
            } else {
                typeName = "var";
            }
        } else if (typeName.equals("Integer")) {
            typeName = "int";
        } else if (typeName.equals("Decimal")) {
            typeName = "double";
        }

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

        for (Ast.Stmt statement : ast.getStatements()) {
            visit(statement);
            newline(indent);
        }

        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");
        newline(++indent);

        for (Ast.Stmt statement : ast.getStatements()) {
            visit(statement);
            newline(indent);
        }

        newline(--indent);
        print("}");
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
            print("\"", ast.getLiteral().toString().replace("\"", "\\\""), "\"");
        } else {
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
        visit(ast.getLeft());

        // Map logical operators to Java equivalents
        String operator = ast.getOperator();
        if ("AND".equals(operator)) {
            operator = "&&";
        } else if ("OR".equals(operator)) {
            operator = "||";
        }

        print(" ", operator, " ");
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        // Check if variable exists and use its JVM name
        Environment.Variable variable = ast.getVariable();
        print(variable.getJvmName());
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
