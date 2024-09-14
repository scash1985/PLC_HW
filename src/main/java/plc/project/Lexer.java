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
            if (peek("[\\s\\u0008]")) { //check if next char is whitespace
                chars.advance();  //skip whitespace
                chars.skip();  // reset length back to 0
            } else {
                tokens.add(lexToken());  //call lexToken to identify token
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
            return lexIdentifier();  // If char is a letter or underscore, call lexIdentifier
        } else if (peek("[+-]") && chars.has(1) && String.valueOf(chars.get(1)).matches("[0-9]")) {
            // Check for a number starting with + or -
            return lexNumber();
        } else if (peek("[0-9]")) {
            return lexNumber();  // If char is a number, call lexNumber
        } else if (peek("\"")) {
            return lexString();  // If char is a double quote, call lexString
        } else if (peek("'")) {
            return lexCharacter();  // If char is a single quote, call lexCharacter
        } else {
            return lexOperator();
        }
    }

    public Token lexIdentifier() {
        if (!peek("[A-Za-z_]")) {  //throws exception if char doesn't start with a letter or underscore
            throw new ParseException("Invalid start of identifier", chars.index);
        }
        while (peek("[A-Za-z0-9_-]")) {
            chars.advance();
        }
        return chars.emit(Token.Type.IDENTIFIER); //creates token of type IDENTIFIER
    }

    public Token lexNumber() {
        boolean isDecimal = false;

        // handle optional leading + or -
        if (peek("[+-]")) {
            chars.advance();
        }

        // handle the number part
        while (peek("[0-9]")) {
            chars.advance();
        }

        if (match("\\.")) {
            isDecimal = true;
            if (!peek("[0-9]")) {
                throw new ParseException("Invalid decimal number format", chars.index);
            }
            while (peek("[0-9]")) {
                chars.advance();
            }
        }

        // emit a DECIMAL or INTEGER token based on whether a decimal point was found
        return isDecimal ? chars.emit(Token.Type.DECIMAL) : chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        // check for opening single quote
        if (!match("'")) {
            throw new ParseException("Expected opening single quote for character literal", chars.index);
        }

        // Handle the character or escape sequence
        if (peek("\\\\")) {
            lexEscape(); // handle escape sequences
        } else if (peek("[^'\n\r]")) {
            chars.advance();
        } else {
            throw new ParseException("Invalid character in character literal", chars.index);
        }

        // Ensure there is only one character
        if (chars.has(0) && peek("[^'\\n\\r\\\\]")) {
            // advance to check if another character is present
            chars.advance();
            if (!peek("'")) {
                throw new ParseException("Character literal contains more than one character", chars.index);
            }
        } else if (peek("\\\\")) {
            // if we saw an escape sequence, check if another character is present
            if (!peek("'")) {
                throw new ParseException("Character literal contains more than one character", chars.index);
            }
        }

        if (!match("'")) {
            throw new ParseException("Expected closing single quote for character literal", chars.index);
        }

        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        // check for opening double quote
        if (!match("\"")) {
            throw new ParseException("Expected opening double quote for string literal", chars.index);
        }

        // Handle the characters in the string
        while (peek("[^\"\\n\\r]")) {
            if (peek("\\\\"))
                lexEscape();
            chars.advance();
        }

        // check for closing double quote
        if (!match("\"")) {
            throw new ParseException("Expected closing double quote for string literal", chars.index);
        }

        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        if (match("\\\\")) {
            if (!peek("[bnrt'\"\\\\]")) {
                throw new ParseException("Invalid escape sequence", chars.index);
            }
            chars.advance();
        } else {
            throw new ParseException("Expected escape sequence after backslash", chars.index);
        }
    }

    public Token lexOperator() {
        if (peek("<", "=") || peek(">", "=") || peek("!", "=") || peek("=", "=")) {
            if (match("<", "=") || match(">", "=") || match("!", "=") || match("=", "=")) {
                return chars.emit(Token.Type.OPERATOR);
            }
        } else {
            chars.advance();
        }

        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        if (peek(patterns)) {
            for (String pattern : patterns)
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