package org.broadinstitute.barclay.help.testdoclets;

import jdk.javadoc.doclet.DocletEnvironment;
import org.broadinstitute.barclay.help.DefaultDocWorkUnitHandler;
import org.broadinstitute.barclay.help.DocException;
import org.broadinstitute.barclay.help.DocWorkUnit;
import org.broadinstitute.barclay.help.DocWorkUnitHandler;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.barclay.help.HelpDoclet;

import javax.lang.model.element.Element;
import java.io.IOException;
import java.util.Map;

/**
 * For testing of help documentation generation.
 */
public class TestDoclet extends HelpDoclet {

    @Override
    public DocWorkUnit createWorkUnit(
            final Element classElement,
            final Class<?> clazz,
            final DocumentedFeature documentedFeature)
    {
        return new DocWorkUnit(
                new TestDocWorkUnitHandler(this),
                classElement,
                clazz,
                documentedFeature);
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
