package org.broadinstitute.barclay.help;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;

import org.apache.commons.lang3.tuple.Pair;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.*;
import org.broadinstitute.barclay.utils.Utils;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of DocWorkUnitHandler. The DocWorkUnitHandler determines the template that will be
 * used for a given work unit, and populates the Freemarker property map used for a single feature/work-unit.
 * *
 * Most consumers will subclass this to provide at least provide a custom FreeMarker Template, and possibly
 * other custom behavior.
 */
public class DefaultDocWorkUnitHandler extends DocWorkUnitHandler {
    final protected static Logger logger = LogManager.getLogger(DefaultDocWorkUnitHandler.class);

    private static final String NAME_FOR_POSITIONAL_ARGS = "[NA - Positional]";
    private static final String DEFAULT_FREEMARKER_TEMPLATE_NAME = "generic.html.ftl";

    /**
     * @param doclet for this documentation run. May not be null.
     */
    public DefaultDocWorkUnitHandler(final HelpDoclet doclet) {
        super(doclet);
    }

    /**
     * Return the template to be used for the particular workUnit. Must be present in the location
     * specified is the -settings-dir doclet parameter.
     *
     * @param workUnit workUnit for which a template ie being requested
     * @return name of the template file to use, relative to -settings-dir
     */
    @Override
    public String getTemplateName(final DocWorkUnit workUnit) {
        return DEFAULT_FREEMARKER_TEMPLATE_NAME;
    }

    /**
     * Get the summary string to be used for a given work unit, applying any fallback policy. This is
     * called by the work unit handler after the work unit has been populated, and may be overridden by
     * subclasses to provide custom behavior.
     *
     * @param workUnit
     * @return the summary string to be used for this work unit
     */
    @Override
    public String getSummaryForWorkUnit(final DocWorkUnit workUnit) {
        String summary = workUnit.getDocumentedFeature().summary();
        if (summary == null || summary.isEmpty()) {
            final CommandLineProgramProperties commandLineProperties = workUnit.getCommandLineProperties();
            if (commandLineProperties != null) {
                summary = commandLineProperties.oneLineSummary();
            }
            if (summary == null || summary.isEmpty()) {
                // If no summary was found from annotations, use the javadoc if there is any
                summary = Arrays.stream(workUnit.getClassDoc().firstSentenceTags())
                        .map(tag -> tag.text())
                        .collect(Collectors.joining());
            }
        }

        return summary == null ? "" : summary;
    }

    /**
     * Get the group name string to be used for a given work unit, applying any fallback policy. This is
     * called by the work unit handler after the work unit has been populated, and may be overridden by
     * subclasses to provide custom behavior.
     *
     * @param workUnit
     * @return the group name to be used for this work unit
     */
    @Override
    public String getGroupNameForWorkUnit(final DocWorkUnit workUnit) {
        String groupName = workUnit.getDocumentedFeature().groupName();
        if (groupName == null || groupName.isEmpty()) {
            final CommandLineProgramGroup clpGroup = workUnit.getCommandLineProgramGroup();
            if (clpGroup != null) {
                groupName = clpGroup.getName();
            }
            if (groupName == null || groupName.isEmpty()) {
                logger.warn("No group name declared for: " + workUnit.getClazz().getCanonicalName());
                groupName = "";
            }
        }
        return groupName;
    }

    /**
     * Get the group summary string to be used for a given work unit's group, applying any fallback policy.
     * This is called by the work unit handler after the work unit has been populated, and may be overridden by
     * subclasses to provide custom behavior.
     *
     * @param workUnit
     * @return the group summary to be used for this work unit's group
     */
    @Override
    public String getGroupSummaryForWorkUnit( final DocWorkUnit workUnit){
        String groupSummary = workUnit.getDocumentedFeature().groupSummary();
        final CommandLineProgramGroup clpGroup = workUnit.getCommandLineProgramGroup();
        if (groupSummary == null || groupSummary.isEmpty()) {
            if (clpGroup != null) {
                groupSummary = clpGroup.getDescription();
            }
            if (groupSummary == null || groupSummary.isEmpty()) {
                logger.warn("No group summary declared for: " + workUnit.getClazz().getCanonicalName());
                groupSummary = "";
            }
        }
        return groupSummary;
    }

