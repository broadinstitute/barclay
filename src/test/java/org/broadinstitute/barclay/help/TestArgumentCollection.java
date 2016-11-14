package org.broadinstitute.barclay.help;

import org.broadinstitute.barclay.argparser.Argument;

import java.io.File;
import java.util.List;

/**
 * Test class used to test argument collection documentation.
 */
public class TestArgumentCollection {

    @Argument(fullName = "optionalStringInputFromArgCollection",
            shortName = "optionalStringInputFromArgCollection",
            doc = "Optional string input from argument collection",
            optional = true)
    public String argCollectOptionalStringInput;

    @Argument(fullName = "requiredStringInputFromArgCollection",
            shortName = "requiredStringInputFromArgCollection",
            doc = "Required string input from argument collection",
            optional = false)
    public String argCollectRequiredStringInput;

    @Argument(fullName = "requiredInputFilesFromArgCollection",
            shortName = "rRequiredInputFilesFromArgCollection",
            doc = "Required input files from argument collection",
            optional = false)
    public List<File> argCollectRequiredInputFiles;

    @Argument(fullName = "optionalInputFilesFromArgCollection",
            shortName = "optionalInputFilesFromArgCollection",
            doc = "Optional input files from argument collection",
            optional = true)
    public List<File> argCollectOptionalInputFiles;
}
