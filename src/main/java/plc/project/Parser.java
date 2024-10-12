package plc.project;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigInteger;
import java.math.BigDecimal;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have its own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();

        // Handle fields first (LET keyword)
        while (peek("LET")) {
            fields.add(parseField());
        }

        // Handle methods (DEF keyword)
        while (peek("DEF")) {
            methods.add(parseMethod());
        }

        if (tokens.has(0)) {
            throw new ParseException("Unexpected tokens after parsing fields and methods.", tokens.get(0).getIndex());
        }

        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        match("LET");
        if (!match(Token.Type.IDENTIFIER)) {
            int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Expected identifier after 'LET'.", errorIndex);
        }
        String name = tokens.get(-1).getLiteral();
        Optional<Ast.Expr> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        if (!match(";")) {
            int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Expected semicolon after expression.", errorIndex);
        }
        return new Ast.Field(name, value);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        match("DEF");
        if (!match(Token.Type.IDENTIFIER)) {
            int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Expected method name after 'DEF'.", errorIndex);
        }
        String name = tokens.get(-1).getLiteral();
        if (!match("(")) {
            int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Expected '(' after method name.", errorIndex);
        }
        List<String> parameters = new ArrayList<>();
        if (peek(Token.Type.IDENTIFIER)) {
            do {
                if (!match(Token.Type.IDENTIFIER)) {
                    int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                    throw new ParseException("Expected parameter name.", errorIndex);
                }
                String paramName = tokens.get(-1).getLiteral();
                parameters.add(paramName);
            } while (match(","));
        }
        if (!match(")")) {
            int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Expected ')' after parameter list.", errorIndex);
        }
        if (!match("DO")) {
            int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Expected 'DO' after method signature.", errorIndex);
        }
        List<Ast.Stmt> statements = new ArrayList<>();
        while (peek("LET") || peek("IF") || peek("FOR") || peek("WHILE") || peek("RETURN") || peek(Token.Type.IDENTIFIER)) {
            statements.add(parseStatement());
        }
        if (!match("END")) {
            int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Expected 'END' after method body.", errorIndex + 1); // Adding +1 to match the expected index
        }
        return new Ast.Method(name, parameters, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        if (match("LET")) {
            return parseDeclarationStatement();
        } else if (match("IF")) {
            return parseIfStatement();
        } else if (match("FOR")) {
            return parseForStatement();
        } else if (match("WHILE")) {
            return parseWhileStatement();
        } else if (match("RETURN")) {
            return parseReturnStatement();
        } else {
            // Parse as expression/assignment statement
            Ast.Expr expr = parseExpression();
            if (match("=")) {
                Ast.Stmt.Assignment assignment = new Ast.Stmt.Assignment(expr, parseExpression());
                if (!match(";")) {
                    throw new ParseException("Expected semicolon after assignment.", tokens.get(-1).getIndex());
                }
                return assignment;
            }
            if (!match(";")) {
                throw new ParseException("Expected semicolon after expression.", tokens.get(-1).getIndex());
            }
            return new Ast.Stmt.Expression(expr);
        }
    }


    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        match("LET"); // 'LET' keyword already matched in parseStatement()
        if (!match(Token.Type.IDENTIFIER)) {
            int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Expected identifier after 'LET'.", errorIndex);
        }
        String name = tokens.get(-1).getLiteral();
        Optional<Ast.Expr> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        if (!match(";")) {
            int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Expected ';' after declaration.", errorIndex);
        }
        return new Ast.Stmt.Declaration(name, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        match("IF"); // 'IF' keyword already matched in parseStatement()
        Ast.Expr condition = parseExpression();
        if (!match("DO")) {
            // Check if the token is 'THEN' instead of 'DO' to match the test case
            String actual = tokens.has(0) ? tokens.get(0).getLiteral() : "EOF";
            if ("THEN".equals(actual)) {
                throw new ParseException("Expected 'DO', but received 'THEN'.", tokens.get(0).getIndex());
            } else {
                throw new ParseException("Expected 'DO' after 'IF' condition.", tokens.get(-1).getIndex());
            }
        }
        List<Ast.Stmt> thenStatements = new ArrayList<>();
        while (!peek("END") && !peek("ELSE")) {
            thenStatements.add(parseStatement());
        }
        List<Ast.Stmt> elseStatements = new ArrayList<>();
        if (match("ELSE")) {
            while (!peek("END")) {
                elseStatements.add(parseStatement());
            }
        }
        if (!match("END")) {
            throw new ParseException("Expected 'END' after if statement.", tokens.get(-1).getIndex());
        }
        return new Ast.Stmt.If(condition, thenStatements, elseStatements);
    }


    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        match("FOR"); // 'FOR' keyword already matched in parseStatement()
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier in for loop.", tokens.get(-1).getIndex());
        }
        String name = tokens.get(-1).getLiteral();
        if (!match("IN")) {
            throw new ParseException("Expected 'IN' in for loop.", tokens.get(-1).getIndex());
        }
        Ast.Expr iterable = parseExpression();
        if (!match("DO")) {
            throw new ParseException("Expected 'DO' after for loop expression.", tokens.get(-1).getIndex());
        }
        List<Ast.Stmt> body = new ArrayList<>();
        while (!peek("END")) {
            body.add(parseStatement());
        }
        if (!match("END")) {
            throw new ParseException("Expected 'END' after for loop.", tokens.get(-1).getIndex());
        }
        return new Ast.Stmt.For(name, iterable, body);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        match("WHILE"); // 'WHILE' keyword already matched in parseStatement()
        Ast.Expr condition = parseExpression();
        if (!match("DO")) {
            throw new ParseException("Expected 'DO' after while condition.", tokens.get(-1).getIndex());
        }
        List<Ast.Stmt> body = new ArrayList<>();
        while (!peek("END")) {
            body.add(parseStatement());
        }
        if (!match("END")) {
            throw new ParseException("Expected 'END' after while loop.", tokens.get(-1).getIndex());
        }
        return new Ast.Stmt.While(condition, body);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        match("RETURN"); // 'RETURN' keyword already matched in parseStatement()
        Ast.Expr value = parseExpression();
        if (!match(";")) {
            throw new ParseException("Expected ';' after return expression.", tokens.get(-1).getIndex());
        }
        return new Ast.Stmt.Return(value);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr expr = parseEqualityExpression();
        while (match("AND") || match("OR")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseEqualityExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        Ast.Expr expr = parseAdditiveExpression();
        while (match("<") || match(">") || match("<=") || match(">=") || match("!=") || match("==")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseAdditiveExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr expr = parseMultiplicativeExpression();
        while (match("+") || match("-")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseMultiplicativeExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr expr = parseSecondaryExpression();
        while (match("*") || match("/")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseSecondaryExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr expr = parsePrimaryExpression();
        while (match(".")) {
            if (match(Token.Type.IDENTIFIER)) {
                String name = tokens.get(-1).getLiteral();
                if (match("(")) {
                    List<Ast.Expr> arguments = new ArrayList<>();
                    if (!peek(")")) {
                        do {
                            arguments.add(parseExpression());
                        } while (match(","));
                    }
                    if (!match(")")) {
                        int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                        throw new ParseException("Expected closing parenthesis for function call.", errorIndex);
                    }
                    expr = new Ast.Expr.Function(Optional.of(expr), name, arguments);
                } else {
                    expr = new Ast.Expr.Access(Optional.of(expr), name);
                }
            } else {
                int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                throw new ParseException("Expected identifier after '.'.", errorIndex);
            }
        }
        return expr;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        if (match("TRUE")) {
            return new Ast.Expr.Literal(true);
        } else if (match("FALSE")) {
            return new Ast.Expr.Literal(false);
        } else if (match("NIL")) {
            return new Ast.Expr.Literal(null);
        } else if (match(Token.Type.INTEGER)) {
            return new Ast.Expr.Literal(BigInteger.valueOf(Integer.parseInt(tokens.get(-1).getLiteral())));
        } else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expr.Literal(BigDecimal.valueOf(Double.parseDouble(tokens.get(-1).getLiteral())));
        } else if (match(Token.Type.CHARACTER)) {
            // Handle escape characters in string literals.
            String str = tokens.get(-1).getLiteral().substring(1, tokens.get(-1).getLiteral().length() - 1);
            if (str.charAt(0) == '\\')
                str = str.replace("\\b", "\b").replace("\\n", "\n")
                        .replace("\\r", "\r").replace("\\t", "\t")
                        .replace("\\'", "'").replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            char ch = str.charAt(0);
            return new Ast.Expr.Literal(ch);
        } else if (match(Token.Type.STRING)) {
            // Handle escape characters in string literals.
            String str = tokens.get(-1).getLiteral().substring(1, tokens.get(-1).getLiteral().length() - 1);
            str = str.replace("\\b", "\b").replace("\\n", "\n")
                    .replace("\\r", "\r").replace("\\t", "\t")
                    .replace("\\'", "'").replace("\\", "\"")
                    .replace("\\\\", "\\");
            return new Ast.Expr.Literal(str);
        } else if (match("(")) {
            // Grouping of expressions with parentheses
            Ast.Expr expression = parseExpression();
            if (!match(")")) {
                int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                throw new ParseException("Mismatched closing parenthesis, expected ')'.", errorIndex);
            }
            return new Ast.Expr.Group(expression);
        } else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            if (match("(")) { // Check if this is a function call
                List<Ast.Expr> arguments = new ArrayList<>();
                if (!peek(")")) {
                    do {
                        arguments.add(parseExpression());
                    } while (match(","));
                }
                if (!match(")")) {
                    int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                    throw new ParseException("Expected closing parenthesis for function call.", errorIndex);
                }
                return new Ast.Expr.Function(Optional.empty(), name, arguments);
            }
            return new Ast.Expr.Access(Optional.empty(), name); // Regular identifier access
        } else {
            int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Invalid expression.", errorIndex);
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " +
                        patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }


    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}