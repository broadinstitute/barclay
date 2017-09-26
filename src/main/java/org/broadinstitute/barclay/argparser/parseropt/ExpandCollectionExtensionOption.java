package org.broadinstitute.barclay.argparser.parseropt;

import org.broadinstitute.barclay.utils.Utils;

import java.util.Collection;
import java.util.Collections;

/**
 * Option for loading argument collections from files ending with concrete extensions.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class ExpandCollectionExtensionOption implements CommandLineParserOption {

    /**
     * Default xtension for collection argument list files.
     */
    public static final String DEFAULT_COLLECTION_LIST_FILE_EXTENSION = ".args";

    private final Collection<String> extensions;

    /** Constructor for a collection of extensions. */
    public ExpandCollectionExtensionOption(final Collection<String> extensions) {
        this.extensions = Utils.nonNull(extensions);
    }

    /**
     * Default constructor uses {@link #DEFAULT_COLLECTION_LIST_FILE_EXTENSION}.
     */
    public ExpandCollectionExtensionOption() {
        this(Collections.singleton(DEFAULT_COLLECTION_LIST_FILE_EXTENSION));
    }

    /**
     * Load options for argument collections from files if the argument value ends with one of the
     * provided extensions.
     */
    public Collection<String> getExtensions() {
        return extensions;
    }
}
