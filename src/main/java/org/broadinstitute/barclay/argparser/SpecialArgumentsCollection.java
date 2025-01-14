package org.broadinstitute.barclay.argparser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This collection is for arguments that require special treatment by the arguments parser itself.
 * It should not grow beyond a very short list.
 */
public final class SpecialArgumentsCollection {
    public static final String HELP_FULLNAME = "help";

    public static final String SHOW_HIDDEN_FULLNAME = "show-hidden";
    @Deprecated
    public static final String SHOW_HIDDEN_DEPRECATED = "showHidden";

    public static final String VERSION_FULLNAME = "version";

    public static final String ARGUMENTS_FILE_FULLNAME = "arguments-file";
    @Deprecated
    public static final String ARGUMENTS_FILE_DEPRECATED = "arguments_file";

    
    private static final long serialVersionUID = 1L;

    @Argument(shortName = "h", fullName = HELP_FULLNAME, doc= "display the help message", special = true)
    public boolean HELP = false;

    @Argument(fullName = VERSION_FULLNAME, doc="display the version number for this tool", special = true)
    public boolean VERSION = false;

    @Argument(fullName = ARGUMENTS_FILE_FULLNAME, shortName = ARGUMENTS_FILE_DEPRECATED, doc="read one or more arguments files and add them to the command line", optional = true, special = true)
    public List<File> ARGUMENTS_FILE = new ArrayList<>();

    @Advanced
    @Argument(fullName = SHOW_HIDDEN_FULLNAME, shortName = SHOW_HIDDEN_DEPRECATED, doc = "display hidden arguments", special = true)
    public boolean SHOW_HIDDEN = false;
}
