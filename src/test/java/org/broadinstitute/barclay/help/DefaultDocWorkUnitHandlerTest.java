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