    /**
     * Return the description to be used for the work unit. We need to manually strip
     * out any inline custom javadoc tags since we don't those in the summary.
     *
     * @param currentWorkUnit
     * @return Description to be used or the work unit.
     */
    protected String getDescription(final DocWorkUnit currentWorkUnit) {
        return Arrays.stream(currentWorkUnit.getClassDoc().inlineTags())
                .filter(t -> getTagPrefix() == null || !t.name().startsWith(getTagPrefix()))
                .map(t -> t.text())
                .collect(Collectors.joining());
    }

    /**
     * Create the Freemarker Template Map, with the following top-level structure:
     *
     * The overall default key/value structure of featureMaps looks like this:
     *
     * name -> String
     * group -> String
     * version -> String
     * timestamp -> String
     * summary -> String
     * description -> String
     * extraDocs -> List
     * plugin1 ->
     *      name -> String
     *      filename -> String (link)
     *   .
     *   .
     * pluginN -> ...
     * arguments -> List
     * gson-arguments -> List
     * groups -> list of maps, one per group
     *      name -> ..
     *      id -> ..
     *      summary ->..
     * data -> list of maps, one per documented feature
     *      name ->..
     *      summary -> ..
     *      group -> ..
     *      filename -> ..
     *
     *
     * Key/value structure of groupMaps:
     *      name -> ..
     *      id -> ..
     *      summary ->..
     *
     * @param workUnit work unit to process
     * @param featureMaps list of feature maps, one per documented feature, as defined above
     * @param groupMaps list of group maps, one per group, as defined above
     */
    @Override
    public void processWorkUnit(
            final DocWorkUnit workUnit,
            final List<Map<String, String>> featureMaps,
            final List<Map<String, String>> groupMaps) {

        CommandLineArgumentParser clp = null;
        List<? extends CommandLinePluginDescriptor<?>> pluginDescriptors = new ArrayList<>();

        // Not all DocumentedFeature targets are CommandLinePrograms, and thus not all can be instantiated via
        // a no-arg constructor. But we do want to generate a doc page for them. Any arguments associated with
        // such a feature will show up in the doc page for any referencing CommandLinePrograms, instead of in
        // the standalone feature page.
        //
        // Ex: We may want to document an input or output file format by adding @DocumentedFeature
        // to the format's reader/writer class (i.e. TableReader), and then reference that feature
        // in the extraDocs attribute in a CommandLineProgram that reads/writes that format.
        try {
            final Object argumentContainer = workUnit.getClazz().newInstance();
            if (argumentContainer instanceof CommandLinePluginProvider) {
                pluginDescriptors = ((CommandLinePluginProvider) argumentContainer).getPluginDescriptors();
                clp = new CommandLineArgumentParser(
                        argumentContainer, pluginDescriptors, Collections.emptySet()
                );
            } else {
                clp = new CommandLineArgumentParser(argumentContainer);
            }
        } catch (IllegalAccessException | InstantiationException e) {
            // DocumentedFeature does not assume a no-arg constructor unless it is also annotated with CommandLineProgramProperties
            if (workUnit.getCommandLineProperties() != null) {
                throw new RuntimeException(workUnit.getClazz() + " requires a non-arg constructor, because it is annotated with CommandLineProgramProperties ", e);
            }
        }

        workUnit.setProperty("groups", groupMaps);
        workUnit.setProperty("data", featureMaps);

        addHighLevelBindings(workUnit);
        addCommandLineArgumentBindings(workUnit, clp);
        addDefaultPlugins(workUnit, pluginDescriptors);
        addExtraDocsBindings(workUnit);
        addCustomBindings(workUnit);
    }

