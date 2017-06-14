/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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