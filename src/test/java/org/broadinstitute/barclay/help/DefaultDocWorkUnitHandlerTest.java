package org.broadinstitute.barclay.help;

import org.broadinstitute.barclay.help.testinputs.TestArgumentContainer;
import org.broadinstitute.barclay.help.testinputs.TestExtraDocs;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DefaultDocWorkUnitHandlerTest extends DocGenBaseTest {

    @DataProvider
    public Object[][] workUnitForDocStrings() throws Exception {
        return new Object[][] {
                {createDocWorkUnit(TestArgumentContainer.class),
                        "", "Argument container class for testing documentation generation.",
                        "Test feature group name", "Test program group used for testing"},
                {createDocWorkUnit(TestExtraDocs.class, mockClassDoc("Javadoc description")),
                        "", "Javadoc description",
                        "Test extra docs group name", ""},
                // TODO: because DefaultWorkUnitHandler does not have an extra tag, the mocked class doc uses an empty tag name for the inline tag
                // TODO: perhaps Barclay should include a default inline tag prefix for being able to re-use between different toolkits
                {createDocWorkUnit(TestExtraDocs.class, mockClassDoc("Javadoc description \nin two lines", Collections.singletonMap("", "Inline tag"))),
                        "Inline tag", "Javadoc description in two lines",
                        "Test extra docs group name", ""}
        };
    }

    @Test(dataProvider = "workUnitForDocStrings")
    public void testGetDocumentationStrings(final DocWorkUnit workUnit,
            final String description, final String summary,
            final String groupName, final String groupSummary) throws Exception {
        Assert.assertEquals(DEFAULT_DOC_WORK_UNIT_HANDLER.getDescription(workUnit), description);
        Assert.assertEquals(DEFAULT_DOC_WORK_UNIT_HANDLER.getSummaryForWorkUnit(workUnit), summary);
        Assert.assertEquals(DEFAULT_DOC_WORK_UNIT_HANDLER.getGroupNameForWorkUnit(workUnit), groupName);
        Assert.assertEquals(DEFAULT_DOC_WORK_UNIT_HANDLER.getGroupSummaryForWorkUnit(workUnit), groupSummary);
    }

}