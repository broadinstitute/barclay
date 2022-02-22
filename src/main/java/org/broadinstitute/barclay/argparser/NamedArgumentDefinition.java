package org.broadinstitute.barclay.argparser;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.utils.Utils;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for named argument definitions.
 */
public class NamedArgumentDefinition extends ArgumentDefinition {
    private static final Logger logger = LogManager.getLogger();

    private final Argument argumentAnnotation;
    private final CommandLinePluginDescriptor<?> descriptorForControllingPlugin;

    private final boolean isOptional;
    private final Set<String> mutuallyExclusiveArgs;
    private final String defaultValueAsString;
    private boolean hasBeenSet = false;

    public static final String NULL_ARGUMENT_STRING = "null";

    /**
     * Comparator for sorting ArgumentDefinitions in alphabetical order by longName
     */
    public static Comparator<NamedArgumentDefinition> sortByLongName =
            (argDef1, argDef2) -> String.CASE_INSENSITIVE_ORDER.compare(argDef1.getLongName(), argDef2.getLongName());

    /**
     * @param argumentAnnotation The {@code Argument} annotation object for this argument. cannot be null.
     * @param containingObject The containing parent {@code Object} for this argument. cannot be null.
     * @param underlyingField The {@code Field} object for this argument. cannot be null.
     * @param descriptorForControllingPlugin The controlling {@code CommandLinePluginDescriptor} object for this
     *                                      argument, if any, if this argument is contained by a plugin that is
     *                                      controlled by the descriptor. NOTE: arguments in the
     *                                      {@code CommandLinePluginDescriptor} itself are not contained in a plugin,
     *                                      and will not have a {@code descriptorForControllingPlugin}. can be null.
     */
    public NamedArgumentDefinition(
            final Argument argumentAnnotation,
            final Object containingObject,
            final Field underlyingField,
            final CommandLinePluginDescriptor<?> descriptorForControllingPlugin)
    {
        super(containingObject, underlyingField);

        Utils.nonNull(argumentAnnotation);
        Utils.nonNull(containingObject);
        Utils.nonNull(underlyingField);

        this.argumentAnnotation = argumentAnnotation;
        this.descriptorForControllingPlugin = descriptorForControllingPlugin;
        this.mutuallyExclusiveArgs = new LinkedHashSet<>(Arrays.asList(argumentAnnotation.mutex()));

        this.defaultValueAsString = convertDefaultValueToString();
        this.isOptional = argumentAnnotation.optional() || !this.defaultValueAsString.equals(NULL_ARGUMENT_STRING);

        if (!this.isOptional) {
            if (this.isHidden()) { // required arguments cannot be hidden
                throw new CommandLineException.CommandLineParserInternalException(
                        String.format("A required argument cannot be annotated with @Hidden: %s", getLongName()));
            } else if (this.isAdvanced()) { // required arguments cannot be advanced
                throw new CommandLineException.CommandLineParserInternalException(
                        String.format("A required argument cannot be annotated with @Advanced: %s", getLongName()));
            }
        }
        if (isCollection()) {
            intializeCollection("@Argument");
        } else if (argumentAnnotation.suppressFileExpansion()) {
            throw new CommandLineException.CommandLineParserInternalException(
                    "suppressFileExpansion can only be used for collection arguments");
        }
        validateBoundsDefinitions();
    }

    /**
     * @return the short name for this argument. can be empty.
     */
    public String getShortName() { return argumentAnnotation.shortName(); }

    /**
     * @return the full name for this argument. can be empty.
     */
    public String getFullName() { return argumentAnnotation.fullName(); }

    /**
     * @return the long name for this argument. cannot be null. Returns the fullName of the argument if any,
     * otherwise the name of the underlying field.
     */
    public String getLongName() { return !getFullName().isEmpty() ? getFullName() : getUnderlyingField().getName(); }

    /**
     * the doc string for this argument, if any.
     * @return doc string. can be empty.
     */
    public String getDocString() { return argumentAnnotation.doc(); }

    /**
     * return true if this argument is defined as common argument.
     * @return true if this argument is defined as common argument, otherwise false.
     */
    public boolean isCommon() { return argumentAnnotation.common(); }

