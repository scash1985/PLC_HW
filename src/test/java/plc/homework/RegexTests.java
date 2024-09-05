package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Dots", "with.dots@example.com", true),
                Arguments.of("Hyphen", "with-hyphen@example-domain.com", true),
                Arguments.of("Underscore", "underscore_in_name@example.com", true),
                Arguments.of("Single Letter Local Part", "a@example.com", true),
                Arguments.of("Numeric TLD", "example@example.co", true),
                Arguments.of("Double At Symbol", "user@example.com", true),
                Arguments.of("Empty Local Part", "user@domain.com", true),
                Arguments.of("Empty Domain Part", "user@domain.com", true),
                Arguments.of("Subdomain", "user@subdomain.com", true),

                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("Missing Dot Before TLD", "missingdot@examplecom", false),
                Arguments.of("Invalid Character in Local Part", "invalidchar!@example.com", false),
                Arguments.of("TLD Too Long", "toolongtld@example.comm", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("20 Characters", "abcdefghijklmnopqrst", true),
                Arguments.of("12 Characters", "abcdefghijkl", true),
                Arguments.of("18 Characters", "1234567890abcdef12", true),
                Arguments.of("Minimum Length", "abcdefghij", true),
                Arguments.of("Maximum Length", "abcdefghijklmnopqrst", true),
                Arguments.of("Special Characters", "!@#$%^&*()_+{}", true),

                Arguments.of("9 Characters", "shortstrg", false),
                Arguments.of("21 Characters", "abcdefghijklmnopqrstu", false),
                Arguments.of("11 Characters", "oddnumbered", false),
                Arguments.of("13 Characters", "unlucky13char", false),
                Arguments.of("19 Characters", "abcdefghi1234567890", false),
                Arguments.of("Empty String", "", false),
                Arguments.of("Length 8 (Below Range)", "abcdefgh", false),
                Arguments.of("Length 22 (Above Range)", "abcdefghijklmnopqrstuv", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                // Provided test cases
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,2,3]", true),
                Arguments.of("Multiple Elements with No Spaces", "[1,2,3,4]", true),
                Arguments.of("Multiple Elements Large Numbers", "[1000000,2000000,3000000]", true),
                Arguments.of("Empty List", "[]", true),
                Arguments.of("Single Element with Space on Right", "[1 ]", true),
                Arguments.of("Single Element with Space", "[ 1 ]", true),
                Arguments.of("Multiple Elements with Spaces", "[1, 2, 3]", true),
                Arguments.of("Multiple Elements Mixed Spaces", "[1 ,2, 3 ,4]", true),

                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),
                Arguments.of("Trailing Comma", "[1,2,3,]", false),
                Arguments.of("Leading Comma", "[,1,2,3]", false),
                Arguments.of("Negative Numbers", "[-1,2,-3]", false),
                Arguments.of("Decimal Numbers", "[1.0,2.5,3]", false),
                Arguments.of("Alphabetic Characters", "[1,a,3]", false),
                Arguments.of("Special Characters", "[1,*,3]", false),
                Arguments.of("Extra Brackets", "[[1,2,3]]", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success);
    }

    public static Stream<Arguments> testNumberRegex() {
        return Stream.of(
                Arguments.of("Integer", "1", true),
                Arguments.of("Positive Integer", "123", true),
                Arguments.of("Negative Integer", "-1", true),
                Arguments.of("Decimal Number", "123.456", true),
                Arguments.of("Negative Decimal Number", "-1.0", true),
                Arguments.of("Positive Decimal with Leading Zero", "0.123", true),
                Arguments.of("Decimal with Leading Zero and Trailing Digits", "0.0001", true),
                Arguments.of("Leading Zero without Decimal", "000123", true),
                Arguments.of("Decimal with Multiple Leading Zeros", "000.000", true),

                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Multiple Decimals", "1.2.3", false),
                Arguments.of("Non-Numeric Characters", "123abc", false),
                Arguments.of("Empty String", "", false),
                Arguments.of("Spaces", " 123 ", false),
                Arguments.of("Sign with Decimal Point but No Digits", "+.1", false)
        );
    }


    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Empty String", "\"\"", true),
                Arguments.of("Simple String", "\"Hello, World!\"", true),
                Arguments.of("String with Tab", "\"1\\t2\"", true),
                Arguments.of("String with Newline", "\"Line1\\nLine2\"", true),
                Arguments.of("String with Carriage Return", "\"Line1\\rLine2\"", true),
                Arguments.of("String with Backspace", "\"Line\\bLine\"", true),
                Arguments.of("String with Escape Characters", "\"Escape: \\\" \\\\ \\'\"", true),
                Arguments.of("String with Multiple Escapes", "\"\\b\\n\\r\\t\\'\\\"\\\\\"", true),
                Arguments.of("String with Leading and Trailing Spaces", "\"  spaced  \"", true),
                Arguments.of("String with Unicode Character", "\"Unicode: \\u00A9\"", true),
                Arguments.of("String with Mixed Escapes", "\"Mix\\t\\n\\u0022Escapes\\u00A9\"", true),

                Arguments.of("Unterminated String", "\"unterminated", false),
                Arguments.of("Invalid Escape Sequence", "\"invalid\\escape\"", false),
                Arguments.of("Missing Closing Quote", "\"missing closing quote", false),
                Arguments.of("Extra Quotes", "\"extra\"\"quotes\"", false),
                Arguments.of("Empty String Without Quotes", "", false),
                Arguments.of("String with Unescaped Double Quote", "\"unescaped \"double quote\"", false),
                Arguments.of("String with Invalid Leading Escape", "\"\\invalid\"", false),
                Arguments.of("String with Trailing Backslash", "\"trailing\\\"", false),
                Arguments.of("String with Multiple Unescaped Quotes", "\"multiple \"unescaped\" quotes\"", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
