package org.broadinstitute.barclay.help;

import org.broadinstitute.barclay.argparser.Argument;

/**
 * Class for testing extraDocs property in docgen.
 */
@DocumentedFeature(groupName = "Test extra docs group name")
public class TestExtraDocs {

    @Argument(fullName = "extraDocsArgument",
            shortName = "extDocArg",
            doc = "Extra stuff",
            optional = true)
    public String optionalFileList = "initial string value";

}