    /**
     * Return true if this argument is optional. An argument is defined as optional if either the
     * {@code optional} annotation property is set or it has a default initial value that is non-null
     * (for scalar args) or a non-null, non-empty collection (for collection args).
     *
     * NOTE: arguments with a {@code Field} that has a primitive type always have a non-null initial value,
     * and will never return false.
     *
     * @return true if this argument is defined as an optional argument, otherwise false.
     */
    public boolean isOptional() { return isOptional; }

    /**
     * return true if this argument has the {@code @Hidden} annotation.
     * @return true if this argument is hidden, otherwise false.
     */
    public boolean isHidden() { return getUnderlyingField().getAnnotation(Hidden.class) != null; }

    /**
     * return true if this argument has the {@code @Advanced} annotation.
     * @return true if this argument is advanced, otherwise false.
     */
    public boolean isAdvanced() { return getUnderlyingField().getAnnotation(Advanced.class) != null; }

    /**
     * return true if this argument has the {@code @Deprecated} annotation.
     * @return true if this argument is advanced, otherwise false.
     */
    public boolean isDeprecated() { return getUnderlyingField().isAnnotationPresent(Deprecated.class); }

    /**
     * return true if this argument is a flag (boolean valued) argument
     * @return true if this argument is boolean valued
     */
    public boolean isFlag() {
        return getUnderlyingFieldClass().equals(boolean.class) ||
                getUnderlyingFieldClass().equals(Boolean.class);
    }

    /**
     * Return the minimum number of elements allowed for this argument.
     * @return the minimum number of elements allowed for this argument
     */
    public int getMinElements() { return argumentAnnotation.minElements(); }

    /**
     * Return the maximum number of elements allowed for this argument.
     * @return the maximum number of elements allowed for this argument
     */
    public int getMaxElements() { return argumentAnnotation.maxElements(); }

    /**
     * Returns {@code true} if the argument has a non-default bounded (minimum/maximum value) range, {@code false} otherwise.
     */
    public boolean hasBoundedRange() {
        return getMinValue() != Double.NEGATIVE_INFINITY || getMaxValue() != Double.POSITIVE_INFINITY;
    }

    /**
     * Returns {@code true} if the argument has a non-default recommended (minimum/maximum value) range, {@code false} otherwise.
     */
    public boolean hasRecommendedRange() {
        return getMaxRecommendedValue() != Double.POSITIVE_INFINITY || getMinRecommendedValue() != Double.NEGATIVE_INFINITY;
    }

    /**
     * Return the minimum value allowed for this argument.
     * @return the minimum value allowed for this argument
     */
    public Double getMinValue() { return argumentAnnotation.minValue(); }

    /**
     * Return the maximum value allowed for this argument.
     * @return the maximum value allowed for this argument
     */
    public Double getMaxValue() { return argumentAnnotation.maxValue(); }

    /**
     * Return the minimum recommended value allowed for this argument.
     * @return the minimum recommendedvalue allowed for this argument
     */
    public Double getMinRecommendedValue() { return argumentAnnotation.minRecommendedValue(); }

    /**
     * Return the maximum recommended value allowed for this argument.
     * @return the maximum recommended value allowed for this argument
     */
    public Double getMaxRecommendedValue() { return argumentAnnotation.maxRecommendedValue(); }

    /**
     * Return a String representing the initial/default value for this argument. Empty, null, or uninitialized
     * reference fields will return the string {@link #NULL_ARGUMENT_STRING}
     * @return
     */
    public String getDefaultValueAsString() { return defaultValueAsString; }

    /**
     * Return the set of argument names with which this argument is mutually exclusive.
     * @return the set of argument names with which this argument is mutually exclusive. May be empty.
     */
    public Set<String> getMutexTargetList() { return mutuallyExclusiveArgs; }

    /**
     * Add the anme of an argument to the list of arguments with which this argument is mutually exclusive.
     * @param target name if the mutually exclusive argument
     */
    public void addMutexTarget(final String target) { mutuallyExclusiveArgs.add(target); }

