package org.broadinstitute.barclay.help.scanners;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.broadinstitute.barclay.help.DocWorkUnit;
import org.broadinstitute.barclay.help.DocletUtils;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.barclay.help.HelpDoclet;
import org.broadinstitute.barclay.utils.Utils;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementScanner14;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An {@link ElementScanner14} for finding {@link DocumentedFeature}s to be included in a Barclay doc
 * task. Returns a set of {@link DocWorkUnit} objects.
 */
public class DocumentedFeatureScanner extends ElementScanner14<Void, Void> {
    private final HelpDoclet helpDoclet;
    private final DocletEnvironment docEnv;
    private final Reporter reporter;
    private Set<DocWorkUnit> workUnits = new LinkedHashSet<>();     // Set of all things we are going to document

    /**
     * For internal use only. External callers should use
     * {@link JavaLanguageModelScanners#getWorkUnits(HelpDoclet, DocletEnvironment, Reporter, Set)}}
     *
     * @param helpDoclet the {@link HelpDoclet}
     * @param docEnv the {@link DocletEnvironment}
     * @param reporter reporter to be used to issue messages
     */
    DocumentedFeatureScanner(
            final HelpDoclet helpDoclet,
            final DocletEnvironment docEnv,
            final Reporter reporter) {
        Utils.nonNull(helpDoclet, "helpDoclet");
        Utils.nonNull(docEnv, "doclet environment");
        Utils.nonNull(reporter, "logger");

        this.helpDoclet = helpDoclet;
        this.docEnv = docEnv;
        this.reporter = reporter;
    }

    @Override
    public Void scan(final Element e, final Void unused) {
        if (e.asType().getKind().equals(TypeKind.DECLARED)) {
            final Class<?> clazz = DocletUtils.getClassForDeclaredElement(e, docEnv);
            final DocumentedFeature documentedFeature = DocletUtils.getDocumentedFeatureForClass(clazz);
            if (documentedFeature != null) {
                if (documentedFeature.enable() && helpDoclet.includeInDocs(documentedFeature, clazz)) {
                    final DocWorkUnit workUnit = helpDoclet.createWorkUnit(e, clazz, documentedFeature);
                    if (workUnit != null) {
                        workUnits.add(workUnit);
                    }
                } else {
                    reporter.print(Diagnostic.Kind.NOTE, "Skipping disabled documentation for feature: " + e);
                }
            }
        }
        return super.scan(e, unused);
    }

    /**
     * Return the {@link DocWorkUnit}s for the included elements.
     */
    Set<DocWorkUnit> getWorkUnits() { return workUnits; }

}