    /**
     * Add high-level summary information, such as name, summary, description, version, etc.
     *
     * @param workUnit
     */
    protected void addHighLevelBindings(final DocWorkUnit workUnit)
    {
        workUnit.setProperty("name", workUnit.getName());
        workUnit.setProperty("group", workUnit.getGroupName());
        workUnit.setProperty("summary", workUnit.getSummary());

        // Note that these properties are inserted into the toplevel WorkUnit FreeMarker map typed
        // as Strings, with values "true" or "false", but the same entries are typed as Booleans in the
        // FreeMarker map for the index.
        workUnit.setProperty("beta", workUnit.isBetaFeature());
        workUnit.setProperty("experimental", workUnit.isExperimentalFeature());

        workUnit.setProperty("description", getDescription(workUnit));

        workUnit.setProperty("version", getDoclet().getBuildVersion());
        workUnit.setProperty("timestamp", getDoclet().getBuildTimeStamp());
    }

    /**
     * Add any custom freemarker bindings discovered via custom javadoc tags. Subclasses can override this to
     * provide additional custom bindings.
     *
     * @param currentWorkUnit the work unit for the feature being documented
     */
    protected void addCustomBindings(final DocWorkUnit currentWorkUnit) {
        final String tagFilterPrefix = getTagPrefix();
        Arrays.stream(currentWorkUnit.getClassDoc().inlineTags())
                .filter(t -> t.name().startsWith(tagFilterPrefix))
                .forEach(t -> currentWorkUnit.setProperty(t.name().substring(tagFilterPrefix.length()), t.text()));
    }

    /**
     * Subclasses override this to have javadoc tags with this prefix placed in the freemarker map.
     * @return string refix used for custom javadoc tags
     */
    protected String getTagFilterPrefix(){ return ""; }

    /**
     * Add bindings describing related capabilities to currentWorkUnit
     */
    protected void addExtraDocsBindings(final DocWorkUnit currentWorkUnit)
    {
        final List<Map<String, Object>> extraDocsData = new ArrayList<Map<String, Object>>();

        // add in all of the explicitly related extradocs items
        for (final Class<?> extraDocClass : currentWorkUnit.getDocumentedFeature().extraDocs()) {
            final DocWorkUnit otherUnit = getDoclet().findWorkUnitForClass(extraDocClass);
            if (otherUnit != null) {
                extraDocsData.add(
                        new HashMap<String, Object>() {
                            static final long serialVersionUID = 0L;
                            {
                                put("name", otherUnit.getName());
                                put("filename", otherUnit.getTargetFileName());
                            }
                        });
            } else {
                final String msg = String.format(
                        "An \"extradocs\" value (%s) was specified for (%s), but the target was not included in the " +
                        "source list for this javadoc run, or the target has no documentation.",
                        extraDocClass,
                        currentWorkUnit.getName()
                );
                throw new DocException(msg);
            }
        }
        currentWorkUnit.setProperty("extradocs", extraDocsData);
    }

    @SuppressWarnings("unchecked")
    /**
     * Add information about all of the arguments available to toProcess root
     */
    protected void addCommandLineArgumentBindings(final DocWorkUnit currentWorkUnit, final CommandLineArgumentParser clp)
    {
        final Map<String, List<Map<String, Object>>> argMap = createArgumentMap();
        currentWorkUnit.setProperty("arguments", argMap);

        if (clp != null) {
            // do positional arguments, followed by named arguments
            processPositionalArguments(clp, argMap);
            clp.getNamedArgumentDefinitions().stream().forEach(argDef -> processNamedArgument(currentWorkUnit, argMap, argDef));

            // sort the resulting args
            argMap.entrySet().stream().forEach( entry -> entry.setValue(sortArguments(entry.getValue())));

            // Write out the GSON version
            // make a GSON-friendly map of arguments -- uses some hacky casting
            final List<GSONArgument> allGSONArgs = new ArrayList<>();
            for (final Map<String, Object> item : argMap.get("all")) {
                GSONArgument itemGSONArg = new GSONArgument();

                itemGSONArg.populate(item.get("summary").toString(),
                        item.get("name").toString(),
                        item.get("synonyms").toString(),
                        item.get("type").toString(),
                        item.get("required").toString(),
                        item.get("fulltext").toString(),
                        item.get("defaultValue").toString(),
                        item.get("minValue").toString(),
                        item.get("maxValue").toString(),
                        item.get("minRecValue").toString(),
                        item.get("maxRecValue").toString(),
                        item.get("kind").toString(),
                        (List<Map<String, Object>>)item.get("options")
                );
                allGSONArgs.add(itemGSONArg);
            }
            currentWorkUnit.setProperty("gson-arguments", allGSONArgs);
        }
    }

