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
        Environment.Function env = scope.lookupFunction("main", 0);

        if (env.getReturnType() != Environment.Type.INTEGER)
            throw new RuntimeException("Main function's return type is not an integer.");

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
        this.method = ast;
        // Create a list of parameter types
        ArrayList<Environment.Type> parameterTypes = new ArrayList<>();
        for (String typeName : ast.getParameterTypeNames()) {
            parameterTypes.add(Environment.getType(typeName));
        }
        // Get the return type of the method
        Environment.Type returnType = ast.getReturnTypeName().isPresent()
                ? Environment.getType(ast.getReturnTypeName().get())
                : Environment.Type.NIL;
        // Define the method in the current scope with its return type
        Environment.Function methodFunction = scope.defineFunction(
                ast.getName(), ast.getName(), parameterTypes, returnType, args -> Environment.NIL
        );
        ast.setFunction(methodFunction);
        // Enter a new scope for the method body
        scope = new Scope(scope);
        // Define parameters as variables in the new scope
        for (int i = 0; i < ast.getParameters().size(); i++) {
            scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), parameterTypes.get(i), Environment.NIL);
        }
        // Visit each statement in the method body
        for (Ast.Stmt stmt : ast.getStatements()) {
            visit(stmt);
        }

        // Check for missing return type in main method
        if (ast.getName().equals("main") && !ast.getReturnTypeName().isPresent()) {
            for (Ast.Stmt stmt : ast.getStatements()) {
                if (stmt instanceof Ast.Stmt.Return) {
                    throw new RuntimeException("Missing integer return type for main");
                }
            }
        }

        this.method = null;
        // Exit the scope after processing the method
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        // Visit the expression to ensure it's valid
        visit(ast.getExpression());

        if (!(ast.getExpression() instanceof Ast.Expr.Function)) {
            throw new RuntimeException("Expression is not a function.");
        }

        // Get the type of the expression
        Environment.Type exprType = ast.getExpression().getType();

        // Allow the expression to evaluate to NIL if it is a valid function like `print`
        if (exprType.equals(Environment.Type.NIL)) {
            Ast.Expr.Function funcExpr = (Ast.Expr.Function) ast.getExpression();
            if (funcExpr.getFunction().getReturnType().equals(Environment.Type.NIL)) {
                return null; // It's okay for functions like `print` to return NIL
            }
        }

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
        // Visit the condition expression
        visit(ast.getCondition());

        // Ensure the condition evaluates to a non-NIL BOOLEAN type
        if (ast.getCondition().getType().equals(Environment.Type.NIL)) {
            throw new RuntimeException("Condition expression in the IF statement must not evaluate to NIL.");
        }

        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Condition of if statement must be of type BOOLEAN.");
        }

        // Check if both 'then' and 'else' branches are empty
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("If statement must have at least one statement in the 'then' or 'else' branch.");
        }

        // Enter a new scope for the 'then' branch and visit all statements
        scope = new Scope(scope);
        for (Ast.Stmt stmt : ast.getThenStatements()) {
            visit(stmt);
        }
        scope = scope.getParent(); // Exit the 'then' scope

        // Check and visit 'else' branch if present
        if (!ast.getElseStatements().isEmpty()) {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getElseStatements()) {
                visit(stmt);
            }
            scope = scope.getParent(); // Exit the 'else' scope
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        // Visit the iterable expression
        visit(ast.getValue());

        // Check if the value's type is assignable to an iterable type
        if (!ast.getValue().getType().equals(Environment.Type.INTEGER_ITERABLE)) {
            throw new RuntimeException("The value of the for loop must be an iterable type, such as INTEGER_ITERABLE.");
        }

        if (ast.getStatements().isEmpty()) {
            throw new RuntimeException("The statements list must not be empty.");
        }

        // Enter a new scope for the loop body
        scope = new Scope(scope);

        // Define the loop variable with the element type of the iterable
        scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);

        // Visit all statements within the loop body
        for (Ast.Stmt stmt : ast.getStatements()) {
            visit(stmt);
        }

        // Exit the loop scope
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        // Visit the condition expression
        visit(ast.getCondition());
        // Ensure the condition evaluates to a boolean type
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Condition of while loop must be of type BOOLEAN.");
        }
        // Enter a new scope for the loop body
        scope = new Scope(scope);
        for (Ast.Stmt stmt : ast.getStatements()) {
            visit(stmt);
        }
        // Exit the loop scope
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        // Ensure there is a method context (method should be set when visiting the method)
        if (method == null || method.getFunction() == null) {
            throw new RuntimeException("Return statement not in a method.");
        }

        // Visit the return value expression
        visit(ast.getValue());

        // Check if the type of the return value matches the method's return type
        requireAssignable(method.getFunction().getReturnType(), ast.getValue().getType());

        return null;
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
            BigInteger value = (BigInteger) literal;
            if (value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0 || value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new RuntimeException("Integer literal is out of range.");
            }
            ast.setType(Environment.Type.INTEGER);
        } else if (literal instanceof BigDecimal) {
            BigDecimal value = (BigDecimal) literal;
            if (value.doubleValue() == Double.NEGATIVE_INFINITY || value.doubleValue() == Double.POSITIVE_INFINITY) {
                throw new RuntimeException("Double literal is out of range.");
            }
            ast.setType(Environment.Type.DECIMAL);
        } else {
            ast.setType(Environment.Type.NIL);
        }
        return null;
    }


    @Override
    public Void visit(Ast.Expr.Group ast) {
        // Visit the inner expression
        visit(ast.getExpression());

        // Ensure the inner expression is valid (not NIL unless it's allowed)
        if (ast.getExpression().getType().equals(Environment.Type.NIL)) {
            throw new RuntimeException("Grouped expression must not evaluate to NIL.");
        }

        // Ensure the contained expression is a binary expression
        if (!(ast.getExpression() instanceof Ast.Expr.Binary)) {
            throw new RuntimeException("Group expression must be a binary expression.");
        }

        // Set the type of the group to match the type of the inner expression
        ast.setType(ast.getExpression().getType());
        return null;
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
            case "<":
            case ">":
            case "<=":
            case ">=":
                requireAssignable(Environment.Type.COMPARABLE, leftType);
                requireAssignable(Environment.Type.COMPARABLE, rightType);
                requireAssignable(leftType, rightType);
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
                // Handle string concatenation
                if (leftType.equals(Environment.Type.STRING) || rightType.equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING);
                } else if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else if (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else if ((leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.DECIMAL)) ||
                        (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.INTEGER))) {
                    throw new RuntimeException("Mixed types of INTEGER and DECIMAL are not allowed for binary operations.");
                } else {
                    throw new RuntimeException("Operands must be numeric types or one must be a STRING for concatenation.");
                }
                break;
            case "-":
            case "*":
            case "/":
                if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else if (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else {
                    throw new RuntimeException("Operands must be numeric types.");
                }
                break;
            case "AND":
            case "OR":
                if (!leftType.equals(Environment.Type.BOOLEAN) || !rightType.equals(Environment.Type.BOOLEAN)) {
                    throw new RuntimeException("Operands of logical operators must be of type BOOLEAN.");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;
            default:
                throw new RuntimeException("Unknown operator: " + ast.getOperator());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            Environment.Variable variable = ast.getReceiver().get().getType().getField(ast.getName());
            if (variable == null) {
                throw new RuntimeException("Field " + ast.getName() + " is not defined.");
            }
            ast.setVariable(variable);
            // Ensure the type can be accessed through the variable
        } else {
            Environment.Variable variable = scope.lookupVariable(ast.getName());
            if (variable == null) {
                throw new RuntimeException("Variable " + ast.getName() + " is not defined.");
            }
            ast.setVariable(variable);
            // Ensure the type can be accessed through the variable
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        // Visit all argument expressions to ensure they are valid
        for (Ast.Expr arg : ast.getArguments()) {
            visit(arg);
        }

        Environment.Function function;

        if (ast.getReceiver().isPresent()) {
            // If there's a receiver, check its type for the method
            visit(ast.getReceiver().get());
            Environment.Type receiverType = ast.getReceiver().get().getType();
            function = receiverType.getMethod(ast.getName(), ast.getArguments().size());
            if (function == null) {
                throw new RuntimeException("The method " + ast.getName() + "/" + ast.getArguments().size() + " is not defined in this scope.");
            }
        } else {
            // Lookup the function in the current scope using its name and argument count
            function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            if (function == null) {
                throw new RuntimeException("The function " + ast.getName() + "/" + ast.getArguments().size() + " is not defined in this scope.");
            }
        }

        // Verify that the argument types match the function's parameter types
        for (int i = 0; i < ast.getArguments().size(); i++) {
            requireAssignable(function.getParameterTypes().get(i), ast.getArguments().get(i).getType());
        }

        // Set the function in the AST node
        ast.setFunction(function);

        return null;
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