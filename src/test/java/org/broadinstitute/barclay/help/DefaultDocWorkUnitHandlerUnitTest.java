package org.broadinstitute.barclay.help;

import org.broadinstitute.barclay.argparser.TestProgramGroup;
import org.broadinstitute.barclay.help.testinputs.TestArgumentContainer;
import org.broadinstitute.barclay.help.testinputs.TestExtraDocs;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DefaultDocWorkUnitHandlerUnitTest {

    @DataProvider
    public static Object[][] workUnitDescriptions() {
        return new Object[][] {
                {DocGenMocks.createDocWorkUnit(getNewInstance(), TestArgumentContainer.class),
                        ""},
                {DocGenMocks.createDocWorkUnit(getNewInstance(), TestExtraDocs.class, DocGenMocks.mockClassDoc("Javadoc description")),
                        ""},
                // because DefaultWorkUnitHandler does not define an extra tag, the mocked class doc uses an empty tag name for the inline tag
                {DocGenMocks.createDocWorkUnit(getNewInstance(), TestExtraDocs.class, DocGenMocks
                        .mockClassDoc("Javadoc description \nin two lines", Collections.singletonMap("", "Inline tag"))),
                        "Inline tag"}
        };
    }

    @DataProvider
    public static Object[][] workUnitSummary() {
        return new Object[][] {
                {DocGenMocks.createDocWorkUnit(getNewInstance(), TestArgumentContainer.class),
                        TestArgumentContainer.ONE_LINE_SUMMARY},
                {DocGenMocks.createDocWorkUnit(getNewInstance(), TestExtraDocs.class, DocGenMocks.mockClassDoc("Javadoc description")),
                        "Javadoc description"},
                // because DefaultWorkUnitHandler does not define an extra tag, the mocked class doc uses an empty tag name for the inline tag
                {DocGenMocks.createDocWorkUnit(getNewInstance(), TestExtraDocs.class, DocGenMocks
                        .mockClassDoc("Javadoc description \nin two lines", Collections.singletonMap("", "Inline tag"))),
                        "Javadoc description in two lines"}
        };
    }

    @DataProvider
    public static Object[][] workUnitGroupName() {
        return new Object[][] {
                {DocGenMocks.createDocWorkUnit(getNewInstance(), TestArgumentContainer.class),
                        TestArgumentContainer.GROUP_NAME},
                {DocGenMocks.createDocWorkUnit(getNewInstance(), TestExtraDocs.class),
                        TestExtraDocs.GROUP_NAME}
        };
    }

    @DataProvider
    public static Object[][] workUnitGroupSummary() {
        return new Object[][] {
                {DocGenMocks.createDocWorkUnit(getNewInstance(), TestArgumentContainer.class),
                        TestProgramGroup.DESCRIPTION},
                {DocGenMocks.createDocWorkUnit(getNewInstance(), TestExtraDocs.class, DocGenMocks.mockClassDoc("Javadoc description")),
                        ""}
        };
    }

    @Test(dataProvider = "workUnitDescriptions")
    public void testGetDescription(final DocWorkUnit workUnit, final String expectedDescription) {
        Assert.assertEquals(getNewInstance().getDescription(workUnit), expectedDescription);
    }

    @Test(dataProvider = "workUnitSummary")
    public void testGetSummaryForWorkUnit(final DocWorkUnit workUnit, final String expectedDescription) {
        Assert.assertEquals(getNewInstance().getSummaryForWorkUnit(workUnit), expectedDescription);
    }

    @Test(dataProvider = "workUnitGroupName")
    public void testGetGroupNameForWorkUnit(final DocWorkUnit workUnit, final String expectedDescription) {
        Assert.assertEquals(getNewInstance().getGroupNameForWorkUnit(workUnit), expectedDescription);
    }

    @Test(dataProvider = "workUnitGroupSummary")
    public void testGetGroupSummaryForWorkUnit(final DocWorkUnit workUnit, final String expectedDescription) {
        Assert.assertEquals(getNewInstance().getGroupSummaryForWorkUnit(workUnit), expectedDescription);
    }

    private static DefaultDocWorkUnitHandler getNewInstance() {
        return new DefaultDocWorkUnitHandler(new HelpDoclet());
    }
}