package org.broadinstitute.barclay.help.testinputs;

import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ExperimentalFeature;
import org.broadinstitute.barclay.help.DocumentedFeature;

/**
 * Class for testing extraDocs property in docgen.
 */
@DocumentedFeature(groupName = TestExtraDocs.GROUP_NAME)
@ExperimentalFeature
public class TestExtraDocs {

    public static final String GROUP_NAME = "Test extra docs group name";

    @Argument(fullName = "extraDocsArgument",
            shortName = "extDocArg",
            doc = "Extra stuff",
            optional = true)
    public String optionalFileList = "initial string value";

}
