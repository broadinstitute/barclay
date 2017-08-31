package org.broadinstitute.barclay.help;

import org.broadinstitute.barclay.help.testinputs.TestArgumentContainer;
import org.broadinstitute.barclay.help.testinputs.TestExtraDocs;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DocWorkUnitTest {

    @Test
    public void testPropertyMap() {
        final DocWorkUnit workUnit = DocGenMocks
                .createDocWorkUnit(getDefaultDocWorkUnitHandler(), TestExtraDocs.class);
        Assert.assertTrue(workUnit.getRootMap().isEmpty());
        Assert.assertEquals(workUnit.getProperty("test_property"), null);
        workUnit.setProperty("test_property", "test_value");
        Assert.assertEquals(workUnit.getProperty("test_property"), "test_value");
        Assert.assertEquals(workUnit.getRootMap().size(), 1);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals(
                DocGenMocks.createDocWorkUnit(getDefaultDocWorkUnitHandler(), TestExtraDocs.class).getName(), "TestExtraDocs");
    }

    @Test
    public void testGetCommandLineProperties() {
        Assert.assertNull(
                DocGenMocks.createDocWorkUnit(getDefaultDocWorkUnitHandler(), TestExtraDocs.class).getCommandLineProperties());
        Assert.assertNotNull(
                DocGenMocks.createDocWorkUnit(getDefaultDocWorkUnitHandler(), TestArgumentContainer.class).getCommandLineProperties());
    }

    @Test
    public void tesGetBetaFeature() {
        Assert.assertFalse(
                DocGenMocks.createDocWorkUnit(getDefaultDocWorkUnitHandler(), TestExtraDocs.class).getBetaFeature());
        Assert.assertTrue(
                DocGenMocks.createDocWorkUnit(getDefaultDocWorkUnitHandler(), TestArgumentContainer.class).getBetaFeature());
    }

    @Test
    public void testCompareTo() {
        final DocWorkUnit first = DocGenMocks
                .createDocWorkUnit(getDefaultDocWorkUnitHandler(), TestArgumentContainer.class);
        final DocWorkUnit second = DocGenMocks
                .createDocWorkUnit(getDefaultDocWorkUnitHandler(), TestExtraDocs.class);
        Assert.assertEquals(first.compareTo(first), 0);
        Assert.assertTrue(first.compareTo(second) < 0);
        Assert.assertTrue(second.compareTo(first) > 0);
    }

    private static DefaultDocWorkUnitHandler getDefaultDocWorkUnitHandler() {
        return new DefaultDocWorkUnitHandler(new HelpDoclet());
    }

}