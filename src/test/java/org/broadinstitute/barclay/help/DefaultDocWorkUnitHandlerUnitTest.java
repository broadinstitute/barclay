package org.broadinstitute.barclay.help;

import com.sun.javadoc.ClassDoc;
import org.broadinstitute.barclay.argparser.TestProgramGroup;
import org.broadinstitute.barclay.help.testinputs.TestArgumentContainer;
import org.broadinstitute.barclay.help.testinputs.TestExtraDocs;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DefaultDocWorkUnitHandlerUnitTest {

    @DataProvider
    public static Object[][] workUnitDescriptions() {
        return new Object[][] {
                {TestArgumentContainer.class, "", Collections.EMPTY_MAP, ""},
                {TestExtraDocs.class, "Javadoc description", Collections.EMPTY_MAP, ""},
                {TestExtraDocs.class, "Javadoc description \nin two lines", Collections.singletonMap("", "Inline tag"), "Inline tag"}
        };
    }

    @DataProvider
    public static Object[][] workUnitSummary() {
        return new Object[][] {
                {TestArgumentContainer.class, "", Collections.EMPTY_MAP, TestArgumentContainer.ONE_LINE_SUMMARY},
                {TestExtraDocs.class, "Javadoc description", Collections.EMPTY_MAP, "Javadoc description"},
                {TestExtraDocs.class, "Javadoc description \nin two lines", Collections.singletonMap("", "Inline tag"), "Javadoc description in two lines"}
        };
    }

    @DataProvider
    public static Object[][] workUnitGroupName() {
        return new Object[][] {
                {TestArgumentContainer.class, TestArgumentContainer.GROUP_NAME},
                {TestExtraDocs.class, TestExtraDocs.GROUP_NAME}
        };
    }

    @DataProvider
    public static Object[][] workUnitGroupSummary() {
        return new Object[][] {
                {TestArgumentContainer.class, TestProgramGroup.DESCRIPTION},
                {TestExtraDocs.class, Collections.EMPTY_MAP, ""},
        };
    }

    @Test(dataProvider = "workUnitDescriptions")
    public void testGetDescription(final Class<?> docWorkUnitClazz, final String javadocText, final Map<String, String> inlineTags,
            final String expectedDescription) {
        final DocWorkUnit mockWorkUnit = createMockWorkUnit(docWorkUnitClazz, javadocText, inlineTags);
        Assert.assertEquals(getDefaultWorkUnitHandlerInstance().getDescription(mockWorkUnit), expectedDescription);
    }

    @Test(dataProvider = "workUnitSummary")
    public void testGetSummaryForWorkUnit(final Class<?> docWorkUnitClazz, final String javadocText, final Map<String, String> inlineTags,
            final String expectedSummary) {
        final DocWorkUnit mockWorkUnit = createMockWorkUnit(docWorkUnitClazz, javadocText, inlineTags);
        Assert.assertEquals(getDefaultWorkUnitHandlerInstance().getSummaryForWorkUnit(mockWorkUnit), expectedSummary);
    }

    @Test(dataProvider = "workUnitGroupName")
    public void testGetGroupNameForWorkUnit(final Class<?> docWorkUnitClazz, final String expectedGroupName) {
        final DocWorkUnit mockWorkUnit = DocGenMocks.createDocWorkUnit(
                getDefaultWorkUnitHandlerInstance(), docWorkUnitClazz);
        Assert.assertEquals(getDefaultWorkUnitHandlerInstance().getGroupNameForWorkUnit(mockWorkUnit), expectedGroupName);
    }

    @Test(dataProvider = "workUnitGroupSummary")
    public void testGetGroupSummaryForWorkUnit(final Class<?> docWorkUnitClazz, final String expectedGroupSummary) {
        final DocWorkUnit mockWorkUnit = DocGenMocks.createDocWorkUnit(
                getDefaultWorkUnitHandlerInstance(), docWorkUnitClazz);
        Assert.assertEquals(getDefaultWorkUnitHandlerInstance().getGroupSummaryForWorkUnit(mockWorkUnit), expectedGroupSummary);
    }

    private static DocWorkUnit createMockWorkUnit(final Class<?> docWorkUnitClazz, final String javadocText, final Map<String, String> inlineTags) {
        final ClassDoc mockClassDoc = DocGenMocks.mockClassDoc(javadocText, inlineTags);
        return DocGenMocks.createDocWorkUnit(getDefaultWorkUnitHandlerInstance(), docWorkUnitClazz, mockClassDoc);
    }

    private static DefaultDocWorkUnitHandler getDefaultWorkUnitHandlerInstance() {
        return new DefaultDocWorkUnitHandler(new HelpDoclet());
    }
}