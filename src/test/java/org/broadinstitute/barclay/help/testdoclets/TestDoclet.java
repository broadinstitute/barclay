package org.broadinstitute.barclay.help.testdoclets;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import org.broadinstitute.barclay.help.DocException;
import org.broadinstitute.barclay.help.DocWorkUnit;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.barclay.help.HelpDoclet;

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
    protected DocWorkUnit createWorkUnit(
        final DocumentedFeature documentedFeature,
        final ClassDoc classDoc,
        final Class<?> clazz)
    {
        return new DocWorkUnit(
                new TestDocWorkUnitHandler(this),
                documentedFeature,
                classDoc,
                clazz);
    }

    /**
     * Trivial helper routine that returns the map of name and summary given the workUnit
     * AND adds a super-category so that we can custom-order the categories in the index
     *
     * @param workUnit
     * @return
     */
    @Override
    protected final Map<String, String> getGroupMap(DocWorkUnit workUnit) {
        Map<String, String> root = super.getGroupMap(workUnit);
        root.put("supercat", "other");
        return root;
    }

}
