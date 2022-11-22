package org.broadinstitute.barclay.help;

import com.sun.javadoc.ClassDoc;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.argparser.DeprecatedFeature;
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
public class DocWorkUnitTest {

    @Test
    public void testPropertyMap() {
        final String property = "test_property";
        final String value = "test_value";
        final DocWorkUnit workUnit = createDocWorkUnitForDefaultHandler(TestExtraDocs.class, DocGenMocks.mockClassDoc("", Collections.emptyMap()));
        Assert.assertTrue(workUnit.getRootMap().isEmpty());
        Assert.assertEquals(workUnit.getProperty(property), null);
        workUnit.setProperty(property, value);
        Assert.assertEquals(workUnit.getProperty(property), value);
        Assert.assertEquals(workUnit.getRootMap().size(), 1);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals(
                createDocWorkUnitForDefaultHandler(TestExtraDocs.class, DocGenMocks.mockClassDoc("", Collections.emptyMap())).getName(), "TestExtraDocs");
    }

    @Test
    public void testGetCommandLineProperties() {
        Assert.assertNull(
                createDocWorkUnitForDefaultHandler(TestExtraDocs.class, DocGenMocks.mockClassDoc("", Collections.emptyMap())).getCommandLineProperties());
        Assert.assertNotNull(
                createDocWorkUnitForDefaultHandler(TestArgumentContainer.class, DocGenMocks.mockClassDoc("", Collections.emptyMap())).getCommandLineProperties());
    }

    @DataProvider
    public Object[][] betaFeatureData() {
        return new Object[][] {
                {TestExtraDocs.class, false},
                {TestArgumentContainer.class, true}
        };
    }

    @Test(dataProvider = "betaFeatureData")
    public void tesGetBetaFeature(final Class<?> clazz, final boolean isBetaFeature) {
        Assert.assertEquals(createDocWorkUnitForDefaultHandler(clazz, DocGenMocks.mockClassDoc("", Collections.emptyMap())).isBetaFeature(),
                isBetaFeature);
    }

    @DataProvider
    public Object[][] experimentalFeatureData() {
        return new Object[][] {
                {TestExtraDocs.class, true},
                {TestArgumentContainer.class, false}
        };
    }

    @Test(dataProvider = "experimentalFeatureData")
    public void tesGetExperimentalFeature(final Class<?> clazz, final boolean isExperimentalFeature) {
        Assert.assertEquals(createDocWorkUnitForDefaultHandler(clazz, DocGenMocks.mockClassDoc("", Collections.emptyMap())).isExperimentalFeature(),
                isExperimentalFeature);
    }

    @CommandLineProgramProperties(
            summary = "test CLP deprecation",
            oneLineSummary = "",
            programGroup = TestProgramGroup.class)
    @DeprecatedFeature
    @DocumentedFeature
    public class DeprecatedCLPTest {
    }

    @Test
    public void testDeprecatedCommandLineProgram() {
        final DocWorkUnit deprecatedCLPWorkUnit = createDocWorkUnitForDefaultHandler(DeprecatedCLPTest.class,
                        DocGenMocks.mockClassDoc("", Collections.emptyMap()));
        Assert.assertTrue(deprecatedCLPWorkUnit.isDeprecatedFeature());
        Assert.assertEquals(deprecatedCLPWorkUnit.getDeprecationDetail(), "This feature is deprecated and will be removed in a future release.");
    }

    @Test
    public void testCompareTo() {
        final DocWorkUnit first = createDocWorkUnitForDefaultHandler(TestArgumentContainer.class, DocGenMocks.mockClassDoc("", Collections.emptyMap()));
        final DocWorkUnit second = createDocWorkUnitForDefaultHandler(TestExtraDocs.class, DocGenMocks.mockClassDoc("", Collections.emptyMap()));
        Assert.assertEquals(first.compareTo(first), 0);
        Assert.assertTrue(first.compareTo(second) < 0);
        Assert.assertTrue(second.compareTo(first) > 0);
    }

    private DocWorkUnit createDocWorkUnitForDefaultHandler(
            final Class<?> clazz, final ClassDoc classDoc) {
        return new DocWorkUnit(
                new DefaultDocWorkUnitHandler(new HelpDoclet()),
                clazz.getAnnotation(DocumentedFeature.class),
                classDoc,
                clazz
        );
    }

}