    private String getTagPrefix() {
        String customPrefix = getTagFilterPrefix();
        return customPrefix == null ?
                customPrefix : "@" + customPrefix + ".";

    }

    // Add top level bindings for the default instances for each plugin descriptor
    // NOTE: The default Freemarker template provided by Barclay has no references to plugins, since they're
    // defined by the consumer. However, if a custom freemarker template is being used that DOES contain references
    // to plugins, the corresponding custom doclet class or documentation handler should ensure that the root
    // map is populated with proper values. Otherwise, when the template is run on any documentable instance
    // that happens to not have any plugin instances present, freemarker will throw when it finds the undefined
    // reference.
    protected void addDefaultPlugins(
            final DocWorkUnit currentWorkUnit,
            final List<? extends CommandLinePluginDescriptor<?>> pluginDescriptors)
    {
        for (final CommandLinePluginDescriptor<?> descriptor :  pluginDescriptors) {
            final String descriptorName = descriptor.getDisplayName();  // key/name at the root of the freemarker map
            final HashSet<HashMap<String, Object>> defaultsForPlugins = new HashSet<>();
            currentWorkUnit.setProperty(descriptorName, defaultsForPlugins);

            for (final Object plugin : descriptor.getDefaultInstances()) {
                final HashMap<String, Object> pluginDetails = new HashMap<>();
                pluginDetails.put("name", plugin.getClass().getSimpleName());
                pluginDetails.put("filename", DocletUtils.phpFilenameForClass(plugin.getClass(), getDoclet().getOutputFileExtension()));
                defaultsForPlugins.add(pluginDetails);
            }
        }
    }

    private void processNamedArgument(
            final DocWorkUnit currentWorkUnit,
            final Map<String, List<Map<String, Object>>> args,
            final NamedArgumentDefinition argDef)
    {
        // Rather than including all the doc for all of the arguments of all plugins right in with the main doc for
        // a given DocumentedFeature, we instead exclude them (note that this excludes arguments that are
        // controlled by a plugin, but not by the controlling descriptor itself) by default, on the premise that the
        // plugins themselves are standalone @DocumentedFeatures that have their own doc. Custom doclets can override
        // processNamedArgument and provide an alternative policy.
        if (!argDef.isControlledByPlugin() && (!argDef.isHidden() || getDoclet().showHiddenFeatures())) {
            final Map<String, Object> argMap = new HashMap<>();
            final FieldDoc fieldDoc = getFieldDocForCommandLineArgument(currentWorkUnit, argDef);
            final String argKind = processNamedArgument(argMap, argDef, fieldDoc.commentText());

            // Finalize argument bindings
            args.get(argKind).add(argMap);
            args.get("all").add(argMap);
        }
    }

