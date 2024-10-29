package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;


/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        // Visit all fields and methods in the source
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        // Look up the type in the environment
        Environment.Type fieldType = Environment.getType(ast.getTypeName());
        // If there is an expression assigned, evaluate its type
        if (ast.getValue().isPresent()) {
            Ast.Expr valueExpr = ast.getValue().get();
            visit(valueExpr);
            requireAssignable(fieldType, valueExpr.getType());
        }
        // Define the field variable in the current scope
        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), fieldType, Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        // Create a list of parameter types
        List<Environment.Type> parameterTypes = new ArrayList<>();
        for (String typeName : ast.getParameterTypeNames()) {
            parameterTypes.add(Environment.getType(typeName));
        }
        // Get the return type of the method
        Environment.Type returnType = ast.getReturnTypeName().isPresent()
                ? Environment.getType(ast.getReturnTypeName().get())
                : Environment.Type.ANY;

        // Define the method in the current scope
        Environment.Function methodFunction = scope.defineFunction(
                ast.getName(), ast.getName(), parameterTypes, returnType, args -> Environment.NIL
        );
        ast.setFunction(methodFunction);
        // Enter a new scope for the method body
        scope = new Scope(scope);
        for (int i = 0; i < ast.getParameters().size(); i++) {
            scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), parameterTypes.get(i), Environment.NIL);
        }
        // Visit each statement in the method body
        for (Ast.Stmt stmt : ast.getStatements()) {
            visit(stmt);
        }
        // Exit the scope
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        // Visit the expression to ensure it's valid
        visit(ast.getExpression());
        // Get the type of the expression
        Environment.Type exprType = ast.getExpression().getType();
        // Check if the expression is valid in the context
        if (exprType.equals(Environment.Type.NIL)) {
            throw new RuntimeException("Expression must not evaluate to NIL.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        Environment.Type declaredType = ast.getTypeName().isPresent()
                ? Environment.getType(ast.getTypeName().get())
                : null;
        // Evaluate the value expression if present
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            Environment.Type valueType = ast.getValue().get().getType();
            if (declaredType != null) {
                requireAssignable(declaredType, valueType);
            } else {
                declaredType = valueType;
            }
        } else if (declaredType == null) {
            throw new RuntimeException("Variable declaration must have a type or value.");
        }
        // Define the variable in the current scope
        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), declaredType, Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        // Ensure the receiver is an Access expression
        if (!(ast.getReceiver() instanceof Ast.Expr.Access)) {
            throw new RuntimeException("The receiver of an assignment must be an access expression.");
        }
        // Visit the receiver and the value expressions
        visit(ast.getReceiver());
        visit(ast.getValue());
        // Check type assignability
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        Object literal = ast.getLiteral();
        if (literal instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (literal instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (literal instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if (literal instanceof BigInteger) {
            ast.setType(Environment.Type.INTEGER);
        } else if (literal instanceof BigDecimal) {
            ast.setType(Environment.Type.DECIMAL);
        } else {
            ast.setType(Environment.Type.NIL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }


    @Override
    public Void visit(Ast.Expr.Binary ast) {
        // Visit the left and right expressions
        visit(ast.getLeft());
        visit(ast.getRight());
        // Ensure the two sides are assignable
        Environment.Type leftType = ast.getLeft().getType();
        Environment.Type rightType = ast.getRight().getType();
        // Check the operator and enforce type rules
        switch (ast.getOperator()) {
            case "==":
            case "!=":
                requireAssignable(leftType, rightType);
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "<":
            case ">":
            case "<=":
            case ">=":
                if (!leftType.equals(Environment.Type.COMPARABLE) || !rightType.equals(Environment.Type.COMPARABLE)) {
                    throw new RuntimeException("Operands must be comparable types.");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
            case "-":
            case "*":
            case "/":
                if (!leftType.equals(Environment.Type.INTEGER) && !leftType.equals(Environment.Type.DECIMAL)) {
                    throw new RuntimeException("Operands must be numeric types.");
                }
                ast.setType(leftType);  // Resulting type is the same as the left operand's type
                break;
            default:
                throw new RuntimeException("Unknown operator: " + ast.getOperator());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(type) || target.equals(Environment.Type.ANY)) {
            return;
        }
        if (target.equals(Environment.Type.COMPARABLE)) {
            if (type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL)
                    || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING)) {
                return;
            }
        }
        throw new RuntimeException("Type " + type.getName() + " is not assignable to " + target.getName());
    }

}
