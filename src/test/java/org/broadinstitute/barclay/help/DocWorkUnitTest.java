package org.broadinstitute.barclay.help;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DocWorkUnitTest extends DocGenBaseTest {

    @Test
    public void testPropertyMap() throws Exception {
        final DocWorkUnit workUnit = createDocWorkUnit(TestExtraDocs.class);
        Assert.assertTrue(workUnit.getRootMap().isEmpty());
        Assert.assertEquals(workUnit.getProperty("test_property"), null);
        workUnit.setProperty("test_property", "test_value");
        Assert.assertEquals(workUnit.getProperty("test_property"), "test_value");
        Assert.assertEquals(workUnit.getRootMap().size(), 1);
    }

    @Test
    public void testGetName() throws Exception {
        Assert.assertEquals(createDocWorkUnit(TestExtraDocs.class).getName(), "TestExtraDocs");
    }

    @Test
    public void testGetCommandLineProperties() throws Exception {
        Assert.assertNull(createDocWorkUnit(TestExtraDocs.class).getCommandLineProperties());
        Assert.assertNotNull(createDocWorkUnit(TestArgumentContainer.class).getCommandLineProperties());
    }

    @Test
    public void tesGetBetaFeature() throws Exception {
        Assert.assertFalse(createDocWorkUnit(TestExtraDocs.class).getBetaFeature());
        Assert.assertTrue(createDocWorkUnit(TestArgumentContainer.class).getBetaFeature());
    }

    @Test
    public void testCompareTo() throws Exception {
        final DocWorkUnit first = createDocWorkUnit(TestArgumentContainer.class);
        final DocWorkUnit second = createDocWorkUnit(TestExtraDocs.class);
        Assert.assertEquals(first.compareTo(first), 0);
        Assert.assertTrue(first.compareTo(second) < 0);
        Assert.assertTrue(second.compareTo(first) > 0);
    }

}