    /**
     * Return the {@code CommandLinePluginDescriptor} that controls the plugin that contains this argument, if any.
     * @return the {@code CommandLinePluginDescriptor} that controls this argument. may be null.
     */
    public CommandLinePluginDescriptor<?> getDescriptorForControllingPlugin() { return descriptorForControllingPlugin; }

    /**
     * Determine if one or more values were provided on the command line for this argument.
     * @return true if this argument was set by the user
     */
    public boolean getHasBeenSet() { return hasBeenSet; }

    @Override
    public String getCommandLineDisplayString() {
        final Object value = getArgumentValue();
        if (isCollection()) {
            // if an expansion file was provided on the command line, use the original values provided by
            // the user on the command line so the name of the file is seen instead of the file contents,
            // since that could be huge
            final Collection<?> collect = getOriginalCommandLineValues() == null ?
                    (Collection<?>) value :
                    getOriginalCommandLineValues();
            return collect.stream()
                    .map(this::getNameValuePairForDisplay)
                    .collect(Collectors.joining(" "));
        } else {
            return getNameValuePairForDisplay(value);
        }
    }

    /**
     * Get the list of short and long aliases for this argument.
     * @return The list of possible aliases for this argument. Will not be null or empty, and contain only
     * 1 or 2 values. the names are drawn from the fullName, shortName, and {@code Field} name for this argument.
     */
    public List<String> getArgumentAliases() {
        final List<String> aliases = new ArrayList<>();
        if (!getShortName().isEmpty()) {
            aliases.add(getShortName());
        }
        if (!getFullName().isEmpty()) {
            aliases.add(getFullName());
        } else {
            aliases.add(getUnderlyingField().getName());
        }
        return aliases;
    }

    /**
     * Return a String representing the possible aliases for this argument suitable for display.
     * @return string containing all aliases for this argument.
     */
    public String getArgumentAliasDisplayString() {
        return getArgumentAliases().stream().collect(Collectors.joining("/"));
    }

    /**
     * Determine if this argument definition is controlled by a plugin (and thus subject to
     * descriptor dependency validation).
     *
     * @return true if this argument is controlled by a {@link CommandLinePluginDescriptor}
     */
    public boolean isControlledByPlugin() {
        return descriptorForControllingPlugin != null;
    }

    @Override
    public void setArgumentValues(
            final CommandLineArgumentParser commandLineArgumentParser,
            final PrintStream messageStream,
            final List<String> preprocessedValues) // Note these might be tag surrogates
    {
        if (isCollection()) {
            setCollectionValues(commandLineArgumentParser, messageStream, preprocessedValues);
        } else {
            setScalarValue(commandLineArgumentParser, messageStream, preprocessedValues);
        }
        hasBeenSet = true;
    }

