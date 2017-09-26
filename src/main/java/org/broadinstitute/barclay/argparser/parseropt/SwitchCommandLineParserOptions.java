package org.broadinstitute.barclay.argparser.parseropt;

/**
 * Options to switch behaviour from the default parser.
 */
public enum SwitchCommandLineParserOptions implements CommandLineParserOption {

    /**
     * The default behavior for the parser is to:
     *
     * <p><ul>
     *     <li>Replace the contents of a collection argument with any values from the command line</li>
     *     <li>Optionally allow the special singleton value of "null" to clear the contents of the collection.</li>
     * </ul></p>
     *
     * Specifying "APPEND_TO_COLLECTIONS" changes the behavior so that any collection arguments are ADDED to the
     * initial values of the collection, and allows the special value "null" to be used first to clear the initial
     * values.
     */
    APPEND_TO_COLLECTIONS,    // default behavior is "replace"
}