    private FieldDoc getFieldDocForCommandLineArgument(
            final DocWorkUnit currentWorkUnit,
            final NamedArgumentDefinition argDef)
    {
        // Retrieve the ClassDoc corresponding to this argument directly
        final String declaringClassTypeName = argDef.getUnderlyingField().getDeclaringClass().getTypeName();
        final ClassDoc declaringClassDoc = getDoclet().getRootDoc().classNamed(declaringClassTypeName);

        if (declaringClassDoc == null) {
            throw new DocException(
                    String.format("Can't resolve ClassDoc for declaring class for argument \"%s\" in \"%s\" with qualified name \"%s\"",
                            argDef.getUnderlyingField().getName(),
                            currentWorkUnit.getClassDoc().qualifiedTypeName(),
                            declaringClassTypeName));
        }

        final FieldDoc fieldDoc = getFieldDoc(declaringClassDoc, argDef.getUnderlyingField().getName());

        if (fieldDoc == null) {
            throw new DocException(
                    String.format(
                        "The class \"%s\" is referenced by \"%s\", and must be included in the list of documentation sources.",
                        argDef.getUnderlyingField().getDeclaringClass().getCanonicalName(),
                        currentWorkUnit.getClassDoc().qualifiedTypeName())
            );
        }

        // Try to validate that the FieldDoc we found is the correct one. The qualified name of the argument's
        // FieldDoc should start with the the normalized name of the argument's declaring class.
        final String normalizedTypeName = declaringClassTypeName.replace('$', '.');
        if (!fieldDoc.qualifiedName().startsWith(normalizedTypeName)) {
            // The qualified name for a FieldDoc doesn't include the full path of the class if it corresponds to
            // an argument who's declaring class is local/anonymous
            if (argDef.getUnderlyingField().getDeclaringClass().isLocalClass() || argDef.getUnderlyingField().getDeclaringClass().isAnonymousClass()) {
                logger.warn(String.format(
                        "Field level Javadoc is ignored for local/anonymous class for member field \"%s\" in \"%s\" of type \"%s\".",
                        argDef.getUnderlyingField().getName(),
                        currentWorkUnit.getClazz().getCanonicalName(),
                        declaringClassTypeName));
            } else {
                logger.warn(String.format(
                        "Can't validate FieldDoc \"%s\" for member field \"%s\" in \"%s\" of type \"%s\".",
                        fieldDoc.qualifiedName(),
                        argDef.getUnderlyingField().getName(),
                        currentWorkUnit.getClazz().getCanonicalName(),
                        declaringClassTypeName));
            }
        }
        return fieldDoc;
    }

    private void processPositionalArguments(
            final CommandLineArgumentParser clp,
            final Map<String, List<Map<String, Object>>> args) {
        // first get the positional arguments
        final PositionalArgumentDefinition positionalArgDef = clp.getPositionalArgumentDefinition();
        if (positionalArgDef != null) {
            final Map<String, Object> argBindings = new HashMap<>();
            argBindings.put("kind", "positional");
            argBindings.put("name", NAME_FOR_POSITIONAL_ARGS);
            argBindings.put("summary", positionalArgDef.getPositionalArgumentsAnnotation().doc());
            argBindings.put("fulltext", positionalArgDef.getPositionalArgumentsAnnotation().doc());
            argBindings.put("otherArgumentRequired", "NA");
            argBindings.put("synonyms", "NA");
            argBindings.put("exclusiveOf", "NA");
            argBindings.put("type", argumentTypeString(positionalArgDef.getUnderlyingField().getGenericType()));
            argBindings.put("options", getPossibleValues(positionalArgDef, "positional"));
            argBindings.put("attributes", "NA");
            argBindings.put("required", "yes");
            argBindings.put("minRecValue", "NA");
            argBindings.put("maxRecValue", "NA");
            argBindings.put("minValue", "NA");
            argBindings.put("maxValue", "NA");
            argBindings.put("defaultValue", "NA");
            argBindings.put("minElements", positionalArgDef.getPositionalArgumentsAnnotation().minElements());
            argBindings.put("maxElements", positionalArgDef.getPositionalArgumentsAnnotation().maxElements());

            args.get("positional").add(argBindings);
            args.get("all").add(argBindings);
        }
    }

    /**
     * Return the argument kind (required, advanced, hidden, etc) of this argumentDefinition
     *
     * @param argumentDefinition
     * @return
     */
    private String docKindOfArg(final NamedArgumentDefinition argumentDefinition) {

        // deprecated
        // required (common or otherwise)
        // common optional
        // advanced
        // hidden

        // Required first (after positional, which are separate), regardless of what else it might be
        if (argumentDefinition.isDeprecated()) {
            return "deprecated";
        }
        if (argumentDefinition.isControlledByPlugin()) {
            return "dependent";
        }
        if (!argumentDefinition.isOptional()) {
            return "required";
        }
        if (argumentDefinition.isCommon()) {  // these will all be optional
            return "common";
        }
        if (argumentDefinition.isAdvanced()) {
            return "advanced";
        }
        if (argumentDefinition.isHidden()) {
            return "hidden";
        }
        return "optional";
    }

