package org.broadinstitute.barclay.help;

import com.sun.javadoc.ClassDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.CommandLineProgramGroup;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collection of all relevant information about a single feature the HelpDoclet can document
 */
public class DocWorkUnit implements Comparable<DocWorkUnit> {
    final protected static Logger logger = LogManager.getLogger(DocWorkUnit.class);

    private final String name;                          // name of the this work unit/feature

    private final Class<?> clazz;                       // class that's being documented
    private final ClassDoc classDoc;                    // javadoc documentation for clazz
    private final DocWorkUnitHandler workUnitHandler;   // handler for this work unit

    // Annotations attached to the feature class being documented by this work unit
    private final DocumentedFeature documentedFeature;
    private final CommandLineProgramProperties commandLineProperties;
    private final BetaFeature betaFeature;

    private Map<String, Object> propertyMap = new HashMap<>(); // propertyMap for this unit's template

    // Cached values derived by fallback policies that are implemented by the work unit handler.
    protected String summary;         // summary description of this feature
    protected String groupName;       // name of the feature group to which this feature belongs
    protected String groupSummary;    // summary description of this feature's feature group

    /**
     * @param workUnitHandler
     * @param documentedFeatureAnnotation
     * @param classDoc
     * @param clazz
     */
    public DocWorkUnit(
            final DocWorkUnitHandler workUnitHandler,
            final DocumentedFeature documentedFeatureAnnotation,
            final ClassDoc classDoc,
            final Class<?> clazz)
    {
        Utils.nonNull(workUnitHandler, "workUnitHandler cannot be null");
        Utils.nonNull(documentedFeatureAnnotation, "DocumentedFeature annotation cannot be null");
        Utils.nonNull(classDoc, "classDoc cannot be null");
        Utils.nonNull(clazz, "class cannot be null");

        this.name = clazz.getSimpleName();

        this.documentedFeature = documentedFeatureAnnotation;
        this.commandLineProperties = clazz.getAnnotation(CommandLineProgramProperties.class);
        this.betaFeature = clazz.getAnnotation(BetaFeature.class);
        this.workUnitHandler = workUnitHandler;
        this.classDoc = classDoc;
        this.clazz = clazz;

        // summary, groupName and groupSummary can each be determined via fallback policies dictated
        // by the feature handler, so delegate back to the handler to allow it to do the initialization
        // once, and then cache the results so that all consumers see consistent values.
        summary = workUnitHandler.getSummaryForWorkUnit(this);
        groupName = workUnitHandler.getGroupNameForWorkUnit(this);
        groupSummary = workUnitHandler.getGroupSummaryForWorkUnit(this);
    }

    /**
     * Get the root property map used for this work unit.
     * @return Root property map for this work unit.
     */
    public Map<String, Object> getRootMap() {
        return (this.propertyMap);
    }

    /**
     * Set a property on the root property map for this work unit.
     * @param key
     * @param value
     */
    public void setProperty(final String key, final Object value) {
        propertyMap.put(key, value);
    }

    /**
     * Get a property from the root property map for this work unit
     * @param key
     * @return Object value for the given property, or null if property not found.
     */
    public Object getProperty(final String key) {
        return propertyMap.get(key);
    }

    /**
     * Get the DocumentedFeature annotation object for this class.
     * @return DocumentedFeature object. Will not be null.
     */
    public DocumentedFeature getDocumentedFeature() { return documentedFeature; }

    /**
     * Get the JavDoc ClassDoc for this work unit.
     * @return ClassDoc for this work unit. Will not be null.
     */
    public ClassDoc getClassDoc() { return classDoc; }

    /**
     * The name of this documentation unit
     */
    public String getName() {
        return name;
    }

    /**
     * The name of the documentation group (e.g., walkers, read filters) class belongs to
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * The group summary for the group for this object
     */
    public String getGroupSummary() {
        return groupSummary;
    }

    /**
     * The summary of the documentation object
     */
    public String getSummary() { return summary; }

    public Class<?> getClazz() { return clazz; }

    /**
     * Populate the property map for this work unit by delegating to the documented feature handler for this work unit.
     * @param featureMaps map of all features included in this javadoc run
     * @param groupMaps map of all groups included in the javadoc run
     */
    public void processDoc(final List<Map<String, String>> featureMaps, final List<Map<String, String>> groupMaps) {
        workUnitHandler.processWorkUnit(this, featureMaps, groupMaps);

    };

    /**
     * Get the template to be used for this work unit. Delegates to the feature handler.
     * @return name of the template (relative to the input path specified to the doclet) for the template to be used
     * for this work unit.
     */
    public String getTemplateName() { return workUnitHandler.getTemplateName(this); }

    public String getTargetFileName() { return workUnitHandler.getDestinationFilename(this); }

    /**
     * Get the CommandLineProgramProperties annotation for this work unit.
     * @return CommandLineProgramProperties object for this work unit. May be null for features that are not
     * command line programs.
     */
    public CommandLineProgramProperties getCommandLineProperties() { return commandLineProperties; }

    /**
     * @return a boolean determining if this documented feature is marked as a beta feature
     */
    public boolean getBetaFeature() { return betaFeature != null; }

    /**
     * Get the CommandLineProgramGroup object from the CommandLineProgramProperties of this work unit.
     * @return CommandLineProgramGroup if the feature has one, otherwise null.
     */
    public CommandLineProgramGroup getCommandLineProgramGroup() {
        if (commandLineProperties != null) {
            try {
                return commandLineProperties.programGroup().newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                logger.warn(
                        String.format("Can't instantiate program group class to retrieve summary for group %s for class %s",
                                commandLineProperties.programGroup().getName(),
                                clazz.getName()));
            }
        }
        return null;
    }

    /**
     * Sort in order of the name of this WorkUnit
     */
    public int compareTo(DocWorkUnit other) {
        return this.name.compareTo(other.name);
    }
}
