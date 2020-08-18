package org.broadinstitute.barclay.help;

import org.apache.commons.lang3.tuple.Pair;

import org.broadinstitute.barclay.argparser.*;
import org.broadinstitute.barclay.argparser.WorkflowProperties;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The WDL work unit handler. Its main task is to convert the types for all arguments for a given work
 * unit (tool) from Java types to WDL-compatible types by updating the freemarker map with the transformed types.
 */
public class WDLWorkUnitHandler extends DefaultDocWorkUnitHandler {
    private static final String GATK_FREEMARKER_TEMPLATE_NAME = "wdlToolTemplate.wdl.ftl";

    /**
     * the name used in the freemarker template as an argument placeholder for positional args; this constant
     * must be kept in sync with the corresponding one used in the template
     */
    public static final String POSITIONAL_ARGS = CommandLineArgumentParser.LONG_OPTION_PREFIX + "positionalArgs";

    // keep track of tool outputs (Map<argName, argType>)
    protected final Map<String, String> runtimeOutputs = new LinkedHashMap<>();

    // requiredOutputs is a subset of runtimeOutputs; the data is somewhat redundant with runtimeOutputs but
    // simplifies access from within the template
    protected final Map<String, String> requiredOutputs = new LinkedHashMap<>();

    // keep track of required companion files (Map<argName, List<Map<companionName, attributes>>>) for
    // arguments for this work unit
    protected final Map<String, List<Map<String, String>>> requiredCompanionFiles = new HashMap<>();

    // keep track of optional companion files (Map<argName, List<Map<companionName, attributes>>>) for
    // arguments for this work unit
    protected final Map<String, List<Map<String, String>>> optionalCompanionFiles = new HashMap<>();

    // Set of inputs that should appear in the param_meta section with localization optional set to true
    protected final Set<String> localizationOptional = new LinkedHashSet<>();

    /**
     * Create the WDL work unit handler for a single work unit.
     * @param doclet the controlling doclet for this work unit
     */
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

        // add the properties required by the WDL template for workflow outputs, required outputs and companions
        // and localization optional
        currentWorkUnit.getRootMap().put(TemplateProperties.WDL_RUNTIME_OUTPUTS, runtimeOutputs);
        currentWorkUnit.getRootMap().put(TemplateProperties.WDL_REQUIRED_OUTPUTS, requiredOutputs);
        currentWorkUnit.getRootMap().put(TemplateProperties.WDL_REQUIRED_COMPANIONS, requiredCompanionFiles);
        currentWorkUnit.getRootMap().put(TemplateProperties.WDL_OPTIONAL_COMPANIONS, optionalCompanionFiles);
        currentWorkUnit.getRootMap().put(TemplateProperties.WDL_LOCALIZATION_OPTIONAL, localizationOptional);
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
        // suppress special args such as --help and --version from showing up in the WDL
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

        // Store the actual (unmodified) arg name that the app will recognize, for use in the task command block.
        // Now generate a WDL-friendly name if necessary (if "input" and "output" are reserved words in WDL and
        // can't be used for arg names; also WDL doesn't accept embedded "-" for variable names, so use a non-kebab
        // name with an underscore) for use as the argument name in the rest of the WDL source.
        final String actualArgName = (String) argBindings.get(TemplateProperties.ARGUMENT_NAME);
        argBindings.put(TemplateProperties.WDL_ARGUMENT_ACTUAL_NAME, actualArgName);
        String wdlName = CommandLineArgumentParser.LONG_OPTION_PREFIX + transformJavaNameToWDLName(actualArgName.substring(2));
        argBindings.put(TemplateProperties.ARGUMENT_NAME, wdlName);

        propagateArgument(wdlName, argDef, argBindings);

