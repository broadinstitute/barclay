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

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Tag;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DocGenBaseTest {

    protected final static HelpDoclet HELP_DOCLET = new HelpDoclet();
    protected final static DefaultDocWorkUnitHandler DEFAULT_DOC_WORK_UNIT_HANDLER =
            new DefaultDocWorkUnitHandler(HELP_DOCLET);

    /**
     * Generates an empty ClassDoc (mocked).
     */
    protected ClassDoc emptyClassDoc() {
        return mockClassDoc("");
    }

    /**
     * Generates an ClassDoc with the javadoc text
     */
    protected ClassDoc mockClassDoc(final String javadocText) {
        return mockClassDoc(javadocText, Collections.emptyMap());
    }


    /**
     * @param inlineTags map of (custom) tag names in javadoc to its text.
     *
     * @return mocked class doc.
     */
    protected ClassDoc mockClassDoc(final String javadocText,
            final Map<String, String> inlineTags) {
        // mock class
        final ClassDoc mockedClassDoc = Mockito.mock(ClassDoc.class);
        // mock the javadoc text
        final Tag[] javadoc = Arrays.stream(javadocText.split("\n"))
                .map(line -> mockTag(null, line))
                .toArray(Tag[]::new);
        Mockito.when(mockedClassDoc.firstSentenceTags()).thenReturn(javadoc);
        final Tag[] inline = inlineTags.entrySet().stream()
                .map(entry -> mockTag(entry.getKey(), entry.getValue()))
                .toArray(Tag[]::new);
        Mockito.when(mockedClassDoc.inlineTags()).thenReturn(inline);
        return mockedClassDoc;
    }


    private Tag mockTag(final String name, final String text) {
        final Tag tag = Mockito.mock(Tag.class);
        Mockito.when(tag.name()).thenReturn(name);
        Mockito.when(tag.text()).thenReturn(text);
        return tag;
    }

    protected DocWorkUnit createDocWorkUnit(final Class<?> clazz, final ClassDoc classDoc) {
        return new DocWorkUnit(
                DEFAULT_DOC_WORK_UNIT_HANDLER,
                clazz.getAnnotation(DocumentedFeature.class),
                classDoc,
                clazz
        );
    }

    protected DocWorkUnit createDocWorkUnit(final Class<?> clazz) {
        return createDocWorkUnit(clazz, emptyClassDoc());
    }
}
