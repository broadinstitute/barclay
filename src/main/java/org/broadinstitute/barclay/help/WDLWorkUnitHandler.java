package org.broadinstitute.barclay.help;

import org.apache.commons.lang3.tuple.Pair;

import org.broadinstitute.barclay.argparser.*;
import org.broadinstitute.barclay.argparser.RuntimeProperties;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * The WDL work unit handler. Its main task is to convert the types for all arguments for a given work
 * unit (tool) from Java types to WDL-compatible types by updating the freemarker map with the transformed types.
 */
public class WDLWorkUnitHandler extends DefaultDocWorkUnitHandler {
    private static final String GATK_FREEMARKER_TEMPLATE_NAME = "wdlToolTemplate.wdl.ftl";

    // keep track of tool outputs (Map<argName, argType>)
    private Map<String, String> runtimeOutputs = new HashMap<>();

    // keep track of companion files (Map<argName, List<companionNames>) for a single argument
    private Map<String, List<String>> companionFiles = new HashMap<>();

    /**
     * name of the top level freemarker map entry for runtime properties
     */
    public static final String RUNTIME_PROPERTIES = "runtimeProperties";
    /**
     * runtime memory property (stored in "runtimeProperties", used to initialize arg value in JSON)
     */
    public static final String RUNTIME_PROPERTY_MEMORY = "memoryRequirements";
    /**
     * runtime disks property (stored in "runtimeProperties", used to initialize arg value in JSON)
     */
    public static final String RUNTIME_PROPERTY_DISKS = "diskRequirements";
    /**
     * name of the top level freemarker map entry for runtime outputs
     */
    public static final String RUNTIME_OUTPUTS = "runtimeOutputs";
    /**
     * name of the top level freemarker map entry for companion resources
     */
    public static final String COMPANION_RESOURCES = "companionResources";

    /**
     * the name used in the freemarker template as an argument placeholder for positional args; this constant
     * must be kept in sync with the corresponding one used in the template
     */
    public static final String POSITIONAL_ARGS = "positionalArgs";

    public WDLWorkUnitHandler(final HelpDoclet doclet) {
        super(doclet);
    }

    /**
     * @param workUnit the DocWorkUnit object being processed
     * @return the name of a the freemarker template to be used for the class being documented.
     * Must reside in the folder passed to the Barclay Doclet via the "-settings-dir" parameter to
     * Javadoc.
     */
    @Override
    public String getTemplateName(final DocWorkUnit workUnit) { return GATK_FREEMARKER_TEMPLATE_NAME; }

    @Override
    protected void addCommandLineArgumentBindings(final DocWorkUnit currentWorkUnit, final CommandLineArgumentParser clp) {
        super.addCommandLineArgumentBindings(currentWorkUnit, clp);

        // add the property used by the WDL freemarker template to emit workflow outputs
        final Map<String, String> runtimeOutputsForFreemarker = new HashMap<>();
        runtimeOutputsForFreemarker.putAll(runtimeOutputs);
        currentWorkUnit.getRootMap().put(RUNTIME_OUTPUTS, runtimeOutputsForFreemarker);
        runtimeOutputs.clear();

        // synthesize arguments for the companion resources
        @SuppressWarnings("unchecked")
        final Map<String, List<Map<String, Object>>> argMap =
                (Map<String, List<Map<String, Object>>>) currentWorkUnit.getRootMap().get("arguments");
        final List<Map<String, Object>> allArgsMap = argMap.get("all");
        // <arg, List<companions>>
        final Map<String, List<Map<String, Object>>> argCompanionResourceArgMaps = new HashMap<>();
        allArgsMap.forEach(m -> {
            final String argName = (String) m.get("name");
            final Map<String, Object> argPropertiesMap = m;
            final List<Map<String, Object>> argCompanions = new ArrayList<>();
            if (companionFiles.containsKey(argName)) {
                for (final String companion : companionFiles.get(argName)) {
                    // the companion files retain all properties of the original arg, except for name and synonyms
                    final Map<String, Object> companionMap = new HashMap<>();
                    companionMap.putAll(argPropertiesMap);
                    companionMap.put("name", companion);
                    companionMap.put("synonyms", "");
                    companionMap.put("summary", "Companion resource for: " + companionMap.get("summary"));
                    argCompanions.add(companionMap);
                }
                argCompanionResourceArgMaps.put(argName, argCompanions);
            }
        });

        // add the property used by the WDL freemarker template for companion resources
        currentWorkUnit.getRootMap().put(COMPANION_RESOURCES, argCompanionResourceArgMaps);
    }

