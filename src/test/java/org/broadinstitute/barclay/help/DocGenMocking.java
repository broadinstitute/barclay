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
public class DocGenMocking {

    /**
     * Generates an empty ClassDoc (mocked).
     */
    public static ClassDoc emptyClassDoc() {
        return mockClassDoc("");
    }

    /**
     * Generates an ClassDoc with the javadoc text
     */
    public static ClassDoc mockClassDoc(final String javadocText) {
        return mockClassDoc(javadocText, Collections.emptyMap());
    }


    /**
     * @param inlineTags map of (custom) tag names in javadoc to its text.
     *
     * @return mocked class doc.
     */
    public static ClassDoc mockClassDoc(final String javadocText,
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


    private static Tag mockTag(final String name, final String text) {
        final Tag tag = Mockito.mock(Tag.class);
        Mockito.when(tag.name()).thenReturn(name);
        Mockito.when(tag.text()).thenReturn(text);
        return tag;
    }

    public static DocWorkUnit createDocWorkUnit(final DocWorkUnitHandler docWorkUnitHandler,
            final Class<?> clazz, final ClassDoc classDoc) {
        return new DocWorkUnit(
                docWorkUnitHandler,
                clazz.getAnnotation(DocumentedFeature.class),
                classDoc,
                clazz
        );
    }

    public static DocWorkUnit createDocWorkUnit(final DocWorkUnitHandler docWorkUnitHandler, final Class<?> clazz) {
        return createDocWorkUnit(docWorkUnitHandler, clazz, emptyClassDoc());
    }
}
