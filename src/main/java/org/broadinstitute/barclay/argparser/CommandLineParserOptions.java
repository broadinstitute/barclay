package org.broadinstitute.barclay.argparser;

/**
 * Options used to control command line parser behavior.
 */
public enum CommandLineParserOptions  {

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

    /**
     * The default behavior for the parser is to load options for argument collections from a file
     * if it ends with {@link CommandLineArgumentParser#COLLECTION_LIST_FILE_EXTENSION}.
     *
     * Specifying "DO_NOT_EXPAND_COLLECTION_LIST_FILE" changes the behavior so that any collection arguments are
     * treated as values independently of the format.
     */
    DO_NOT_EXPAND_COLLECTION_LIST_FILE // default behavior is "expand"

}
