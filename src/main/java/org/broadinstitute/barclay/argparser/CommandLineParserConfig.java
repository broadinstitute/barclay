package org.broadinstitute.barclay.argparser;

import java.util.Collection;
import java.util.Collections;

/**
 * Interface for the command line parser configuration.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface CommandLineParserConfig {

    /**
     * Default extension for collection argument list files.
     */
    public static final String DEFAULT_COLLECTION_LIST_FILE_EXTENSION = ".args";

    /**
     * Configuration for append/replace values in a Collection argument.
     *
     * <p>The default behavior is returning {@code false}, which:
     *
     * <p><ul>
     *     <li>Replace the contents of a collection argument with any values from the command line.</li>
     *     <li>Optionally allow the special singleton value of "null" to clear the contents of the collection.</li>
     * </ul></p>
     *
     * <p>Returning {@code true} changes the behavior so that any collection arguments are ADDED to the
     * initial values of the collection, and allows the special value "null" to be used first to clear the initial
     * values.
     *
     * @return {@code true} if the result should be append to the initial values of the collection;
     * {@code false} if they should be substituted.
     */
    default public boolean getAppendToCollections() {
        return false;
    }

    /**
     * Configuration for loading Collection arguments from files ending with concrete extensions.
     *
     * <p>Default behaviour returns {@link #DEFAULT_COLLECTION_LIST_FILE_EXTENSION} as the file
     * extension to expand.
     *
     * @return extension(s) for detect an argument as a file for loading a Collection argument.
     */
    default Collection<String> getExpansionFileExtensions() {
        return Collections.singleton(DEFAULT_COLLECTION_LIST_FILE_EXTENSION);
    }

}
