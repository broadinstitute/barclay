package org.broadinstitute.barclay.help;

import org.broadinstitute.barclay.utils.Utils;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for work unit handlers for docs. The DocWorkUnitHandler defines the template
 * used for each documented feature, and populates the template property map for that template.
 */
public abstract class DocWorkUnitHandler {
    private final HelpDoclet doclet;

    /**
     * @param doclet the HelpDoclet driving this documentation run. Can not be null.
     */
    public DocWorkUnitHandler(final HelpDoclet doclet) {
        Utils.nonNull("Doclet cannot be null");
        this.doclet = doclet;
    }

    /**
     * @return the HelpDoclet driving this documentation run
     */
    public HelpDoclet getDoclet() {
        return doclet;
    }

    /**
     * Actually generate the documentation map by populating the associated workUnit's properties.
     *
     * @param workUnit work unit to generate documentation for
     */
    public abstract void processWorkUnit(DocWorkUnit workUnit, List<Map<String, String>>featureMaps, List<Map<String, String>> groupMaps);

    /**
     * Return the name of the FreeMarker template to be used to process the work unit.
     *
     * Note this is a flat filename relative to settings/helpTemplates in the source tree
     * @param workUnit template to use for this work unit
     * @return name of the template
     * @throws IOException
     */
    public abstract String getTemplateName(DocWorkUnit workUnit);

    /**
     * Return the flat filename (no paths) that the handler would like the Doclet to
     * write out the documentation for workUnit
     * @param workUnit
     * @return the name of the destination file to which documentation output will be written
     */
    public String getDestinationFilename(final DocWorkUnit workUnit) {
        return DocletUtils.phpFilenameForClass(workUnit.getClazz(), getDoclet().outputFileExtension);
    }

    /**
     * Returns the JSON output file name.
     */
    public String getJSONFilename(final DocWorkUnit workUnit) {
        return DocletUtils.phpFilenameForClass(workUnit.getClazz(), "json");
    }

    /**
     * Apply any fallback rules to determine the summary line that should be used for the work unit.
     * Default implementation uses the value from the DocumentedFeature annotation.
     * @param workUnit
     * @return Summary for this work unit.
     */
    public String getSummaryForWorkUnit(final DocWorkUnit workUnit) {
        return workUnit.getDocumentedFeature().summary();
    }

    /**
     * Apply any fallback rules to determine the group name line that should be used for the work unit.
     * Default implementation uses the value from the DocumentedFeature annotation.
     * @param workUnit
     * @return Group name to be used for this work unit.
     */
    public String getGroupNameForWorkUnit(final DocWorkUnit workUnit) {
        return workUnit.getDocumentedFeature().groupName();
    }

    /**
     * Apply any fallback rules to determine the group summary line that should be used for the work unit.
     * Default implementation uses the value from the DocumentedFeature annotation.
     * @param workUnit
     * @return Group summary to be used for this work unit.
     */
    public String getGroupSummaryForWorkUnit(final DocWorkUnit workUnit) {
        return workUnit.getDocumentedFeature().groupSummary();
    }

}
