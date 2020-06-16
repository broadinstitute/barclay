package org.broadinstitute.barclay.help;

/**
 * Work unit handler for tab completion work units.
 */
public class BashTabCompletionDocWorkUnitHandler extends DefaultDocWorkUnitHandler {

    /**
     * @param doclet for this run. May not be null.
     */
    public BashTabCompletionDocWorkUnitHandler(final HelpDoclet doclet) {
        super(doclet);
    }

    /**
     * Add bindings describing related capabilities to currentWorkUnit. The tab completion doclet filters
     * out any work unit that represents a class that doesn't have
     * {@link org.broadinstitute.barclay.argparser.CommandLineProgramProperties}. Since doing so may filter
     * out classes that are referenced by the extraDocs attribute in classes that ARE
     * {@link org.broadinstitute.barclay.argparser.CommandLineProgramProperties}, we need to suppress
     * resolution of extraDocs for tab completion work units by overriding this method to do nothing.
     */
    @Override
    protected void addExtraDocsBindings(final DocWorkUnit currentWorkUnit) { }

}