    // Propagate any argument values provided by the user to the argument. For values that are a tag
    // surrogate, each instance added to the collection must be populated with any tags and attributes.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setCollectionValues(
            final CommandLineArgumentParser commandLineArgumentParser,
            final PrintStream messageStream,
            final List<String> preprocessedValues) // Note that some of these might be tag surrogates
    {
        final Collection c = (Collection) getArgumentValue();
        if (!commandLineArgumentParser.getAppendToCollectionsParserOption()) {
            // if this is a collection then we only want to clear it once at the beginning, before we
            // process any of the values, unless we're in APPEND_TO_COLLECTIONS mode
            c.clear();
        }

        for (int i = 0; i < preprocessedValues.size(); i++) {
            final String stringValue = preprocessedValues.get(i);
            if (stringValue.equals(NULL_ARGUMENT_STRING)) {
                if (i != 0) {
                    // if a "null" is included that isn't the first value for this option, honor it but warn, since it
                    // will clobber any previously set values, and may indicate an unintentional error on the user's part
                    logger.warn(String.format(
                            "A \"null\" value was detected for an option after values for that option were already set. " +
                                    "Clobbering previously set values for this option: %s.", getArgumentAliasDisplayString()));
                }
                if (!isOptional()) {
                    throw new CommandLineException(
                            String.format("Non \"null\" value must be provided for '%s'", getArgumentAliasDisplayString()));
                }
                c.clear();
            } else {
                // if a collection argument was presented as a tagged argument on the command line, and an expansion
                // file was provided as the value, propagate the tags to each value from the expansion file
                final Pair<String, String> normalizedSurrogatePair = getNormalizedTagValuePair(commandLineArgumentParser, stringValue);
                final List<String> expandedValues = argumentAnnotation.suppressFileExpansion() ?
                        Collections.singletonList(normalizedSurrogatePair.getRight()) :
                        commandLineArgumentParser.expandFromExpansionFile(
                                this,
                                messageStream,
                                normalizedSurrogatePair.getRight(),
                                preprocessedValues);

                for (final String expandedValue : expandedValues) {
                    final Object actualValue = getValuePopulatedWithTags(
                            normalizedSurrogatePair.getLeft(),
                            expandedValue);
                    checkArgumentRange(actualValue);
                    c.add(actualValue);
                }
            }
        }
    }

    // Propagate any argument values provided by the user to the argument. For values that are a tag
    // surrogate, the constructed value must be populated with any tags and attributes.
    // Note that even though the target is a scalar, the user may have tried to provided more than
    // one value, in which case we throw.
    private void setScalarValue(
            final CommandLineArgumentParser commandLineArgumentParser,
            final PrintStream messageStream,
            final List<String> originalValues) // Note that these might be tag surrogates
    {
        if (getHasBeenSet() || originalValues.size() > 1) {
            throw new CommandLineException.BadArgumentValue(
                    String.format("Argument '%s' cannot be specified more than once.", getArgumentAliasDisplayString()));
        }

        if (isFlag() && originalValues.isEmpty()){
            setArgumentValue(true);
        } else {
            final String stringValue = originalValues.get(0);
            Object value = null;
            if (stringValue.equals(NULL_ARGUMENT_STRING)) {
                if (getUnderlyingField().getType().isPrimitive()) {
                    throw new CommandLineException.BadArgumentValue(
                            String.format("Argument '%s' is not a nullable argument type.", getArgumentAliasDisplayString()));
                }
            } else {
                final Pair<String, String> normalizedSurrogatePair = getNormalizedTagValuePair(commandLineArgumentParser, stringValue);
                value = getValuePopulatedWithTags(normalizedSurrogatePair.getLeft(), normalizedSurrogatePair.getRight());
            }
            checkArgumentRange(value);
            setArgumentValue(value);
        }
    }

    // Get a tag/value pair for an argument value even if there its not tagged (left side will be null)
    private Pair<String, String> getNormalizedTagValuePair(
            final CommandLineArgumentParser commandLineArgumentParser,
            final String preprocessedValue) {
        final Pair<String, String> taggedSurrogatePair =
                commandLineArgumentParser.getTaggedArgumentParser().getTaggedOptionForSurrogate(preprocessedValue);
        return taggedSurrogatePair == null ?
                Pair.of(null, preprocessedValue) :
                taggedSurrogatePair;
    }

    @Override
    public void validateValues(final CommandLineArgumentParser commandLineArgumentParser) {
        final String fullName = getLongName();

        // get a list of any mutually exclusive arguments that were provided
        final List<String> providedMutexArguments = new ArrayList<>();
        for (final String mutexWith : getMutexTargetList()) {
            final NamedArgumentDefinition mutexWithDef =
                    commandLineArgumentParser.getNamedArgumentDefinitionByAlias(mutexWith);
            if (mutexWithDef.getHasBeenSet()) {
                providedMutexArguments.add(mutexWithDef.getLongName());
            }
        }
        if (getHasBeenSet() && !providedMutexArguments.isEmpty()) {
            throw new CommandLineException(
                    String.format("Argument '%s' cannot be used in conjunction with argument(s) %s",
                            fullName,
                            providedMutexArguments.stream().collect(Collectors.joining(" "))));
        }
        if (!isOptional()) {
            if (isCollection()) {
                final Collection<?> c = (Collection<?>) getArgumentValue();
                if (c.isEmpty() && providedMutexArguments.isEmpty()) {
                    throw new CommandLineException.MissingArgument(fullName, getArgRequiredErrorMessage());
                }
            } else if (!getHasBeenSet() && providedMutexArguments.isEmpty()) {
                throw new CommandLineException.MissingArgument(fullName, getArgRequiredErrorMessage());
            }
        }
    }

    /**
     * Return a string with the usage statement for this argument.
     * @param allActualArguments {code Map} of all namedArgumentDefinitions for the containing object
     * @param pluginDescriptors Collection of {@code CommandLinePluginDescriptor} objects for the containing object
     * @param argumentColumnWidth width reserved for argument name column display
     * @param descriptionColumnWidth width reserved for argument description column display
     * @return the usage string for this argument
     */
    public String getArgumentUsage(
            final Map<String, NamedArgumentDefinition> allActualArguments,
            final Collection<CommandLinePluginDescriptor<?>> pluginDescriptors,
            final int argumentColumnWidth,
            final int descriptionColumnWidth) {

        final StringBuilder sb = new StringBuilder();
        sb.append("--").append(getLongName());
        if (!getShortName().isEmpty() && !getShortName().equals(getLongName())) {
            sb.append(",-").append(getShortName());
        }

        sb.append(String.format(" <%s>", getUnderlyingFieldClass().getSimpleName()));

        int labelLength = sb.toString().length();
        int numSpaces = argumentColumnWidth - labelLength;
        if (labelLength > argumentColumnWidth) {
            sb.append("\n");
            numSpaces = argumentColumnWidth;
        }
        printSpaces(sb, numSpaces);

        final String description = getArgumentDescription(allActualArguments, pluginDescriptors);
        final String wrappedDescription = Utils.wrapParagraph(description, descriptionColumnWidth);
        final String[] descriptionLines = wrappedDescription.split("\n");
        for (int i = 0; i < descriptionLines.length; ++i) {
            if (i > 0) {
                printSpaces(sb, argumentColumnWidth);
            }
            sb.append(descriptionLines[i]);
            sb.append("\n");
        }
        sb.append("\n");

        return sb.toString();
    }

    // Get a name value pair for this argument and a value, suitable for display.
    private String getNameValuePairForDisplay(final Object value) {
        if (value != null) {
            if (argumentAnnotation.sensitive()) {
                return String.format("--%s ***********", getLongName());
            } else {
                // if we're displaying a tagged argument, include the tag name and attributes
                if (value instanceof TaggedArgument) {
                    return String.format("--%s %s", TaggedArgumentParser.getDisplayString(getLongName(), (TaggedArgument) value), value);
                } else {
                    return String.format("--%s %s", getLongName(), value);
                }
            }
        }
        return "";
    }

    // Convert the initial value for this argument to a string representation.
    private String convertDefaultValueToString() {
        final Object initialValue = getArgumentValue();
        if (initialValue != null) {
            if (isCollection() && ((Collection<?>) initialValue).isEmpty()) {
                // treat empty collections the same as uninitialized non-collection types
                return NULL_ARGUMENT_STRING;
            } else {
                // this is an initialized primitive type or a non-empty collection
                return initialValue.toString();
            }
        } else {
            return NULL_ARGUMENT_STRING;
        }
    }

    // check if the value is infinity or a mathematical integer
    private static boolean isInfinityOrMathematicalInteger(final double value) {
        return Double.isInfinite(value) || value == Math.rint(value);
    }

    // Validate that any range bounds defined for this argument are coherent with the underlying type
    // and are self-consistent.
    private void validateBoundsDefinitions() {
        // bounds should be only set for numeric arguments and if the type is integer it should
        // be set to an integer
        if (!Number.class.isAssignableFrom(getUnderlyingFieldClass())) {
            if (hasBoundedRange() || hasRecommendedRange()) {
                throw new CommandLineException.CommandLineParserInternalException(
                        String.format(
                                "Min/max value ranges can only be set for numeric arguments. " +
                                "Argument --%s has a minimum or maximum value but has a non-numeric type.",
                                getLongName()));
            }
        }
        if (Integer.class.isAssignableFrom(getUnderlyingFieldClass())) {
            if (!isInfinityOrMathematicalInteger(getMaxValue())
                    || !isInfinityOrMathematicalInteger(getMinValue())
                    || !isInfinityOrMathematicalInteger(getMaxRecommendedValue())
                    || !isInfinityOrMathematicalInteger(getMinRecommendedValue())) {
                throw new CommandLineException.CommandLineParserInternalException(
                        String.format(
                                "Integer argument --%s has a minimum or maximum attribute with a non-integral value.",
                                getLongName()));
            }
        }
    }

    // If the string value is tag surrogate key, retrieve the underlying tags and return an object that has been
    // populated with the actual value and tags and attributes provided by the user for that argument.
    private Object getValuePopulatedWithTags(final String originalTag, final String stringValue)
    {
        // See if the value is a surrogate key in the tag parser's map that was placed there during preprocessing,
        // and if so, unpack the values retrieved via the key and use those to populate the field
        final Object value = constructFromString(stringValue, getLongName());

        if (TaggedArgument.class.isAssignableFrom(getUnderlyingFieldClass())) {
            // NOTE: this propagates the tag name/attributes to the field BEFORE the value is set
            TaggedArgument taggedArgument = (TaggedArgument) value;
            TaggedArgumentParser.populateArgumentTags(
                    taggedArgument,
                    getLongName(),
                    originalTag);
        } else if (originalTag != null) {
            // a tag was found for a non-taggable argument
            throw new CommandLineException(
                    String.format("The argument: \"%s/%s\" does not accept tags: \"%s\"",
                            getShortName(),
                            getFullName(),
                            originalTag));
        }
        return value;
    }

    // Return a usage string representing this argument.
    private String getArgumentDescription(
            final Map<String, NamedArgumentDefinition> allActualArguments,
            final Collection<CommandLinePluginDescriptor<?>> pluginDescriptors) {
        final StringBuilder sb = new StringBuilder();
        if (!getDocString().isEmpty()) {
            sb.append(getDocString());
            sb.append("  ");
        }
        if (isCollection()) {
            if (isOptional()) {
                sb.append("This argument may be specified 0 or more times. ");
            } else {
                sb.append("This argument must be specified at least once. ");
            }
        }
        if (isOptional()) {
            sb.append("Default value: ");
            sb.append(getDefaultValueAsString());
            sb.append(". ");
        } else {
            sb.append("Required. ");
        }

        // if this argument definition is a string field claimed by a plugin descriptor (i.e.,
        // it holds the names of plugins specified by the user on the command line, such as read filter names),
        // then we need to delegate to the plugin descriptor to generate the list of allowed values
        if (!usageForPluginDescriptorArgument(sb, pluginDescriptors)) {
            // If the argument wasn't claimed by any descriptor, treat it as a normal argument
            sb.append(getOptionsAsDisplayString());
        }

        if (!getMutexTargetList().isEmpty()) {
            sb.append(" Cannot be used in conjunction with argument(s)");
            for (final String argument : getMutexTargetList()) {
                final NamedArgumentDefinition mutexArgumentDefinition = allActualArguments.get(argument);
                sb.append(" ").append(mutexArgumentDefinition.getUnderlyingField().getName());
                if (!mutexArgumentDefinition.getShortName().isEmpty()) {
                    sb.append(" (").append(mutexArgumentDefinition.getShortName()).append(")");
                }
            }
        }
        return sb.toString();
    }

    private boolean usageForPluginDescriptorArgument(
            final StringBuilder sb,
            final Collection<CommandLinePluginDescriptor<?>> pluginDescriptors)
    {
        if (getUnderlyingFieldClass().equals(String.class)) {
            for (CommandLinePluginDescriptor<?> descriptor : pluginDescriptors) {
                // See if this this argument came from a plugin descriptor; delegate to get the list of allowed values if it is
                final Set<String> allowedValues = descriptor.getAllowedValuesForDescriptorHelp(getLongName());
                if (allowedValues != null) {
                    if (allowedValues.isEmpty()) {
                        sb.append("Any value allowed");
                    } else {
                        sb.append(OPTION_DOC_PREFIX);
                        sb.append(String.join(", ", allowedValues.stream().sorted(String::compareToIgnoreCase).collect(Collectors.toList())));
                        sb.append(OPTION_DOC_SUFFIX);
                    }
                    return true;
                }
                // Do nothing because the argument doesn't belong to this descriptor
            }
        }
        return false;
    }

    // Error message for when mutex args are mutually required (meaning one of them must be specified) but none was
    private String getArgRequiredErrorMessage() {
        return getMutexTargetList().isEmpty() ?
                String.format("Argument '%s' is required", getLongName()) :
                String.format("Argument '%s' is required unless one of {%s} are provided", getLongName(), getMutexTargetList());
    }

    /**
     * Check the provided value against any range constraints specified in the Argument annotation
     * for the corresponding field. Throw an exception if limits are violated.
     *
     * - Only checks numeric types (int, double, etc.)
     */
    private void checkArgumentRange(final Object argumentValue) {
        // Only validate numeric types because we have already ensured at constructor time that only numeric types have bounds
        if (!Number.class.isAssignableFrom(getUnderlyingFieldClass())) {
            return;
        }

        final Double argumentDoubleValue = (argumentValue == null) ? null : ((Number)argumentValue).doubleValue();

        // Check hard limits first, if specified
        if (hasBoundedRange() && isValueOutOfRange(argumentDoubleValue)) {
            throw new CommandLineException.OutOfRangeArgumentValue(getLongName(), getMinValue(), getMaxValue(), argumentValue);
        }

        // Check recommended values
        if (hasRecommendedRange() && isValueOutOfRange(argumentDoubleValue)) {
            final boolean outMinValue = getMinRecommendedValue() != Double.NEGATIVE_INFINITY;
            final boolean outMaxValue = getMaxRecommendedValue() != Double.POSITIVE_INFINITY;
            if (outMinValue && outMaxValue) {
                logger.warn("Argument --{} has value {}, but recommended within range ({},{})",
                        getLongName(), argumentDoubleValue, getMinRecommendedValue(), getMaxRecommendedValue());
            } else if (outMinValue) {
                logger.warn("Argument --{} has value {}, but minimum recommended is {}",
                        getLongName(), argumentDoubleValue, getMinRecommendedValue());
            } else if (outMaxValue) {
                logger.warn("Argument --{} has value {}, but maximum recommended is {}",
                        getLongName(), argumentDoubleValue, getMaxRecommendedValue());
            }
            // if there is no recommended value, do not log anything
        }
    }

    // Set the underlying field to the new value.
    private void setArgumentValue(final Object value) {
        try {
            getUnderlyingField().set(getContainingObject(), value);
        } catch (final IllegalAccessException e) {
            throw new CommandLineException.ShouldNeverReachHereException(
                    String.format(
                            "Couldn't set field value for %s in %s with value %s.",
                            getUnderlyingField().getName(),
                            getContainingObject().toString(),
                            value.toString()),
                    e);
        }
    }

    // null values are always out of range
    private boolean isValueOutOfRange(final Double value) {
        return value == null || getMinValue() != Double.NEGATIVE_INFINITY && value < getMinValue()
                || getMaxValue() != Double.POSITIVE_INFINITY && value > getMaxValue();
    }

    private static void printSpaces(final StringBuilder sb, final int numSpaces) {
        for (int i = 0; i < numSpaces; ++i) {
            sb.append(" ");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NamedArgumentDefinition)) return false;
        if (!super.equals(o)) return false;

        NamedArgumentDefinition that = (NamedArgumentDefinition) o;

        if (isOptional() != that.isOptional()) return false;
        if (!argumentAnnotation.equals(that.argumentAnnotation)) return false;
        if (getDescriptorForControllingPlugin() != null ? !getDescriptorForControllingPlugin().equals(that.getDescriptorForControllingPlugin()) : that.getDescriptorForControllingPlugin() != null)
            return false;
        if (!mutuallyExclusiveArgs.equals(that.mutuallyExclusiveArgs)) return false;
        return getDefaultValueAsString() != null ? getDefaultValueAsString().equals(that.getDefaultValueAsString()) : that.getDefaultValueAsString() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + argumentAnnotation.hashCode();
        result = 31 * result + (getDescriptorForControllingPlugin() != null ? getDescriptorForControllingPlugin().hashCode() : 0);
        result = 31 * result + (isOptional() ? 1 : 0);
        result = 31 * result + mutuallyExclusiveArgs.hashCode();
        result = 31 * result + (getDefaultValueAsString() != null ? getDefaultValueAsString().hashCode() : 0);
        return result;
    }
}