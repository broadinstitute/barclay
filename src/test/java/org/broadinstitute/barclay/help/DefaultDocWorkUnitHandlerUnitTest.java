package org.broadinstitute.barclay.help;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.argparser.TestProgramGroup;
import org.broadinstitute.barclay.help.testinputs.TestArgumentContainer;
import org.broadinstitute.barclay.help.testinputs.TestExtraDocs;
import org.mockito.Mockito;
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
                {TestExtraDocs.class, ""},
        };
    }

    @Test(dataProvider = "workUnitDescriptions")
    public void testGetDescription(final Class<?> docWorkUnitClazz, final String javadocText, final Map<String, String> inlineTags,
            final String expectedDescription) {
        final DefaultDocWorkUnitHandler handler = getDefaultWorkUnitHandlerInstance();
        final DocWorkUnit mockWorkUnit = createMockWorkUnit(handler, docWorkUnitClazz, javadocText, inlineTags);
        Assert.assertEquals(handler.getDescription(mockWorkUnit), expectedDescription);
    }

    @Test(dataProvider = "workUnitSummary")
    public void testGetSummaryForWorkUnit(final Class<?> docWorkUnitClazz, final String javadocText, final Map<String, String> inlineTags,
            final String expectedSummary) {
        final DefaultDocWorkUnitHandler handler = getDefaultWorkUnitHandlerInstance();
        final DocWorkUnit mockWorkUnit = createMockWorkUnit(handler, docWorkUnitClazz, javadocText, inlineTags);
        Assert.assertEquals(handler.getSummaryForWorkUnit(mockWorkUnit), expectedSummary);
    }

    @Test(dataProvider = "workUnitGroupName")
    public void testGetGroupNameForWorkUnit(final Class<?> docWorkUnitClazz, final String expectedGroupName) {
        final DefaultDocWorkUnitHandler handler = getDefaultWorkUnitHandlerInstance();
        final DocWorkUnit mockWorkUnit = createMockWorkUnit(handler, docWorkUnitClazz, "", Collections.emptyMap());
        Assert.assertEquals(handler.getGroupNameForWorkUnit(mockWorkUnit), expectedGroupName);
    }

    @Test(dataProvider = "workUnitGroupSummary")
    public void testGetGroupSummaryForWorkUnit(final Class<?> docWorkUnitClazz, final String expectedGroupSummary) {
        final DefaultDocWorkUnitHandler handler = getDefaultWorkUnitHandlerInstance();
        final DocWorkUnit mockWorkUnit = createMockWorkUnit(handler, docWorkUnitClazz, "", Collections.emptyMap());
        Assert.assertEquals(handler.getGroupSummaryForWorkUnit(mockWorkUnit), expectedGroupSummary);
    }

    @DocumentedFeature
    @CommandLineProgramProperties(summary = "CLP without no-args constructor", oneLineSummary = "CLP without no-args constructor", programGroup = TestProgramGroup.class)
    private final class ClpWithoutNoArgsConstructor {
        public ClpWithoutNoArgsConstructor(final int arg) { }
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testProcessWorkUnitWithoutNoArgsConstructorClpThrows() {
        final DefaultDocWorkUnitHandler handler = getDefaultWorkUnitHandlerInstance();
        final DocWorkUnit workUnit = createMockWorkUnit(handler, ClpWithoutNoArgsConstructor.class, "", Collections.emptyMap());
        handler.processWorkUnit(workUnit, Collections.emptyList(), Collections.emptyList());
    }

    @DataProvider
    public Object[][] processWorkUnitData() {
        return new Object[][]{
                {TestArgumentContainer.class},
                {TestExtraDocs.class}
        };
    }

    @Test(dataProvider = "processWorkUnitData", enabled = false)
    public void testProcessWorkUnit(final Class<?> docWorkUnitClazz) {
        final DefaultDocWorkUnitHandler handler = getDefaultWorkUnitHandlerInstance();
        final DocWorkUnit workUnit = createMockWorkUnit(handler, docWorkUnitClazz, "", Collections.emptyMap());

        // mock the class doc to return an empty FieldDoc array to use while populating arguments
        Mockito.when(workUnit.getClassDoc().fields(Mockito.anyBoolean())).thenReturn(new FieldDoc[]{});
        handler.processWorkUnit(workUnit, Collections.emptyList(), Collections.emptyList());
        // assert that the property map is not empty
        Assert.assertFalse(workUnit.getRootMap().isEmpty());
    }

    private static DocWorkUnit createMockWorkUnit(final DefaultDocWorkUnitHandler handler, final Class<?> docWorkUnitClazz, final String javadocText, final Map<String, String> inlineTags) {
        final ClassDoc mockClassDoc = DocGenMocks.mockClassDoc(javadocText, inlineTags);
        return new DocWorkUnit(
                handler,
                docWorkUnitClazz.getAnnotation(DocumentedFeature.class),
                mockClassDoc,
                docWorkUnitClazz
        );

    }

    private static DefaultDocWorkUnitHandler getDefaultWorkUnitHandlerInstance() {
        return new DefaultDocWorkUnitHandler(new HelpDoclet());
    }
}