        return argCategory;
    }

    @Override
    protected void processPositionalArguments(
            final CommandLineArgumentParser clp,
            final Map<String, List<Map<String, Object>>> argBindings) {
        super.processPositionalArguments(clp, argBindings);
        final PositionalArgumentDefinition argDef = clp.getPositionalArgumentDefinition();
        if (argDef != null) {
            final Map<String, Object> positionalArgBindings = argBindings.get(TemplateProperties.ARGUMENTS_POSITIONAL).get(0);
            propagateArgument(POSITIONAL_ARGS, argDef, positionalArgBindings);
        }
    }

    @SuppressWarnings("unchecked")
    protected void propagateArgument(
            final String wdlArgName,
            final ArgumentDefinition argDef,
            final Map<String, Object> argBindings) {
        final WorkflowInput workFlowInput = argDef.getUnderlyingField().getAnnotation(WorkflowInput.class);
        final WorkflowOutput workFlowOutput = argDef.getUnderlyingField().getAnnotation(WorkflowOutput.class);

        // replace the java type of the argument with the appropriate wdl type, and set the WDL input type
        final String preProcessedType = (String) argBindings.get(TemplateProperties.ARGUMENT_TYPE);
        final String wdlType = getWDLTypeForArgument(argDef, null, preProcessedType);
        final String wdlInputType = getWDLTypeForArgument(argDef, workFlowOutput, preProcessedType);

        argBindings.put(TemplateProperties.ARGUMENT_TYPE, wdlType);
        argBindings.put(TemplateProperties.WDL_ARGUMENT_INPUT_TYPE, wdlInputType);
        argBindings.put(TemplateProperties.ARGUMENT_DEFAULT_VALUE, defaultValueAsJSON(
                wdlType,
                (String) argBindings.get(TemplateProperties.ARGUMENT_DEFAULT_VALUE)));

        // finally, keep track of the outputs and companions
        final boolean argIsRequired = wdlArgName.equals(POSITIONAL_ARGS) || ((argBindings.get(TemplateProperties.ARGUMENT_REQUIRED)).equals("yes"));
        propagateWorkflowAttributes(
                workFlowInput,
                workFlowOutput,
                wdlArgName,
                wdlType,
                !argIsRequired);
    }

    /**
     * Update the list of workflow output resources, and update companion files.
     *
     * @param workflowInput the {@link WorkflowInput} to use when updating runtime outputs, may be null
     * @param workflowOutput the {@link WorkflowOutput} to use when updating runtime outputs, may be null
     * @param targetArgWDLName the wdlname for the target argument for this workflow resource
     * @param targetArgWDLType the wdltype for the target argument for this workflow resource
     * @param targetArgIsOptional true if the target argument being considered is optional, otherwise false
     */
    protected void propagateWorkflowAttributes(
            final WorkflowInput workflowInput,
            final WorkflowOutput workflowOutput,
            final String targetArgWDLName,
            final String targetArgWDLType,
            final boolean targetArgIsOptional) {
        final List<Map<String, String>> requiredCompanions = new ArrayList<>();
        final List<Map<String, String>> optionalCompanions = new ArrayList<>();

        if (workflowInput != null) {
            if (workflowInput.localizationOptional() == true) {
                localizationOptional.add(targetArgWDLName);
            }
            for (final String requiredInputCompanion : workflowInput.requiredCompanions()) {
                final Map<String, String> requiredInputCompanionMap = createCompanionMapEntry(targetArgWDLName, requiredInputCompanion);
                if (targetArgIsOptional) {
                    // if the target is optional, then the companions also have to be optional, even when they're
                    // annotated as required
                    optionalCompanions.add(requiredInputCompanionMap);
                } else {
                    requiredCompanions.add(requiredInputCompanionMap);
                }
                if (workflowInput.localizationOptional() == true) {
                    localizationOptional.add(requiredInputCompanionMap.get(TemplateProperties.ARGUMENT_NAME));
                }
            }

            for (final String optionalInputCompanion : workflowInput.optionalCompanions()) {
                final Map<String, String> companionMap = createCompanionMapEntry(targetArgWDLName, optionalInputCompanion);
                optionalCompanions.add(companionMap);
                if (workflowInput.localizationOptional() == true) {
                    localizationOptional.add(companionMap.get(TemplateProperties.ARGUMENT_NAME));
                }
            }
        }

        if (workflowOutput != null) {
            // add the target argument to the list of workflow outputs
            runtimeOutputs.put(targetArgWDLName, targetArgWDLType);
            if (!targetArgIsOptional) {
                requiredOutputs.put(targetArgWDLName, targetArgWDLType);
            }

            for (final String requiredOutputCompanion : workflowOutput.requiredCompanions()) {
                final Map<String, String> companionMap = createCompanionMapEntry(targetArgWDLName, requiredOutputCompanion);
                runtimeOutputs.put(companionMap.get(TemplateProperties.ARGUMENT_NAME), targetArgWDLType);
                if (targetArgIsOptional) {
                    // if the target is optional, then the companions also have to be optional, even when they're
                    // annotated as required
                    optionalCompanions.add(companionMap);
                } else {
                    requiredCompanions.add(companionMap);
                    requiredOutputs.put(companionMap.get(TemplateProperties.ARGUMENT_NAME), targetArgWDLType);
                }
            }
            for (final String optionalOutputCompanion : workflowOutput.optionalCompanions()) {
                final Map<String, String> companionMap = createCompanionMapEntry(targetArgWDLName, optionalOutputCompanion);
                runtimeOutputs.put(companionMap.get(TemplateProperties.ARGUMENT_NAME), targetArgWDLType);
                optionalCompanions.add(companionMap);
            }
        }
        requiredCompanionFiles.put(targetArgWDLName, requiredCompanions);
        optionalCompanionFiles.put(targetArgWDLName, optionalCompanions);
    }

    protected Map<String, String> createCompanionMapEntry(final String targetArgName, final String companionName) {
        final Map<String, String> companionMap = new HashMap<>();
        final String companionArgOption = CommandLineArgumentParser.LONG_OPTION_PREFIX + companionName;
        companionMap.put(TemplateProperties.ARGUMENT_NAME, companionArgOption);
        companionMap.put(TemplateProperties.ARGUMENT_SUMMARY,
                String.format("Companion resource for %s", targetArgName.substring(2)));
        return companionMap;
    }

    /**
     * Return a String that represents the WDL type for this arg, which is a variant of the  user-friendly doc
     * type chosen by the doc system. Interrogates the structured NamedArgumentDefinition type to transform and
     * determine the resulting WDL type.
     *
     * @param argDef the Barclay NamedArgumentDefinition for this arg
     * @param workflowOutput the WorkflowOutput for this argDef, may be null
     * @param argDocType the display type as chosen by the Barclay doc system for this arg. this is what
     * @return the WDL type to be used for this argument
     */
    protected String getWDLTypeForArgument(
            final ArgumentDefinition argDef,
            final WorkflowOutput workflowOutput,
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
                        wdlType = convertJavaTypeToWDLType(workflowOutput, nestedTypeClass, wdlType, argField.getDeclaringClass().toString());
                    } else {
                        nestedTypeClass = Class.forName(genericTypes[0].getTypeName());
                        wdlType = convertJavaTypeToWDLType(workflowOutput, nestedTypeClass, wdlType, argField.getDeclaringClass().toString());
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

        return convertJavaTypeToWDLType(workflowOutput, argumentClass, wdlType, argField.getDeclaringClass().toString());
    }

    /**
     * Given a Java class representing the underlying field  type of an argument, and a human readable doc type,
     * convert the docType to a WDL type.
     *
     * @param workflowOutput the WorkflowOutput associated with the instance of argumentClass, may be null
     * @param argumentClass the Class for the underlying field of the argument being converted
     * @param docType a string representing the human readable type assigned by the Barclay doc system
     * @param sourceContext a String describing the context for this argument, used for error reporting
     * @return the docType string transformed to the corresponding WDL type
     */
    protected String convertJavaTypeToWDLType(
            final WorkflowOutput workflowOutput,
            final Class<?> argumentClass,
            final String docType, final String sourceContext) {
        final String convertedWDLType;
        final Pair<String, String> typeConversionPair = transformToWDLType(argumentClass);

        // If this type is for an arg that is a workflow resource that is a workflow output, and its type
        // is file, we need to use String as the input type for this arg to prevent the workflow
        // manager from attempting to localize the (non-existent) output file when localizing inputs
        if (typeConversionPair != null) {
            convertedWDLType = docType.replace(
                    typeConversionPair.getKey(),
                    transformWorkflowOutputTypeToInputType(workflowOutput, typeConversionPair.getValue()));
        } else if (argumentClass.isEnum()) {
             convertedWDLType = docType.replace(
                     argumentClass.getSimpleName(),
                     "String");
        } else {
            throw new RuntimeException(
                    String.format(
                            "Don't know how to convert Java type %s in %s to a corresponding WDL type. " +
                                    "The WDL generator type converter code must be updated to support this Java type.",
                            argumentClass,
                            sourceContext));
        }
        return convertedWDLType;
    }

    /**
     * If this type is for an arg that is a workflow resource that is a workflow output, and its type is File,
     * we need to use a different type (String) as the input type for this arg to prevent the workflow manager
     * from attempting to localize the (non-existent) output file when localizing inputs.
     *
     * @param workflowOutput WorkflowOutput for this type instance, if any (may be null)
     * @param convertedWDLType the wdl type for this type instance
     * @return the type to use for the workflow/task input for this argument
     */
    protected String transformWorkflowOutputTypeToInputType(
            final WorkflowOutput workflowOutput,
            final String convertedWDLType) {
        return workflowOutput != null && convertedWDLType.equals("File") ?
                "String" :
                convertedWDLType;
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
     * Return the default value suitably formatted as a JSON value. This primarily involves quoting strings and enum
     * values, including arrays thereof.
     *
     * @param wdlType a wdl type
     * @param defaultWDLValue the default wdl value for this type
     * @return the default value formatted as a JSON value
     */
    protected String defaultValueAsJSON(
            final String wdlType,
            final String defaultWDLValue) {

        if (defaultWDLValue.equals("null") || defaultWDLValue.equals("\"\"")) {
            return defaultWDLValue;
        } else if (defaultWDLValue.equals("[]")) {
            return "null";
        } else if (defaultWDLValue.startsWith("[") && wdlType.equals("Array[String]")) {
            // the array is already populated with a value (since we didn't execute the "[]" branch above),
            // so quote the individual values
            return quoteWDLArrayValues(defaultWDLValue);
        } else if (wdlType.equals("String")) {
            return "\"" + defaultWDLValue + "\"";
        } else if (wdlType.equals("Float")) {
            if (defaultWDLValue.equalsIgnoreCase("Infinity") || defaultWDLValue.equalsIgnoreCase("Nan")) {
                // JSON does not recognize "Infinity" or "Nan" as valid float values (!), so we
                // need to treat them as String values
                return "\"" + defaultWDLValue + "\"";
            }
        }
        return defaultWDLValue;
    }

    /**
     * Parse the wdlArrayString and replace the elements with quoted elements
     * @param wdlArray
     * @return a wdlArrayString with each element quoted
     */
    protected String quoteWDLArrayValues(final String wdlArray) {
        final String wdlValues = wdlArray.substring(1, wdlArray.length() - 1);
        final String[] wdlValueArray = wdlValues.split(",");
        return String.format("[%s]",
                Arrays.stream(wdlValueArray).map(s -> String.format("\"%s\"", s.trim())).collect(Collectors.joining(",")));
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

        final WorkflowProperties rtProperties = currentWorkUnit.getClazz().getAnnotation(WorkflowProperties.class);
        if (rtProperties != null) {
            final Map<String, String> workflowPropertiesMap = new HashMap<>();
            workflowPropertiesMap.put(TemplateProperties.WDL_WORKFLOW_MEMORY, rtProperties.memory());
            workflowPropertiesMap.put(TemplateProperties.WDL_WORKFLOW_DISKS, rtProperties.disks());
            workflowPropertiesMap.put(TemplateProperties.WDL_WORKFLOW_CPU, Integer.toString(rtProperties.cpu()));
            workflowPropertiesMap.put(TemplateProperties.WDL_WORKFLOW_PREEMPTIBLE, Integer.toString(rtProperties.preEmptible()));
            workflowPropertiesMap.put(TemplateProperties.WDL_WORKFLOW_BOOT_DISK_SIZE_GB, Integer.toString(rtProperties.bootDiskSizeGb()));
            currentWorkUnit.setProperty(TemplateProperties.WDL_WORKFLOW_PROPERTIES, workflowPropertiesMap);
        }
    }

    @Override
    protected void addExtraDocsBindings(final DocWorkUnit currentWorkUnit) {
        // skip extra docs since they don't affect WDL
    }

}
