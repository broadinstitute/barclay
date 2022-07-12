package org.broadinstitute.barclay.help.testinputs;

import org.broadinstitute.barclay.argparser.Advanced;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.argparser.DeprecatedFeature;
import org.broadinstitute.barclay.argparser.TestProgramGroup;
import org.broadinstitute.barclay.help.DocumentedFeature;

/**
 * A test tool for testing a deprecated (@DeprecatedFeature) CLP.
 */
@CommandLineProgramProperties(
        summary = TestDeprecatedCLP.SUMMARY,
        oneLineSummary = TestDeprecatedCLP.ONE_LINE_SUMMARY,
        programGroup = TestProgramGroup.class)
@DeprecatedFeature
@DocumentedFeature(groupName = TestArgumentContainer.GROUP_NAME, extraDocs = TestExtraDocs.class)
public class TestDeprecatedCLP {

    public static final String SUMMARY = "Test tool summary for deprecated tool.";
    public static final String ONE_LINE_SUMMARY = "Argument container class for testing deprecated CLPs.";
    public static final String GROUP_NAME = "Test feature group name";

    /**
     * Optional int
     */
    @Advanced
    @Argument(fullName = "optionalInt",
            shortName = "optInt",
            doc = "optionalInt with initial value 1", optional = true)
    protected int optionalInt = 1;

}
