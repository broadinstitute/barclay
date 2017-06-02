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

import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.argparser.TestProgramGroup;

/**
 * Documentation of this tool is written in Markdown and rendered as HTML.
 *
 * ## Javadoc formatted with Markdown
 *
 * The purpose of this paragraph is to test embedded [Markdown](https://daringfireball.net/projects/markdown/) formatting:
 *
 * - First element of list (**bold**)
 * - Second element of list (_italic_)
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(
        summary = "Test tool summary with [Markdown](https://daringfireball.net/projects/markdown/) formatted docs",
        oneLineSummary = "Tool where the documentation is formatted with [Markdown](https://daringfireball.net/projects/markdown/) `String`.",
        programGroup = TestProgramGroup.class)
@DocumentedFeature(groupName = MarkdownDocumentedFeature.markdownGroup, groupSummary = MarkdownDocumentedFeature.markdownGroupSummary)
public class ClpWithMarkdownDocs {

    @Argument(doc = "This is a **bold** `String` __argument__")
    public String bold = "";

    @Argument(doc = "This is a *italic* `String` _argument_")
    public String italic = "";

    @ArgumentCollection(doc = "Argument collection with [Markdown](https://daringfireball.net/projects/markdown/) formatted docs")
    public MarkdownArgumentCollection argumentCollection = new MarkdownArgumentCollection();

}
