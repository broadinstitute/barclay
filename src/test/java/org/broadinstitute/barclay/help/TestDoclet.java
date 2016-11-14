package org.broadinstitute.barclay.help;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

import java.io.IOException;
import java.util.Map;

/**
 * For testing of help documentation generation.
 */
public class TestDoclet extends HelpDoclet {

    public static boolean start(RootDoc rootDoc) {
        try {
            return new TestDoclet().startProcessDocs(rootDoc);
        } catch (IOException e) {
            throw new DocException("Exception processing javadoc", e);
        }
    }

    @Override
    protected DocumentedFeatureHandler createDocumentedFeatureHandler(
            final ClassDoc classDoc, final DocumentedFeatureObject documentedFeature)
    {
        return new TestDocumentedFeatureHandler();
    }

    /**
     * Trivial helper routine that returns the map of name and summary given the documentedFeatureObject
     * AND adds a super-category so that we can custom-order the categories in the index
     *
     * @param annotation
     * @return
     */
    @Override
    protected final Map<String, String> getGroupMap(DocumentedFeatureObject annotation) {
        Map<String, String> root = super.getGroupMap(annotation);
        root.put("supercat", "other");
        return root;
    }

}
