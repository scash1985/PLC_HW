package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        Environment.PlcObject value = ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL;
        scope.defineVariable(ast.getName(), value);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope methodScope = new Scope(scope);
            for (int i = 0; i < ast.getParameters().size(); i++) {
                methodScope.defineVariable(ast.getParameters().get(i), args.get(i));
            }
            try {
                scope = methodScope;
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } catch (Return returnValue) {
                return returnValue.value;
            } finally {
                scope = scope.getParent();
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        if(ast.getValue().isPresent()){
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        } else{
            scope.defineVariable((ast.getName()), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        if (ast.getReceiver() instanceof Ast.Expr.Access) {
            Ast.Expr.Access access = (Ast.Expr.Access) ast.getReceiver();

            // Check if receiver is present
            if (access.getReceiver().isPresent()) {
                Environment.PlcObject receiver = visit(access.getReceiver().get());
                Environment.Variable variable = receiver.getField(access.getName());
                variable.setValue(visit(ast.getValue()));
            } else {
                // If no receiver, the variable is in the current scope
                Environment.Variable variable = scope.lookupVariable(access.getName());
                if (variable == null) {
                    throw new RuntimeException("Variable '" + access.getName() + "' is not defined.");
                }
                variable.setValue(visit(ast.getValue()));
            }

            return Environment.NIL;
        } else {
            throw new RuntimeException("Receiver is not a valid access expression.");
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        if (requireType(Boolean.class, visit(ast.getCondition()))) {
            for (Ast.Stmt stmt : ast.getThenStatements()) {
                visit(stmt);
            }
        } else {
            for (Ast.Stmt stmt : ast.getElseStatements()) {
                visit(stmt);
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        Environment.PlcObject iterable = visit(ast.getValue());
        List<Environment.PlcObject> list = requireType(List.class, iterable);

        for (Environment.PlcObject element : list) {
            // Create a new scope for each iteration
            Scope iterationScope = new Scope(scope);

            // Define the variable within the new scope
            iterationScope.defineVariable(ast.getName(), element);

            try {
                scope = iterationScope;  // Use the new scope
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);  // Visit the statements within this scope
                }
            } finally {
                scope = scope.getParent();  // Reset the scope back to the parent after iteration
            }
        }

        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                // Remove the scope creation inside the loop
                //scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                // Don't reset the scope at the end of each loop iteration
                //scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        Environment.PlcObject left = visit(ast.getLeft());
        Environment.PlcObject right = visit(ast.getRight());

        switch (ast.getOperator()) {
            case "+":
                if (left.getValue() instanceof String || right.getValue() instanceof String) {
                    return Environment.create(requireType(String.class, left) + requireType(String.class, right));
                } else if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                    return Environment.create(requireType(BigInteger.class, left).add(requireType(BigInteger.class, right)));
                } else if (left.getValue() instanceof BigDecimal || right.getValue() instanceof BigDecimal) {
                    // Convert BigInteger to BigDecimal if necessary
                    BigDecimal leftDecimal = (left.getValue() instanceof BigInteger)
                            ? new BigDecimal((BigInteger) left.getValue())
                            : requireType(BigDecimal.class, left);
                    BigDecimal rightDecimal = (right.getValue() instanceof BigInteger)
                            ? new BigDecimal((BigInteger) right.getValue())
                            : requireType(BigDecimal.class, right);
                    return Environment.create(leftDecimal.add(rightDecimal));
                }
                break;
            case "-":
                if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                    return Environment.create(requireType(BigInteger.class, left).subtract(requireType(BigInteger.class, right)));
                } else {
                    BigDecimal leftDecimal = (left.getValue() instanceof BigInteger)
                            ? new BigDecimal((BigInteger) left.getValue())
                            : requireType(BigDecimal.class, left);
                    BigDecimal rightDecimal = (right.getValue() instanceof BigInteger)
                            ? new BigDecimal((BigInteger) right.getValue())
                            : requireType(BigDecimal.class, right);
                    return Environment.create(leftDecimal.subtract(rightDecimal));
                }
            case "*":
                if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                    return Environment.create(requireType(BigInteger.class, left).multiply(requireType(BigInteger.class, right)));
                } else {
                    BigDecimal leftDecimal = (left.getValue() instanceof BigInteger)
                            ? new BigDecimal((BigInteger) left.getValue())
                            : requireType(BigDecimal.class, left);
                    BigDecimal rightDecimal = (right.getValue() instanceof BigInteger)
                            ? new BigDecimal((BigInteger) right.getValue())
                            : requireType(BigDecimal.class, right);
                    return Environment.create(leftDecimal.multiply(rightDecimal));
                }
            case "/":
                BigDecimal leftDecimal = (left.getValue() instanceof BigInteger)
                        ? new BigDecimal((BigInteger) left.getValue())
                        : requireType(BigDecimal.class, left);
                BigDecimal rightDecimal = (right.getValue() instanceof BigInteger)
                        ? new BigDecimal((BigInteger) right.getValue())
                        : requireType(BigDecimal.class, right);
                if (rightDecimal.compareTo(BigDecimal.ZERO) == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                return Environment.create(leftDecimal.divide(rightDecimal, 10, RoundingMode.HALF_UP).setScale(1, RoundingMode.HALF_UP));
            case "OR":
            case "||":
                // Short-circuit: If left is true, return true without evaluating right
                if (requireType(Boolean.class, left)) {
                    return Environment.create(true);
                }
                return Environment.create(requireType(Boolean.class, right));
            case "AND":
            case "&&":
                // Short-circuit: If left is false, return false without evaluating right
                if (!requireType(Boolean.class, left)) {
                    return Environment.create(false);
                }
                return Environment.create(requireType(Boolean.class, right));
            case "<":
                return Environment.create(requireType(BigInteger.class, left).compareTo(requireType(BigInteger.class, right)) < 0);
            case ">=":
                return Environment.create(requireType(BigInteger.class, left).compareTo(requireType(BigInteger.class, right)) >= 0);
            case "==":
                return Environment.create(left.getValue().equals(right.getValue()));
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + ast.getOperator());
        }
        throw new UnsupportedOperationException("Invalid types for operator: " + ast.getOperator());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.getField(ast.getName()).getValue();
        } else {
            Environment.Variable variable = scope.lookupVariable(ast.getName());
            if (variable != null) {
                return variable.getValue();
            } else {
                throw new RuntimeException("Variable '" + ast.getName() + "' is not defined.");
            }
        }
    }



    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        List<Environment.PlcObject> arguments = new ArrayList<>();
        for (Ast.Expr argument : ast.getArguments()) {
            arguments.add(visit(argument));
        }

        if (ast.getReceiver().isPresent()) {
            // Instance method call: Get the receiver and call the method on it.
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.callMethod(ast.getName(), arguments);
        } else {
            // Regular function call: Lookup the function in the current scope.
            Environment.Function function = scope.lookupFunction(ast.getName(), arguments.size());
            return function.invoke(arguments);
        }
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