    /**
     * Add the named argument {@code argDed}to the property map if applicable.
     * @param currentWorkUnit current work unit
     * @param args the freemarker arg map
     * @param argDef the arg to add
     */
    protected void processNamedArgument(
            final DocWorkUnit currentWorkUnit,
            final Map<String, List<Map<String, Object>>> args,
            final NamedArgumentDefinition argDef)
    {
        // for WDL gen, we don't want the special args such as --help or --version to show up in the
        // WDL or JSON input files
        if (!argDef.getUnderlyingField().getDeclaringClass().equals(SpecialArgumentsCollection.class)) {
            super.processNamedArgument(currentWorkUnit, args, argDef);
        }
    }

    @Override
    protected String processNamedArgument(
            final Map<String, Object> argBindings,
            final NamedArgumentDefinition argDef,
            final String fieldCommentText) {
        final String argCategory = super.processNamedArgument(argBindings, argDef, fieldCommentText);

        // replace the java type of the argument with the appropriate wdl type (don't pass WorkflowResource
        // for this call site
        final String wdlType = getWDLTypeForArgument(argDef, null, (String) argBindings.get("type"));

        // for args that are output workflow resources and have a WDL type of File, we need to use a String as
        // the *input* type to prevent the workflow manager from attempting to localize them on input, so
        // create a separate property in the property map for use by the template in input definitions
        final WorkflowResource workflowResource = argDef.getUnderlyingField().getAnnotation(WorkflowResource.class);
        final String wdlInputType = getWDLTypeForArgument(argDef, workflowResource, (String) argBindings.get("type"));
        argBindings.put("type", wdlType);
        argBindings.put("wdlinputtype", wdlInputType);

        // Store the actual (unmodified) arg name that the app will recognize, for use in the task command block.
        // Then generate a WDL-friendly name if necessary ("input" and "output" are reserved words in WDL and
        // can't be used for arg names; also WDL doesn't accept embedded "-" for variable names, so use a non-kebab
        // name with an underscore) for use in the rest of the WDL source.
        final String actualArgName = (String) argBindings.get("name");
        argBindings.put("actualArgName", actualArgName);
        String wdlName = "--" + transformJavaNameToWDLName(actualArgName.substring(2));
        argBindings.put("name", wdlName);

        // finally, keep track of the outputs
        if (workflowResource != null) {
            if (workflowResource.output()) {
                runtimeOutputs.put(wdlName, wdlType);
            }
            for (final String companion : workflowResource.companionResources()) {
                final String companionArgOption = "--" + companion;
                companionFiles.merge(
                        wdlName,
                        Collections.singletonList(companionArgOption),
                        (oldList, newList) -> {
                            final List<String> mergedList = new ArrayList<>();
                            mergedList.addAll(oldList);
                            mergedList.add(companionArgOption);
                            return mergedList;
                        });
            }
        }

        return argCategory;
    }

    @Override
    protected void processPositionalArguments(
            final CommandLineArgumentParser clp,
            final Map<String, List<Map<String, Object>>> argBindings) {
        super.processPositionalArguments(clp, argBindings);

        final PositionalArgumentDefinition argDef = clp.getPositionalArgumentDefinition();
        if (argDef != null) {
            final WorkflowResource workflowResource = argDef.getUnderlyingField().getAnnotation(WorkflowResource.class);

            // replace the java type of the argument with the appropriate wdl type
            final String wdlType = getWDLTypeForArgument(argDef, null, (String) argBindings.get("positional").get(0).get("type"));

            // for args that are output workflow resources and have a WDL type of File, we need to use a String as
            // the *input* type to prevent the workflow manager from attempting to localize them on input, so
            // create a separate property in the property map for use by the template in input definitions
            final String wdlInputType = getWDLTypeForArgument(argDef, workflowResource, (String) argBindings.get("positional").get(0).get("type"));
            argBindings.get("positional").get(0).put("type", wdlType);
            argBindings.get("positional").get(0).put("wdlinputtype", wdlInputType);

            // finally, keep track of the outputs
            if (workflowResource != null) {
                if (workflowResource.output()) {
                    runtimeOutputs.put(POSITIONAL_ARGS, wdlType);
                }
                for (final String companion : workflowResource.companionResources()) {
                    final String companionArgOption = "--" + companion;
                    companionFiles.merge(
                            POSITIONAL_ARGS,
                            Collections.singletonList(companionArgOption),
                            (oldList, newList) -> {
                                final List<String> mergedList = new ArrayList<>();
                                mergedList.addAll(oldList);
                                mergedList.add(companionArgOption);
                                return mergedList;
                            });
                }
            }
        }
    }

