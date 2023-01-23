package org.broadinstitute.barclay.help;

import jdk.javadoc.doclet.DocletEnvironment;
import org.apache.commons.lang3.tuple.Pair;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.*;
import org.broadinstitute.barclay.help.scanners.JavaLanguageModelScanners;
import org.broadinstitute.barclay.utils.Utils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.lang.reflect.*;
import java.util.*;


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
                summary = JavaLanguageModelScanners.getDocCommentFirstSentence(getDoclet().getDocletEnv(), workUnit.getDocElement());
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
        return JavaLanguageModelScanners.getDocComment(getDoclet().getDocletEnv(), currentWorkUnit.getDocElement());
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
            final Object argumentContainer = workUnit.getClazz().getDeclaredConstructor().newInstance();
            if (argumentContainer instanceof CommandLinePluginProvider) {
                pluginDescriptors = ((CommandLinePluginProvider) argumentContainer).getPluginDescriptors();
                clp = new CommandLineArgumentParser(
                        argumentContainer, pluginDescriptors, Collections.emptySet()
                );
            } else {
                clp = new CommandLineArgumentParser(argumentContainer);
            }
            workUnit.setProperty("groups", groupMaps);
            workUnit.setProperty("data", featureMaps);

            addHighLevelBindings(workUnit);
            addCommandLineArgumentBindings(workUnit, clp);
            addDefaultPlugins(workUnit, pluginDescriptors);
            addExtraDocsBindings(workUnit);
            addCustomBindings(workUnit);
        } catch (final NoSuchMethodException e) {
            throw new CommandLineException.CommandLineParserInternalException(
                    String.format ("DocumentedFeature class %s does not have the required no argument constructor",
                            workUnit.getClazz()), e);
        } catch (final InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new CommandLineException.CommandLineParserInternalException(
                    String.format ("DocumentedFeature class %s cannot be instantiated",
                            workUnit.getClazz()), e);
        }
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
        workUnit.setProperty(TemplateProperties.FEATURE_DEPRECATED, workUnit.isDeprecatedFeature());
        workUnit.setProperty(TemplateMapConstants.FEATURE_DEPRECATION_DETAIL, workUnit.getDeprecationDetail());

        //this property is called "description" for work units, but "fulltext" for arguments). ideally
        // these would be unified, but it would require a lot of downstream changes to templates and test
        // result expected files
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
        if (tagFilterPrefix != null) {
            final Map<String, List<String>> parts = JavaLanguageModelScanners.getUnknownInlineTags(
                    getDoclet().getDocletEnv(),
                    currentWorkUnit.getDocElement());
            // create properties for any custom tags
            parts.entrySet()
                    .stream()
                    // we only want the tags that start with a (doclet specific) custom tag prefix; skip the
                    // leading '@' since the scanner strips that out
                    .filter(e -> e.getKey().startsWith(tagFilterPrefix.substring(1)))
                    .forEach(e -> currentWorkUnit.setProperty(
                            e.getKey().substring(tagFilterPrefix.substring(1).length()),
                            String.join(" ", (e.getValue()))));
        }
    }

    /**
     * Subclasses override this to have javadoc tags with this prefix placed in the freemarker map.
     * @return string prefix used for custom javadoc tags
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
            // make a GSON-friendly map of arguments
            final List<GSONArgument> allGSONArgs = new ArrayList<>();
            for (final Map<String, Object> detailMap : argMap.get("all")) {
                allGSONArgs.add(new GSONArgument(detailMap));
            }
            currentWorkUnit.setProperty("gson-arguments", allGSONArgs);
        }
    }

    private String getTagPrefix() {
        final String customPrefix = getTagFilterPrefix();
        return customPrefix == null || customPrefix.length() == 0 ?
                null :
                "@" + customPrefix + ".";

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

    /**
     * Add the named argument {@code argDef}to the property map if applicable.
     * @param currentWorkUnit current work unit
     * @param args the freemarker arg map
     * @param argDef the arg to add
     */
    protected void processNamedArgument(
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
            final String commentText = getDocCommentForField(currentWorkUnit, argDef);
            final String argKind = processNamedArgument(argMap, argDef, commentText);

            // Finalize argument bindings
            args.get(argKind).add(argMap);
            args.get("all").add(argMap);
            if (argDef.isDeprecated()) {
                args.get(TemplateProperties.ARGUMENT_DEPRECATED).add(argMap);
            }
        }
    }

    private String getDocCommentForField(final DocWorkUnit workUnit, final NamedArgumentDefinition argDef) {
        // first, see if the field (@Argument) we're looking for is declared directly in the work unit's class
        Element fieldElement = JavaLanguageModelScanners.getElementForField(
                getDoclet().getDocletEnv(),
                workUnit.getDocElement(),
                argDef.getUnderlyingField(),
                ElementKind.FIELD);
        if (fieldElement == null) {
            // the field isn't defined directly in the workunit's enclosing class/element, so it must be
            // defined in a class that is referenced by the workunit's enclosing class. find that class and get
            // it's type element
            Class<?> containingClass = argDef.getUnderlyingField().getDeclaringClass();
            if (containingClass != null) {
                String className = containingClass.getCanonicalName();
                // className can be null if the containing class is an anonymous class, in which case we don't
                // want to traverse the containment hierarchy because we'll just wind up back at the work unit
                // class, which we don't want, so in order to be compatible with the way the old javadoc used
                // to work, just bail...
                if (className != null) {
                    final Element classElement = getDoclet().getDocletEnv().getElementUtils().getTypeElement(className);
                    fieldElement = JavaLanguageModelScanners.getElementForField(
                            getDoclet().getDocletEnv(),
                            classElement,
                            argDef.getUnderlyingField(),
                            ElementKind.FIELD);
                }
            }
        }

        String comment = "";
        if (fieldElement != null) {
            comment = JavaLanguageModelScanners.getDocCommentWithoutTags(getDoclet().getDocletEnv(), fieldElement);
        }
        return comment;

    }

    /**
     * Process any positional arguments for this tool by populating and adding an argument bindings map to the
     * top level freemarker map for the positional arguments, if they are defined by the tool being processed.
     * If no positional arguments are defined, do nothing.
     *
     * @param clp the instantiated {@link CommandLineArgumentParser} in use for this run. If the current
     *            command tool has positional arguments, the definition can be retrieved using
     *            {@link CommandLineArgumentParser#getPositionalArgumentDefinition()}.
     * @param args the top level freemarker map for this tool, to be populated with an argument map for
     *            positional args
     */
    protected void processPositionalArguments(
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
            argBindings.put("collection", positionalArgDef.isCollection());
            argBindings.put(TemplateProperties.ARGUMENT_DEPRECATED, positionalArgDef.isDeprecated());
            if (positionalArgDef.isDeprecated()) {
                argBindings.put(TemplateProperties.ARGUMENT_DEPRECATION_DETAIL, positionalArgDef.getDeprecationDetail());
                args.get(TemplateProperties.ARGLIST_TYPE_DEPRECATED).add(argBindings);
            }
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

        // required (common or otherwise)
        // common optional
        // advanced
        // hidden

        // Required first (after positional, which are separate), regardless of what else it might be
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
        args.put(TemplateProperties.ARGLIST_TYPE_DEPRECATED, new ArrayList<>());
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
     * Process a single named argument by populating the argument bindings map for the top level freemarker map
     * for this argument.
     *
     * @param argBindings the argument property bindings map to be populated for this argument
     * @param argDef the {@link NamedArgumentDefinition} for this argument
     * @param fieldCommentText the comment text for the underlying Field for the argument, if any
     * @return the "kind" category for this argument as used by the freemarker template ("deprecated", "required"
     * (common or otherwise), "common" (optional), "advanced", or "hidden")
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
        argBindings.put(TemplateProperties.ARGUMENT_DEPRECATED, argDef.isDeprecated());
        if (argDef.isDeprecated()) {
            argBindings.put(TemplateProperties.ARGUMENT_DEPRECATION_DETAIL, argDef.getDeprecationDetail());
            attributes.add(TemplateMapConstants.ARG_DEPRECATED_ATTRIBUTE);
        }
        argBindings.put("attributes", attributes.size() > 0 ? String.join(", ", attributes) : "NA");
        argBindings.put("collection", argDef.isCollection());
        return kind;
    }

    /**
     * Return a (possibly empty) list of possible values that can be specified for this argument. Each
     * value in the list is a map with "name" and "summary" keys.
     * @param argDef {ArgumentDefinition}
     * @return list of possible options for {@code argDef}. May be empty. May may not be null.
     */
    private List<Map<String, String>> getPossibleValues(final ArgumentDefinition argDef, final String displayName) {
        Utils.nonNull(argDef);

        final Field underlyingField = argDef.getUnderlyingField();
        Class<?> targetClass = underlyingField.getType();

        // enum options
        if (argDef.isCollection() && underlyingField.getGenericType() instanceof ParameterizedType) {
            // if this argument is a Collection (including an EnumSet), use the type of the generic type
            // parameter as the target class, instead of the collection class itself
            final Type typeParamType = underlyingField.getGenericType();
            final ParameterizedType pType = (ParameterizedType) typeParamType;
            final Type genericTypes[] = pType.getActualTypeArguments();
            if (genericTypes.length != 1 || genericTypes[0] instanceof ParameterizedType) {
                return Collections.emptyList();
            }
            try {
                targetClass = Class.forName(genericTypes[0].getTypeName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(String.format("No class found for type parameter (%s) used for argument (%s)",
                        genericTypes[0].getTypeName(), displayName), e);
            }
        }

        return targetClass.isEnum() ?
                docForEnumArgument(argDef, targetClass) :
                Collections.emptyList();
    }

    /**
     * Helper routine that provides a FreeMarker map for an enumClass, grabbing the
     * values of the enum and their associated javadoc or {@link CommandLineArgumentParser.ClpEnum} documentation.
     *
     * @param argDef argument definition for the argument of the enum type
     * @param enumClass an enum Class that may optionally implement {@link CommandLineArgumentParser.ClpEnum}
     * @return a List of maps with keys for "name" and "summary" for each of the class's possible enum constants
     * @param <T> type param for this enum
     *
     */
    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> List<Map<String, String>> docForEnumArgument(
            final ArgumentDefinition argDef,
            final Class<?> enumClass) {
        final List<Map<String, String>> bindings = new ArrayList<>();
        final T[] enumConstants = (T[]) enumClass.getEnumConstants();
        if (CommandLineArgumentParser.ClpEnum.class.isAssignableFrom(enumClass)) {
            for ( final T enumConst : enumConstants ) {
                bindings.add(createPossibleValuesMap(
                        enumConst.name(),
                        ((CommandLineArgumentParser.ClpEnum) enumConst).getHelpDoc()));
            }
        } else {
            final String canonicalClassName = enumClass.getCanonicalName();
            final Element enumClassElement = getDoclet().getDocletEnv().getElementUtils().getTypeElement(canonicalClassName);
            if (enumClassElement != null ) {
                for ( final T enumConst : enumConstants ) {
                    final Field enumConstField = getFieldForEnumConstant(enumClass, enumConst.name());
                    if (enumConstField != null) {
                        final Element fieldElement = JavaLanguageModelScanners.getElementForField(
                                getDoclet().getDocletEnv(),
                                enumClassElement,
                                enumConstField,
                                ElementKind.ENUM_CONSTANT);
                        if (fieldElement != null) {
                            final String comment = JavaLanguageModelScanners.getDocCommentWithoutTags(
                                    getDoclet().getDocletEnv(),
                                    fieldElement);
                            bindings.add(createPossibleValuesMap(
                                    enumConst.name(),
                                    comment));
                        } else {
                            bindings.add(createPossibleValuesMap(
                                    enumConst.name(),
                                    "")); // empty string since there is no detail
                        }
                    } else {
                        bindings.add(createPossibleValuesMap(
                                enumConst.name(),
                                "No documentation available"));
                    }
                }
            }
        }

        return bindings;
    }

    private <T> Field getFieldForEnumConstant(final Class<?> enumClass, final String enumConstantName) {
        for (final Field enumClassField : enumClass.getFields()) {
            if (enumClassField.getName().equals(enumConstantName)) {
                return enumClassField;
            }
        }
        return null;
    }

    private HashMap<String, String> createPossibleValuesMap(final String name, final String summary) {
        return new HashMap<String, String>() {
            static final long serialVersionUID = 0L;
            {
                put("name", name);
                put("summary", summary);
            }
        };
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
