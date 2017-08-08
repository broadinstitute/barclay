package org.broadinstitute.barclay.help;

import org.broadinstitute.barclay.help.testinputs.TestArgumentContainer;
import org.broadinstitute.barclay.help.testinputs.TestExtraDocs;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DocWorkUnitTest {

    private final static DefaultDocWorkUnitHandler DEFAULT_DOC_WORK_UNIT_HANDLER =
            new DefaultDocWorkUnitHandler(new HelpDoclet());

    @Test
    public void testPropertyMap() throws Exception {
        final DocWorkUnit workUnit = DocGenMocking.createDocWorkUnit(DEFAULT_DOC_WORK_UNIT_HANDLER, TestExtraDocs.class);
        Assert.assertTrue(workUnit.getRootMap().isEmpty());
        Assert.assertEquals(workUnit.getProperty("test_property"), null);
        workUnit.setProperty("test_property", "test_value");
        Assert.assertEquals(workUnit.getProperty("test_property"), "test_value");
        Assert.assertEquals(workUnit.getRootMap().size(), 1);
    }

    @Test
    public void testGetName() throws Exception {
        Assert.assertEquals(DocGenMocking.createDocWorkUnit(DEFAULT_DOC_WORK_UNIT_HANDLER, TestExtraDocs.class).getName(), "TestExtraDocs");
    }

    @Test
    public void testGetCommandLineProperties() throws Exception {
        Assert.assertNull(DocGenMocking.createDocWorkUnit(DEFAULT_DOC_WORK_UNIT_HANDLER, TestExtraDocs.class).getCommandLineProperties());
        Assert.assertNotNull(DocGenMocking.createDocWorkUnit(DEFAULT_DOC_WORK_UNIT_HANDLER, TestArgumentContainer.class).getCommandLineProperties());
    }

    @Test
    public void tesGetBetaFeature() throws Exception {
        Assert.assertFalse(DocGenMocking.createDocWorkUnit(DEFAULT_DOC_WORK_UNIT_HANDLER, TestExtraDocs.class).getBetaFeature());
        Assert.assertTrue(DocGenMocking.createDocWorkUnit(DEFAULT_DOC_WORK_UNIT_HANDLER, TestArgumentContainer.class).getBetaFeature());
    }

    @Test
    public void testCompareTo() throws Exception {
        final DocWorkUnit first = DocGenMocking.createDocWorkUnit(DEFAULT_DOC_WORK_UNIT_HANDLER, TestArgumentContainer.class);
        final DocWorkUnit second = DocGenMocking.createDocWorkUnit(DEFAULT_DOC_WORK_UNIT_HANDLER, TestExtraDocs.class);
        Assert.assertEquals(first.compareTo(first), 0);
        Assert.assertTrue(first.compareTo(second) < 0);
        Assert.assertTrue(second.compareTo(first) > 0);
    }

}