    /**
     * Create the argument map for holding class arguments
     *
     * @return
     */
    private Map<String, List<Map<String, Object>>> createArgumentMap() {
        final Map<String, List<Map<String, Object>>> args = new HashMap<String, List<Map<String, Object>>>();
        args.put("all", new ArrayList<>());
        args.put("common", new ArrayList<>());
        args.put("positional", new ArrayList<>());
        args.put("required", new ArrayList<>());
        args.put("optional", new ArrayList<>());
        args.put("advanced", new ArrayList<>());
        args.put("dependent", new ArrayList<>());
        args.put("hidden", new ArrayList<>());
        args.put("deprecated", new ArrayList<>());
        return args;
    }

    /**
     * Sorts the individual argument list in unsorted according to CompareArgumentsByName
     *
     * @param unsorted
     * @return
     */
    private List<Map<String, Object>> sortArguments(final List<Map<String, Object>> unsorted) {
        Collections.sort(unsorted, new CompareArgumentsByName());
        return unsorted;
    }

    /**
     * Sort arguments by case-insensitive comparison ignoring the -- and - prefixes
     */
    private class CompareArgumentsByName implements Comparator<Map<String, Object>> {
        public int compare(Map<String, Object> x, Map<String, Object> y) {
            return elt(x).compareTo(elt(y));
        }

        private String elt(Map<String, Object> m) {
            final String v = m.get("name").toString().toLowerCase();
            if (v.startsWith("--"))
                return v.substring(2);
            else if (v.startsWith("-"))
                return v.substring(1);
            else if (v.equals(NAME_FOR_POSITIONAL_ARGS.toLowerCase()))
                return "Positional";
            else
                throw new RuntimeException("Expect to see arguments beginning with at least one -, but found " + v);
        }
    }

    /**
     * Pretty prints value
     *
     * Assumes value != null
     *
     * @param value
     * @return
     */
    private Object prettyPrintValueString(final Object value) {
        if (value instanceof String) {
            return value.equals("") ? "\"\"" : value;
        } else {
            return value.toString();
        }
    }

    /**
     * Recursive helper routine to getFieldDoc()
     */
    private FieldDoc getFieldDoc(final ClassDoc classDoc, final String argumentFieldName) {
        for (final FieldDoc fieldDoc : classDoc.fields(false)) {
            if (fieldDoc.name().equals(argumentFieldName)) {
                return fieldDoc;
            }

            // This can return null, specifically, we can encounter https://bugs.openjdk.java.net/browse/JDK-8033735,
            // which is fixed in JDK9 http://hg.openjdk.java.net/jdk9/jdk9/hotspot/rev/ba8c351b7096.
            final Field field = DocletUtils.getFieldForFieldDoc(fieldDoc);
            if (field == null) {
                logger.warn(
                    String.format(
                        "Could not access the field definition for %s while searching for %s, presumably because the field is inaccessible",
                        fieldDoc.name(),
                            argumentFieldName)
                );
            } else if (field.isAnnotationPresent(ArgumentCollection.class)) {
                final ClassDoc typeDoc = getDoclet().getRootDoc().classNamed(fieldDoc.type().qualifiedTypeName());
                if (typeDoc == null) {
                    throw new DocException("Tried to get javadocs for ArgumentCollection field " +
                            fieldDoc + " but couldn't find the class in the RootDoc");
                } else {
                    FieldDoc result = getFieldDoc(typeDoc, argumentFieldName);
                    if (result != null) {
                        return result;
                    }
                    // else keep searching
                }
            }
        }

        // if we didn't find it here, wander up to the superclass to find the field
        if (classDoc.superclass() != null) {
            return getFieldDoc(classDoc.superclass(), argumentFieldName);
        }

        return null;
    }