    /**
     * Return a String that represents the WDL type for this arg, which is a variant of the  user-friendly doc
     * type chosen by the doc system. Interrogates the structured NamedArgumentDefinition type to transform and
     * determine the resulting WDL type.
     *
     * @param argDef the Barclay NamedArgumentDefinition for this arg
     * @param argDocType the display type as chosen by the Barclay doc system for this arg. this is what
     * @return
     */
    protected String getWDLTypeForArgument(
            final ArgumentDefinition argDef,
            final WorkflowResource workflowResource,
            final String argDocType
    ) {
        final Field argField = argDef.getUnderlyingField();
        final Class<?> argumentClass = argField.getType();

        // start the data type chosen by the doc system and transform that based on the underlying
        // java class/type
        String wdlType = argDocType;

        // if the underlying field is a collection type; it needs to map to "Array", and then the
        // type param has to be converted to a WDL type
        if (argDef.isCollection()) {
            Pair<String, String> conversionPair = transformToWDLCollectionType(argumentClass);
            if (conversionPair != null) {
                wdlType = wdlType.replace(conversionPair.getLeft(), conversionPair.getRight());
            } else {
                throw new IllegalArgumentException(
                        String.format(
                                "Unrecognized collection type %s for argument %s in work unit %s." +
                                        "Argument collection type must be one of List or Set.",
                                argumentClass,
                                argField.getName(),
                                argField.getDeclaringClass()));
            }

            // Convert any Collection type params; this only handles a single generic type parameter (i.e List<T>),
            // where type T can in turn be a generic type, again with a single type param. This is sufficient to
            // accommodate all existing cases, with the most complex being List<FeatureInput<T>>. If this code
            // encounters anything more complex, it will throw. Supporting more deeply nested generic types will
            // require additional code.
            final Type typeParamType = argField.getGenericType();
            if (typeParamType instanceof ParameterizedType) {
                final ParameterizedType pType = (ParameterizedType) typeParamType;
                final Type genericTypes[] = pType.getActualTypeArguments();
                if (genericTypes.length != 1) {
                    throw new RuntimeException(String.format(
                            "Generating WDL for tools with arguments that have types that require multiple type parameters is not supported " +
                                    "(class %s for arg %s in %s has multiple type parameters).",
                            argumentClass,
                            argField.getName(),
                            argField.getDeclaringClass()));
                }
                ParameterizedType pType2 = null;
                Type genericTypes2[];
                Class<?> nestedTypeClass;
                try {
                    // we could have nested generic types, like "List<FeatureInput<VariantContext>>", which needs
                    // to translate to "List<File>"
                    if (genericTypes[0] instanceof ParameterizedType) {
                        pType2 = (ParameterizedType) genericTypes[0];
                        genericTypes2 = pType2.getActualTypeArguments();
                        if (genericTypes2.length != 1) {
                            throw new RuntimeException(String.format(
                                    "Generating WDL for tools with args with multiple type parameters is not supported " +
                                            "(class %s for arg %s in %s has multiple type parameters).",
                                    argumentClass,
                                    argField.getName(),
                                    argField.getDeclaringClass()));
                        }

                        nestedTypeClass = Class.forName(pType2.getRawType().getTypeName());
                        wdlType = convertJavaTypeToWDLType(nestedTypeClass, wdlType, argField.getDeclaringClass().toString());
                        if (workflowResource != null && workflowResource.output() && wdlType.equals("File")) {
                            wdlType = "String";
                        }
                    } else {
                        nestedTypeClass = Class.forName(genericTypes[0].getTypeName());
                        wdlType = convertJavaTypeToWDLType(nestedTypeClass, wdlType, argField.getDeclaringClass().toString());
                        if (workflowResource != null && workflowResource.output() && wdlType.equals("File")) {
                            wdlType = "String";
                        }
                    }
                    return wdlType;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(String.format(
                            "WDL generation can't find class %s for %s",
                            pType2.getRawType().toString(),
                            argField.getDeclaringClass()), e);
                }
            } else {
                throw new RuntimeException(String.format(
                        "Generic type must have a ParameterizedType (class %s for argument %s/%s)",
                        argumentClass,
                        argField.getName(),
                        argField.getDeclaringClass()));
            }
        }

        wdlType = convertJavaTypeToWDLType(argumentClass, wdlType, argField.getDeclaringClass().toString());
        if (workflowResource != null && workflowResource.output() && wdlType.equals("File")) {
            wdlType = "String";
        }

        return wdlType;
    }

