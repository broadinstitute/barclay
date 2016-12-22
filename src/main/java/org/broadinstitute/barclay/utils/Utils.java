package org.broadinstitute.barclay.utils;

import java.util.*;
import java.util.function.Supplier;

/**
 * Utility classes used by the command line parsers.
 */
public class Utils {

    private static final int TEXT_WARNING_WIDTH = 68;
    private static final String TEXT_WARNING_PREFIX = "* ";
    private static final String TEXT_WARNING_BORDER = dupString('*', TEXT_WARNING_PREFIX.length() + TEXT_WARNING_WIDTH);
    private static final char ESCAPE_CHAR = '\u001B';
    // ASCII codes for making text blink
    public static final String TEXT_BLINK = ESCAPE_CHAR + "[5m";
    public static final String TEXT_RESET = ESCAPE_CHAR + "[m";

    /**
     * Checks that an Object {@code object} is not null and returns the same object or throws an {@link IllegalArgumentException}
     * @param object any Object
     * @return the same object
     * @throws IllegalArgumentException if a {@code o == null}
     */
    public static <T> T nonNull(final T object) {
        return Utils.nonNull(object, "Null object is not allowed here.");
    }

    /**
     * Checks that an {@link Object} is not {@code null} and returns the same object or throws an {@link IllegalArgumentException}
     * @param object any Object
     * @param message the text message that would be passed to the exception thrown when {@code o == null}.
     * @return the same object
     * @throws IllegalArgumentException if a {@code o == null}
     */
    public static <T> T nonNull(final T object, final String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
        return object;
    }

    /**
     * Checks that an {@link Object} is not {@code null} and returns the same object or throws an {@link IllegalArgumentException}
     * @param object any Object
     * @param message the text message that would be passed to the exception thrown when {@code o == null}.
     * @return the same object
     * @throws IllegalArgumentException if a {@code o == null}
     */
    public static <T> T nonNull(final T object, final Supplier<String> message) {
        if (object == null) {
            throw new IllegalArgumentException(message.get());
        }
        return object;
    }

    public static List<String> warnUserLines(final String msg) {
        List<String> results = new ArrayList<>();
        results.add(String.format(TEXT_WARNING_BORDER));
        results.add(String.format(TEXT_WARNING_PREFIX + "WARNING:"));
        results.add(String.format(TEXT_WARNING_PREFIX));
        prettyPrintWarningMessage(results, msg);
        results.add(String.format(TEXT_WARNING_BORDER));
        return results;
    }

    /**
     * pretty print the warning message supplied
     *
     * @param results the pretty printed message
     * @param message the message
     */
    private static void prettyPrintWarningMessage(final List<String> results, final String message) {
        for (final String line: message.split("\\r?\\n")) {
            final StringBuilder builder = new StringBuilder(line);
            while (builder.length() > TEXT_WARNING_WIDTH) {
                int space = getLastSpace(builder, TEXT_WARNING_WIDTH);
                if (space <= 0) space = TEXT_WARNING_WIDTH;
                results.add(String.format("%s%s", TEXT_WARNING_PREFIX, builder.substring(0, space)));
                builder.delete(0, space + 1);
            }
            results.add(String.format("%s%s", TEXT_WARNING_PREFIX, builder));
        }
    }

    /**
     * Returns the last whitespace location in string, before width characters.
     * @param message The message to break.
     * @param width The width of the line.
     * @return The last whitespace location.
     */
    private static int getLastSpace(final CharSequence message, int width) {
        final int length = message.length();
        int stopPos = width;
        int currPos = 0;
        int lastSpace = -1;
        boolean inEscape = false;
        while (currPos < stopPos && currPos < length) {
            final char c = message.charAt(currPos);
            if (c == ESCAPE_CHAR) {
                stopPos++;
                inEscape = true;
            } else if (inEscape) {
                stopPos++;
                if (Character.isLetter(c))
                    inEscape = false;
            } else if (Character.isWhitespace(c)) {
                lastSpace = currPos;
            }
            currPos++;
        }
        return lastSpace;
    }

    /**
     * Create a new string thats a n duplicate copies of c
     * @param c the char to duplicate
     * @param nCopies how many copies?
     * @return a string
     */
    public static String dupString(char c, int nCopies) {
        char[] chars = new char[nCopies];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    /**
     * Compares two objects, either of which might be null.
     *
     * @param lhs One object to compare.
     * @param rhs The other object to compare.
     *
     * @return True if the two objects are equal, false otherwise.
     */
    public static boolean equals(Object lhs, Object rhs) {
        return lhs == null && rhs == null || lhs != null && lhs.equals(rhs);
    }

}