    /**
     * Returns a Pair of (main, synonym) names for argument with fullName s1 and
     * shortName s2.
     *
     * Previously we had it so the main name was selected to be the longest of the two, provided
     * it didn't exceed MAX_DISPLAY_NAME, in which case the shorter was taken. But we now disable
     * the length-based name rearrangement in order to maintain consistency in the Docs table.
     *
     * This may cause messed up spacing in the CLI-help display but we don't care as much about that
     * since more users use the online Docs for looking up arguments.
     *
     * @param s1 the short argument name without -, or null if not provided
     * @param s2 the long argument name without --, or null if not provided
     * @return A pair of fully qualified names (with - or --) for the argument.  The first
     *         element is the primary display name while the second (potentially null) is a
     *         synonymous name.
     */
    private Pair<String, String> displayNames(String s1, String s2) {
        s1 = ((s1 == null) || (s1.length() == 0)) ? null : "-" + s1;
        s2 = ((s2 == null) || (s2.length() == 0)) ? null : "--" + s2;

        if (s1 == null) return Pair.of(s2, null);
        if (s2 == null) return Pair.of(s1, null);

        return Pair.of(s2, s1);
    }

    /**
     * Returns a human readable string that describes the Type type of an argument.
     *
     * This will include parametrized types, so that Set{T} shows up as Set(T) and not
     * just Set in the docs.
     *
     * @param type
     * @return String representing the argument type
     */
    protected String argumentTypeString(final Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            final List<String> subs = new ArrayList<>();
            for (final Type actualType : parameterizedType.getActualTypeArguments())
                subs.add(argumentTypeString(actualType));
            return argumentTypeString(((ParameterizedType) type).getRawType()) + "[" + String.join(",", subs) + "]";
        } else if (type instanceof GenericArrayType) {
            return argumentTypeString(((GenericArrayType) type).getGenericComponentType()) + "[]";
        } else if (type instanceof WildcardType) {
            throw new RuntimeException("We don't support wildcards in arguments: " + type);
        } else if (type instanceof Class<?>) {
            return ((Class) type).getSimpleName();
        } else {
            throw new DocException("Unknown type: " + type);
        }
    }

    /**
     * Populate a FreeMarker map with attributes of an argument
     *
     * @param argBindings
     * @param argDef
     * @param fieldCommentText
     * @return
     */
    protected String processNamedArgument(
            final Map<String, Object> argBindings,
            final NamedArgumentDefinition argDef,
            final String fieldCommentText) {
        // Retrieve default value
        final Object fieldValue = argDef.getArgumentValue();
        argBindings.put("defaultValue",
                fieldValue == null ?
                        argDef.getDefaultValueAsString() :
                        prettyPrintValueString(fieldValue));

        if (fieldValue instanceof Number) {
            // Retrieve min and max / hard and soft value thresholds for numeric args
            argBindings.put("minValue", argDef.getMinValue());
            argBindings.put("maxValue", argDef.getMaxValue());
            argBindings.put("minRecValue",
                    argDef.getMinRecommendedValue() != Double.NEGATIVE_INFINITY ?
                            argDef.getMinRecommendedValue() :
                            "NA");
            argBindings.put("maxRecValue",
                    argDef.getMaxRecommendedValue() != Double.POSITIVE_INFINITY ?
                            argDef.getMaxRecommendedValue() :
                            "NA");
        } else {
            argBindings.put("minValue", "NA");
            argBindings.put("maxValue", "NA");
            argBindings.put("minRecValue", "NA");
            argBindings.put("maxRecValue", "NA");
        }

        // Add in the number of times you can specify it:
        argBindings.put("minElements", argDef.getMinElements());
        argBindings.put("maxElements", argDef.getMaxElements());

        final String kind = docKindOfArg(argDef);
        argBindings.put("kind", kind);

        final Pair<String, String> names = displayNames(argDef.getShortName(), argDef.getLongName());
        argBindings.put("name", names.getLeft());
        argBindings.put("synonyms", names.getRight() != null ? names.getRight() : "NA");
        argBindings.put("required", argDef.isOptional() ? "no": "yes") ;
        argBindings.put("type", argumentTypeString(argDef.getUnderlyingField().getGenericType()));

        // summary and fulltext
        argBindings.put("summary", argDef.getDocString() != null ? argDef.getDocString() : "");
        argBindings.put("fulltext", fieldCommentText);

        // Does this argument interact with any others?
        if (argDef.isControlledByPlugin()) {
            argBindings.put("otherArgumentRequired",
                    argDef.getContainingObject().getClass().getSimpleName().length() == 0 ?
                            argDef.getContainingObject().getClass().getName() :
                            argDef.getContainingObject().getClass().getSimpleName());
        } else {
            argBindings.put("otherArgumentRequired", "NA");
        }

        argBindings.put("exclusiveOf",
                argDef.getMutexTargetList() != null && !argDef.getMutexTargetList().isEmpty() ?
                    String.join(", ", argDef.getMutexTargetList()) :
                    "NA");

        // possible values
        argBindings.put("options", getPossibleValues(argDef, argDef.getLongName()));

        final List<String> attributes = new ArrayList<>();
        if (!argDef.isOptional()) {
            attributes.add("required");
        }
        if (argDef.isDeprecated()) {
            attributes.add("deprecated");
        }
        argBindings.put("attributes", attributes.size() > 0 ? String.join(", ", attributes) : "NA");

        return kind;
    }

    /**
     * Return a (possibly empty) list of possible values that can be specified for this argument. Each
     * value in the list is a map with "name" and "summary" keys, used to handle enums that implement the
     * {@link CommandLineParser.ClpEnum} interface.
     * @param argDef {ArgumentDefinition}
     * @return list of possible options for {@code argDef}, may not be null
     */
    private List<Map<String, Object>> getPossibleValues(final ArgumentDefinition argDef, final String displayName) {
        Utils.nonNull(argDef);

        final Field underlyingField = argDef.getUnderlyingField();
        Class<?> targetClass = underlyingField.getType();

        // enum options
        // if this argument is a Collection, get the type param to see if its an enum
        if (argDef.isCollection() && underlyingField.getGenericType() instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) underlyingField.getGenericType();
            final Type types[] = parameterizedType.getActualTypeArguments();
            if (types.length != 1) { // if there are multiple (or no!) type parameters, give up
                logger.warn(String.format(
                        "Unable to determine possible values for a collection (%s) with generic type parameter(s)",
                        displayName));
                return Collections.emptyList();
            }
            try {
                // use the type param of the underlying field as the target instead
                targetClass = Class.forName(types[0].getTypeName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(String.format("No class found for type parameter (%s) used for argument (%s)",
                                types[0].getTypeName(), displayName), e);
            }
        }

        return targetClass.isEnum() ?
                docForEnumArgument(targetClass) :
                Collections.emptyList();
    }

    /**
     * Helper routine that provides a FreeMarker map for an enumClass, grabbing the
     * values of the enum and their associated javadoc documentation.
     *
     * @param enumClass
     * @return
     */
    private List<Map<String, Object>> docForEnumArgument(final Class<?> enumClass) {
        final ClassDoc doc = this.getDoclet().getClassDocForClass(enumClass);
        if ( doc == null ) {
            throw new RuntimeException("Tried to get docs for enum " + enumClass + " but got null instead");
        }

        final Set<String> enumConstantFieldNames = enumConstantsNames(enumClass);
        final List<Map<String, Object>> bindings = new ArrayList<Map<String, Object>>();
        for (final FieldDoc fieldDoc : doc.fields(false)) {
            if (enumConstantFieldNames.contains(fieldDoc.name()) ) {
                bindings.add(
                        new HashMap<String, Object>() {
                            static final long serialVersionUID = 0L;
                            {
                                put("name", fieldDoc.name());
                                put("summary", fieldDoc.commentText());
                            }
                        }
                );
            }
        }

        return bindings;
    }

    /**
     * @return a non-null set of fields that are enum constants
     */
    private Set<String> enumConstantsNames(final Class<?> enumClass) {
        final Set<String> enumConstantFieldNames = new HashSet<String>();

        for ( final Field field : enumClass.getFields() ) {
            if ( field.isEnumConstant() )
                enumConstantFieldNames.add(field.getName());
        }

        return enumConstantFieldNames;
    }
}
