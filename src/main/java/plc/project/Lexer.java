package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) {
            if (peek("\\s")) {
                chars.advance();  // Skip whitespace
            } else {
                tokens.add(lexToken());  // Lex a token
            }
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek("[A-Za-z_]")) {
            return lexIdentifier();  // If it's a valid start of an identifier, delegate
        } else if (peek("[0-9]")) {
            return lexNumber();  // If it's a number, delegate to lexNumber
        }
        // Add cases for other tokens (strings, operators, etc.)
        throw new ParseException("Unexpected character", chars.index);
    }

    public Token lexIdentifier() {
        if (!peek("[A-Za-z_]")) {  // identifier must start with a letter or underscore
            throw new ParseException("Invalid start of identifier", chars.index);
        }
        while (peek("[A-Za-z0-9_-]")) {
            chars.advance();  // consume valid identifier characters
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        // Start lexing from the current character index
        boolean isDecimal = false;

        // Lex the integer part
        while (peek("[0-9]")) {
            chars.advance();
        }

        // Check for a decimal point and lex the fractional part if applicable
        if (match("\\.")) {
            isDecimal = true;  // Mark this as a decimal number
            if (!peek("[0-9]")) {
                throw new ParseException("Invalid decimal number format", chars.index);
            }
            while (peek("[0-9]")) {
                chars.advance();
            }
        }

        // Emit the token as either INTEGER or DECIMAL based on the presence of a decimal point
        return isDecimal ? chars.emit(Token.Type.DECIMAL) : chars.emit(Token.Type.INTEGER);
    }


    public Token lexCharacter() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexString() {
        throw new UnsupportedOperationException(); //TODO
    }

    public void lexEscape() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (String pattern : patterns) {
            if (chars.has(0) && String.valueOf(chars.get(0)).matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        if (peek(patterns)) {
            chars.advance();
            return true;
        }
        return false;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