    /**
     * Given a Java class representing the underlying field  type of an argument, and a human readable doc type,
     * convert the docType to a WDL type.
     *
     * @param argumentClass the Class for the underlying field of the argument being converted
     * @param docType a string representing the human readable type assigned by the Barclay doc system
     * @param contextMessage a message describing the context for this argument, used in error reporting
     * @return the docType string transformed to the corresponding WDL type
     */
    protected String convertJavaTypeToWDLType(final Class<?> argumentClass, final String docType, final String contextMessage) {
        String convertedWDLType;
        final Pair<String, String> typeConversionPair = transformToWDLType(argumentClass);
        if (typeConversionPair != null) {
            convertedWDLType = docType.replace(typeConversionPair.getKey(), typeConversionPair.getValue());
        } else if (argumentClass.isEnum()) {
            //NOTE: we could emit structs for all the ENUM types into a shared/common WDL file ?
            convertedWDLType = docType.replace(argumentClass.getSimpleName(), "String");
        } else {
            throw new RuntimeException(
                    String.format(
                            "Can't generate a WDL type for %s, in work unit %s to a WDL type",
                            argumentClass,
                            contextMessage));
        }
        return convertedWDLType;
    }

    /**
     * Given an argument class, return a String pair representing the string that should be replaced (the Java type),
     * and the string to substitute (the corresponding WDL type), i.e., for an argument with type Java Integer.class,
     * return the Pair ("Integer", "Int") to convert from the Java type to the corresponding WDL type.
     *
     * @param argumentClass Class of the argument being converter
     * @return a String pair representing the original and replacement type text, or null if no conversion is available
     */
    protected Pair<String, String> transformToWDLType(final Class<?> argumentClass) {
        return WDLTransforms.transformToWDLType(argumentClass);
    }

    /**
     * Given {@code candidateName}, transform/mangle the name if it is a WDL reserved word, otherwise
     * return {@code candidateName}.
     *
     * @param candidateName
     * @return mangled name if {@code candidateName} is a WDL reserved word, otherwise {@code candidateName}
     */
    protected String transformJavaNameToWDLName(final String candidateName) {
        return WDLTransforms.transformJavaNameToWDLName(candidateName);
    }

    /**
     * Given a Java collection class, return a String pair representing the string that should be replaced (the Java type),
     * and the string to substitute (the corresponding WDL type), i.e., for an argument with type Java List.class,
     * return the Pair ("List", "Array") to convert from the Java type to the corresponding WDL collection type.
     * @param argumentCollectionClass collection Class of the argument being converter
     * @return a String pair representing the original and replacement type text, or null if no conversion is available
     */
    protected Pair<String, String> transformToWDLCollectionType(final Class<?> argumentCollectionClass) {
        return WDLTransforms.transformToWDLCollectionType(argumentCollectionClass);
    }

    /**
     * Add any custom freemarker bindings discovered via custom javadoc tags. Subclasses can override this to
     * provide additional custom bindings.
     *
     * @param currentWorkUnit the work unit for the feature being documented
     */
    @Override
    protected void addCustomBindings(final DocWorkUnit currentWorkUnit) {
        super.addCustomBindings(currentWorkUnit);

        final RuntimeProperties rtProperties = currentWorkUnit.getClazz().getAnnotation(RuntimeProperties.class);
        if (rtProperties != null) {
            final Map<String, String> runtimePropertiesMap = new HashMap<>();
            runtimePropertiesMap.put(RUNTIME_PROPERTY_MEMORY, rtProperties.memoryRequirements());
            runtimePropertiesMap.put(RUNTIME_PROPERTY_DISKS, rtProperties.diskRequirements());
            currentWorkUnit.setProperty(RUNTIME_PROPERTIES, runtimePropertiesMap);
        }
    }

    @Override
    protected void addExtraDocsBindings(final DocWorkUnit currentWorkUnit) {
        // skip extra docs since they don't affect WDL
    }

}
