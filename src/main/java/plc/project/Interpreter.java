package plc.project;

import java.math.BigInteger;
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
        Environment.Variable variable;
        if (ast.getReceiver() instanceof Ast.Expr.Access) {
            Ast.Expr.Access access = (Ast.Expr.Access) ast.getReceiver();
            variable = scope.lookupVariable(access.getName());
        } else {
            throw new RuntimeException("Receiver is not a valid variable.");
        }
        variable.setValue(visit(ast.getValue()));
        return Environment.NIL;
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
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
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
                return Environment.create(requireType(BigInteger.class, left).add(requireType(BigInteger.class, right)));
            case "-":
                return Environment.create(requireType(BigInteger.class, left).subtract(requireType(BigInteger.class, right)));
            case "*":
                return Environment.create(requireType(BigInteger.class, left).multiply(requireType(BigInteger.class, right)));
            case "/":
                return Environment.create(requireType(BigInteger.class, left).divide(requireType(BigInteger.class, right)));
            case "&&":
                return Environment.create(requireType(Boolean.class, left) && requireType(Boolean.class, right));
            case "||":
                return Environment.create(requireType(Boolean.class, left) || requireType(Boolean.class, right));
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + ast.getOperator());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            // If the receiver is present, it’s a field access.
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.getField(ast.getName()).getValue();
        } else {
            // Otherwise, it's a variable access.
            return scope.lookupVariable(ast.getName()).getValue();
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
