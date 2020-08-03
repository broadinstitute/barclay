package org.broadinstitute.barclay.argparser;

import joptsimple.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.barclay.utils.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Annotation-driven utility for parsing command-line arguments, checking for errors, and producing usage message.
 * <p/>
 * This class supports arguments of the form -KEY VALUE, plus positional arguments.
 * <p/>
 * The caller must supply an object that both defines the command line and has the parsed arguments set into it.
 * For each possible "-KEY VALUE" argument, there must be a public data member annotated with @Argument.  The KEY name is
 * the name of the fullName attribute of @Argument.  An abbreviated name may also be specified with the shortName attribute
 * of @Argument.
 * If the data member is a List<T>, then the argument may be specified multiple times.  The type of the data member,
 * or the type of the List element must either have a ctor T(String), or must be an Enum.  List arguments must
 * be initialized by the caller with some kind of list.  Any other argument that is non-null is assumed to have the given
 * value as a default.  If an argument has no default value, and does not have the optional attribute of @Argument set,
 * is required.  For List arguments, minimum and maximum number of elements may be specified in the @Argument annotation.
 * <p/>
 * A single List data member may be annotated with the @PositionalArguments.  This behaves similarly to a Argument
 * with List data member: the caller must initialize the data member, the type must be constructable from String, and
 * min and max number of elements may be specified.  If no @PositionalArguments annotation appears in the object,
 * then it is an error for the command line to contain positional arguments.
 * <p/>
 */
public final class CommandLineArgumentParser implements CommandLineParser {
    // For formatting argument section of usage message.
    private static final int ARGUMENT_COLUMN_WIDTH = 30;
    private static final int DESCRIPTION_COLUMN_WIDTH = 90;

    private static final String defaultUsagePreamble = "Usage: program [arguments...]\n";
    private static final String defaultUsagePreambleWithPositionalArguments =
            "Usage: program [arguments...] [positional-arguments...]\n";

    protected static final String BETA_PREFIX = "\n\n**BETA FEATURE - WORK IN PROGRESS**\n\n";
    protected static final String EXPERIMENTAL_PREFIX = "\n\n**EXPERIMENTAL FEATURE - USE AT YOUR OWN RISK**\n\n";
    protected static final String ARGUMENT_FILE_COMMENT = "#";

    /**
     * Extensions recognized for collection argument file expansion.
     */
    protected static final String EXPANSION_FILE_EXTENSION_LIST = ".list";
    protected static final String EXPANSION_FILE_EXTENSIONS_ARGS = ".args";
    protected static final Set<String> EXPANSION_FILE_EXTENSIONS = new HashSet<>(
            Arrays.asList(EXPANSION_FILE_EXTENSION_LIST, EXPANSION_FILE_EXTENSIONS_ARGS));

    // Prefixes used by the opt parser for short/long prefixes
    public static final String SHORT_OPTION_PREFIX = "-";
    public static final String LONG_OPTION_PREFIX = "--";

    // This is the object that the caller has provided that contains annotations,
    // and into which the values will be assigned.
    private final Object callerArguments;

    private final Set<CommandLineParserOptions> parserOptions;

    private PositionalArgumentDefinition positionalArgumentDefinition;

    // List of all the data members with @Argument annotation
    private List<NamedArgumentDefinition> namedArgumentDefinitions = new ArrayList<>();

    // Maps long name, and short name, if present, to an argument definition that is
    // also in the namedArgumentDefinitions list.
    private final Map<String, NamedArgumentDefinition> namedArgumentsDefinitionsByAlias = new LinkedHashMap<>();

    // The (optional) CommandLineProgramProperties annotation
    private final CommandLineProgramProperties programProperties;

    // Map from (full class) name of each CommandLinePluginDescriptor requested and
    // found to the actual descriptor instance
    private final Map<String, CommandLinePluginDescriptor<?>> pluginDescriptors = new HashMap<>();

    // Keeps a map of tagged arguments for just-in-time retrieval at field population time
    private final TaggedArgumentParser tagParser = new TaggedArgumentParser();

    private final Set<String> argumentsFilesLoadedAlready = new LinkedHashSet<>();

    /**
     * @param callerArguments The object containing the command line arguments to be populated by
     *                        this command line parser.
     */
    public CommandLineArgumentParser(final Object callerArguments) {
        this(callerArguments, Collections.emptyList(), Collections.emptySet());
    }

    /**
     * @param callerArguments The object containing the command line arguments to be populated by
     *                        this command line parser.
     * @param pluginDescriptors A list of {@link CommandLinePluginDescriptor} objects that
     *                          should be used by this command line parser to extend the list of
     *                          command line arguments with dynamically discovered plugins. If
     *                          null, no descriptors are loaded.
     * @param parserOptions A non-null set of {@link CommandLineParserOptions}.
     */
    public CommandLineArgumentParser(
            final Object callerArguments,
            final List<? extends CommandLinePluginDescriptor<?>> pluginDescriptors,
            final Set<CommandLineParserOptions> parserOptions) {
        Utils.nonNull(callerArguments, "The object with command line arguments cannot be null");
        Utils.nonNull(pluginDescriptors, "The list of pluginDescriptors cannot be null");
        Utils.nonNull(parserOptions, "The set of parser options cannot be null");

        this.callerArguments = callerArguments;
        this.parserOptions = parserOptions;

        final Class<?> callerArgumentsClass = this.callerArguments.getClass();
        if ((callerArgumentsClass.getAnnotation(ExperimentalFeature.class) != null) &&
                (callerArgumentsClass.getAnnotation(BetaFeature.class) != null)) {
            throw new CommandLineException.CommandLineParserInternalException("Features cannot be both Beta and Experimental");
        }

        this.programProperties = callerArgumentsClass.getAnnotation(CommandLineProgramProperties.class);

        createArgumentDefinitions(callerArguments, null);
        createCommandLinePluginArgumentDefinitions(pluginDescriptors);

        validateArgumentDefinitions();
    }

    /**
     * Parse command-line arguments, and store values in callerArguments object passed to ctor.
     * @param messageStream Where to write error messages.
     * @param args          Command line tokens.
     * @return true if command line is valid and the program should run, false if help or version was requested
     * @throws CommandLineException if there is an invalid command line
     */
    @Override
    public boolean parseArguments(final PrintStream messageStream, String[] args) {
        final OptionSet parsedArguments;
        final OptionParser parser = getOptionParser();
        try {
            // Preprocess the arguments before the parser sees them, replacing any tagged options
            // and their values with raw option names and surrogate key values, so that tagged
            // options can be recognized by the parser. The actual values will be retrieved using
            // the key when the fields's values are set.
            parsedArguments = parser.parse(tagParser.preprocessTaggedOptions(args));
        } catch (final OptionException e) {
            throw new CommandLineException(e.getMessage());
        }

        // Check if special short circuiting arguments were set
        if (isSpecialFlagSet(parsedArguments, SpecialArgumentsCollection.HELP_FULLNAME)) {
            messageStream.print(usage(true, isSpecialFlagSet(parsedArguments, SpecialArgumentsCollection.SHOW_HIDDEN_FULLNAME)));
            return false;
        } else if (isSpecialFlagSet(parsedArguments, SpecialArgumentsCollection.VERSION_FULLNAME)) {
            messageStream.println(getVersion());
            return false;
        } else if (parsedArguments.has(SpecialArgumentsCollection.ARGUMENTS_FILE_FULLNAME)) {
            // If a special arguments file was specified, read arguments from it and recursively call parseArguments()
            final List<String> newArgs = expandFromArgumentFile(parsedArguments);
            if (!newArgs.isEmpty()) {
                // If we've expanded any argument files, we need to do another pass on the entire list post-expansion,
                // so clear any tag surrogates created in this pass (they'll be regenerated in the next pass).
                tagParser.resetTagSurrogates();
                newArgs.addAll(Arrays.asList(args));
                return parseArguments(messageStream, newArgs.toArray(new String[newArgs.size()]));
            }
        }
        return propagateParsedValues(parsedArguments, messageStream);
    }

    /**
     * @return list of ArgumentDefinitions for all named arguments seen by the parser
     */
    public List<NamedArgumentDefinition> getNamedArgumentDefinitions() { return namedArgumentDefinitions; }

    /**
     * @return ArgumentDefinitions for a given alias, otherwise null
     */
    public NamedArgumentDefinition getNamedArgumentDefinitionByAlias(final String argumentAlias) {
        return namedArgumentsDefinitionsByAlias.get(argumentAlias);
    }

    /**
     * @return PositionalArgumentDefinition representing any positional argument definition seen by the parser
     */
    public PositionalArgumentDefinition getPositionalArgumentDefinition() { return positionalArgumentDefinition; }

    /**
     * Get the {@link TaggedArgumentParser} used for this parser instance.
     * @return
     */
    public TaggedArgumentParser getTaggedArgumentParser() { return tagParser; }

    @Override
    public String getVersion() {
        return "Version:" + this.callerArguments.getClass().getPackage().getImplementationVersion();
    }

    // Return the plugin instance corresponding to the targetDescriptor class
    @Override
    public <T> T getPluginDescriptor(final Class<T> targetDescriptor) {
        return targetDescriptor.cast(pluginDescriptors.get(targetDescriptor.getName()));
    }

    public boolean getAppendToCollectionsParserOption() { return parserOptions.contains(CommandLineParserOptions.APPEND_TO_COLLECTIONS); }

    /**
     * The commandline used to run this program, including any default args that
     * weren't necessarily specified. This is used for logging and debugging.
     * <p/>
     * NOTE: {@link #parseArguments(PrintStream, String[])} must be called before
     * calling this method.
     *
     * @return The commandline, or null if {@link #parseArguments(PrintStream, String[])}
     * hasn't yet been called, or didn't complete successfully.
     */
    @Override
    public String getCommandLine() {
        final StringBuilder commandLineString = new StringBuilder();
        String tempString;
        commandLineString.append(callerArguments.getClass().getSimpleName());

        // positional args first
        if( positionalArgumentDefinition != null) {
            tempString = positionalArgumentDefinition.getCommandLineDisplayString();
            commandLineString.append(tempString.length() > 0 ? " " + tempString : "");
        }

        // then args that were explicitly set
        tempString = namedArgumentDefinitions.stream()
                .filter(NamedArgumentDefinition::getHasBeenSet)
                .map(NamedArgumentDefinition::getCommandLineDisplayString)
                .collect(Collectors.joining(" "));
        commandLineString.append(tempString.length() > 0 ? " " + tempString : "");

        // finally, args that weren't explicitly set, but have a default value
        tempString = namedArgumentDefinitions.stream()
                        .filter(argumentDefinition -> !argumentDefinition.getHasBeenSet() &&
                                !argumentDefinition.getDefaultValueAsString().equals(NamedArgumentDefinition.NULL_ARGUMENT_STRING))
                        .map(NamedArgumentDefinition::getCommandLineDisplayString)
                        .collect(Collectors.joining(" "));
        commandLineString.append(tempString.length() > 0 ? " " + tempString : "");

        return commandLineString.toString();
    }

    /**
     * A typical command line program will call this to get the beginning of the usage message,
     * and then append a description of the program, like this:
     * commandLineParser.getStandardUsagePreamble(getClass()) + "Frobnicates the freebozzle."
     */
    @Override
    public String getStandardUsagePreamble(final Class<?> mainClass) {
        final String preamble = "USAGE: " + mainClass.getSimpleName() + " [arguments]\n\n";
        if (mainClass.getAnnotation(ExperimentalFeature.class) != null) {
            return EXPERIMENTAL_PREFIX + preamble;
        } else if (mainClass.getAnnotation(BetaFeature.class) != null) {
            return BETA_PREFIX + preamble;
        } else {
            return preamble;
        }
    }

    // Validate any argument values. For now, only validates that any mutex targets exist. NOTE: it isn't
    // a requirement for correct behavior that mutex arguments have symmetric declarations, and its not enforced.
    // But they should be declared that way so that the relationship is reflected in usage/help/doc for all
    // arguments involved.
    private void validateArgumentDefinitions() {
        for (final NamedArgumentDefinition mutexSourceDef : namedArgumentDefinitions) {
            for (final String mutexTarget : mutexSourceDef.getMutexTargetList()) {
                final NamedArgumentDefinition mutexTargetDef = namedArgumentsDefinitionsByAlias.get(mutexTarget);
                if (mutexTargetDef == null) {
                    throw new CommandLineException.CommandLineParserInternalException(
                            String.format("Argument '%s' references a nonexistent mutex argument '%s'",
                                    mutexSourceDef.getArgumentAliasDisplayString(),
                                    mutexTarget)
                    );
                } else {
                    // Add at least one alias for the mutex source to the mutex target to ensure the
                    // relationship is symmetric for purposes of usage/help doc display.
                    mutexTargetDef.addMutexTarget(mutexSourceDef.getLongName());
                }
            }
        }
    }

    private String getUsagePreamble() {
        final StringBuilder usagePreamble = new StringBuilder();
        if (null != programProperties) {
            usagePreamble.append(programProperties.summary());
        } else if (positionalArgumentDefinition == null) {
            usagePreamble.append(defaultUsagePreamble);
        } else {
            usagePreamble.append(defaultUsagePreambleWithPositionalArguments);
        }
        return usagePreamble.toString();
    }

    private void addAllAliases(final NamedArgumentDefinition arg){
        for (final String key: arg.getArgumentAliases()) {
            namedArgumentsDefinitionsByAlias.put(key, arg);
        }
    }

    private boolean inArgumentMap(final NamedArgumentDefinition arg){
        for (final String key: arg.getArgumentAliases()){
            if (namedArgumentsDefinitionsByAlias.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private void createArgumentDefinitions(
            final Object callerArguments,
            final CommandLinePluginDescriptor<?> controllingDescriptor)
    {
        for (final Field field : CommandLineParserUtilities.getAllFields(callerArguments.getClass())) {
            final PositionalArguments positionalArgumentAnnotation = field.getAnnotation(PositionalArguments.class);
            final Argument argumentAnnotation = field.getAnnotation(Argument.class);
            final ArgumentCollection argumentCollectionAnnotation = field.getAnnotation(ArgumentCollection.class);

            final String errorString = String.format(
                    "Field %s: Only one of @Argument, @ArgumentCollection or @PositionalArguments can be used", field.getName());
            if (positionalArgumentAnnotation != null) {
                if (argumentAnnotation != null || argumentCollectionAnnotation != null) {
                    throw new CommandLineException.CommandLineParserInternalException(errorString);
                }
                positionalArgumentDefinition = handlePositionalArgumentAnnotation(positionalArgumentAnnotation, callerArguments, field);
            }
            else if (argumentAnnotation != null) {
                if (argumentCollectionAnnotation != null) {
                    throw new CommandLineException.CommandLineParserInternalException(errorString);
                }
                handleArgumentAnnotation(argumentAnnotation, callerArguments, field, controllingDescriptor);
            }
            else if (argumentCollectionAnnotation != null) {
                try {
                    field.setAccessible(true);
                    final Object fieldInstance = field.get(callerArguments);
                    if (fieldInstance == null) {
                        throw new CommandLineException.CommandLineParserInternalException(
                                String.format(
                                        "The ArgumentCollection field '%s' in '%s' must have an initial value",
                                        field.getName(),
                                        callerArguments.getClass().getName()));
                    }
                    createArgumentDefinitions(fieldInstance, controllingDescriptor);
                } catch (final IllegalAccessException e) {
                    throw new CommandLineException.ShouldNeverReachHereException("should never reach here because we setAccessible(true)", e);
                }
            }
        }
    }

    // Find all the instances of plugins specified by the provided plugin descriptors
    private void createCommandLinePluginArgumentDefinitions(
            final List<? extends CommandLinePluginDescriptor<?>> requestedPluginDescriptors)
    {
        // For each descriptor, create the argument definitions for the descriptor object itself,
        // then process it's plugin classes
        requestedPluginDescriptors.forEach(
                descriptor -> {
                    pluginDescriptors.put(descriptor.getClass().getName(), descriptor);
                    createArgumentDefinitions(descriptor, null);
                    findPluginsForDescriptor(descriptor);
                }
        );
    }

    // Find all of the classes that derive from the class specified by the descriptor, obtain an
    // instance each and add its ArgumentDefinitions
    private void findPluginsForDescriptor(final CommandLinePluginDescriptor<?> pluginDescriptor)
    {
        final ClassFinder classFinder = new ClassFinder();
        pluginDescriptor.getPackageNames().forEach(
                pkg -> classFinder.find(pkg, pluginDescriptor.getPluginBaseClass()));
        final Set<Class<?>> pluginClasses = classFinder.getClasses();

        for (Class<?> c : pluginClasses) {
            if (pluginDescriptor.includePluginClass(c)) {
                try {
                    final Object plugin = pluginDescriptor.createInstanceForPlugin(c);
                    createArgumentDefinitions(plugin, pluginDescriptor);
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new CommandLineException.CommandLineParserInternalException("Problem making an instance of plugin " + c +
                            " Do check that the class has a non-arg constructor", e);
                }
            }
        }
    }

    private final void printArgumentUsageBlock(final StringBuilder sb, final String preamble, final List<NamedArgumentDefinition> args) {
        if (args != null && !args.isEmpty()) {
            sb.append(preamble);
            args.stream().sorted(NamedArgumentDefinition.sortByLongName)
                    .forEach(argumentDefinition -> sb.append(argumentDefinition.getArgumentUsage(
                            namedArgumentsDefinitionsByAlias,
                            pluginDescriptors.values(),
                            ARGUMENT_COLUMN_WIDTH,
                            DESCRIPTION_COLUMN_WIDTH)));
        }
    }

    /**
     * Print a usage message based on the arguments object passed to the ctor.
     *
     * @param printCommon True if common args should be included in the usage message.
     * @param printHidden True if hidden args should be included in the usage message.
     * @return Usage string generated by the command line parser.
     */
    @Override
    public String usage(final boolean printCommon, final boolean printHidden) {
        final StringBuilder sb = new StringBuilder();

        final String preamble = getStandardUsagePreamble(callerArguments.getClass()) + getUsagePreamble();
        sb.append(Utils.wrapParagraph(preamble,DESCRIPTION_COLUMN_WIDTH + ARGUMENT_COLUMN_WIDTH));
        sb.append("\n" + getVersion() + "\n");

        // filter on common and partition on plugin-controlled
        final Map<Boolean, List<NamedArgumentDefinition>> allArgsMap = namedArgumentDefinitions.stream()
                .filter(argumentDefinition -> printCommon || !argumentDefinition.isCommon())
                .filter(argumentDefinition -> printHidden || !argumentDefinition.isHidden())
                .collect(Collectors.partitioningBy(a -> a.getDescriptorForControllingPlugin() == null));

        final List<NamedArgumentDefinition> nonPluginArgs = allArgsMap.get(true);
        if (null != nonPluginArgs && !nonPluginArgs.isEmpty()) {
            // partition the non-plugin args on optional
            final Map<Boolean, List<NamedArgumentDefinition>> unconditionalArgsMap = nonPluginArgs.stream()
                    .collect(Collectors.partitioningBy(a -> a.isOptional()));

            // required args
            printArgumentUsageBlock(sb, "\n\nRequired Arguments:\n\n", unconditionalArgsMap.get(false));

            // optional args split by advanced
            final List<NamedArgumentDefinition> optArgs = unconditionalArgsMap.get(true);
            if (null != optArgs && !optArgs.isEmpty()) {
                final Map<Boolean, List<NamedArgumentDefinition>> byAdvanced = optArgs.stream()
                        .collect(Collectors.partitioningBy(a -> a.isAdvanced()));
                printArgumentUsageBlock(sb, "\nOptional Arguments:\n\n", byAdvanced.get(false));
                printArgumentUsageBlock(sb, "\nAdvanced Arguments:\n\n", byAdvanced.get(true));
            }
        }

        // now the conditional/dependent args (those controlled by a plugin descriptor)
        final List<NamedArgumentDefinition> conditionalArgs = allArgsMap.get(false);
        if (null != conditionalArgs && !conditionalArgs.isEmpty()) {
            // group all of the conditional argdefs by the name of their controlling pluginDescriptor class
            final Map<CommandLinePluginDescriptor<?>, List<NamedArgumentDefinition>> argsByControllingDescriptor =
                    conditionalArgs
                            .stream()
                            .collect(Collectors.groupingBy(argDef -> argDef.getDescriptorForControllingPlugin()));

            // sort the list of controlling pluginDescriptors by display name and iterate through them
            final List<CommandLinePluginDescriptor<?>> pluginDescriptorSortedByName =
                    new ArrayList<>(argsByControllingDescriptor.keySet());
            pluginDescriptorSortedByName.sort(
                    (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getDisplayName(), b.getDisplayName())
            );
            for (final CommandLinePluginDescriptor<?> descriptor: pluginDescriptorSortedByName) {
                sb.append("Conditional Arguments for " + descriptor.getDisplayName() + ":\n\n");
                // get all the argument definitions controlled by this pluginDescriptor's plugins, group
                // those by plugin, and get the sorted list of names of the owning plugins
                final Map<String, List<NamedArgumentDefinition>> byPlugin =
                        argsByControllingDescriptor.get(descriptor)
                                .stream()
                                .collect(Collectors.groupingBy(argDef -> argDef.getContainingObject().getClass().getSimpleName()));
                final List<String> sortedPluginNames = new ArrayList<>(byPlugin.keySet());
                sortedPluginNames.sort(String.CASE_INSENSITIVE_ORDER);

                // iterate over the owning plugins in sorted order, get each one's argdefs in sorted order,
                // and print their usage
                for (final String pluginName: sortedPluginNames) {
                    printArgumentUsageBlock(sb, "Valid only if \"" + pluginName + "\" is specified:\n", byPlugin.get(pluginName));
                }
            }
        }

        return sb.toString();
    }

    private boolean propagateParsedValues(final OptionSet parsedArguments, final PrintStream messageStream) {
        // named args first
        for (final OptionSpec<?> optSpec : parsedArguments.asMap().keySet()) {
            if (parsedArguments.has(optSpec)) {
                final NamedArgumentDefinition namedArgumentDefinition = namedArgumentsDefinitionsByAlias.get(optSpec.options().get(0));
                // Note that these values can be preprocessed tag surrogates
                namedArgumentDefinition.setArgumentValues(
                        this,
                        messageStream,
                        optSpec.values(parsedArguments)
                            .stream()
                            .map(Object::toString)
                            .collect(Collectors.toList())
                );
            }
        }
        // positional args
        if (!parsedArguments.nonOptionArguments().isEmpty()) {
            final List<String> stringValues = parsedArguments.nonOptionArguments()
                    .stream()
                    .map(v -> v.toString())
                    .collect(Collectors.toList());
            if (positionalArgumentDefinition == null) {
                throw new CommandLineException.BadArgumentValue(String.format(
                        "Positional arguments were provided '%s' but no positional argument is defined for this tool.",
                        stringValues.stream().collect(Collectors.joining("{", ",","}"))));
            }
            positionalArgumentDefinition.setArgumentValues(this, messageStream, stringValues);
        }

        validateArgumentValues();

        return true;
    }

    private List<String> expandFromArgumentFile(final OptionSet parsedArguments) {
        final List<String> argfiles = parsedArguments.valuesOf(SpecialArgumentsCollection.ARGUMENTS_FILE_FULLNAME).stream()
                .map(f -> (String)f)
                .collect(Collectors.toList());
        final List<String> newArgs = argfiles.stream()
                .distinct()
                .filter(file -> !argumentsFilesLoadedAlready.contains(file))
                .flatMap(file -> loadArgumentsFile(file).stream())
                .collect(Collectors.toList());
        argumentsFilesLoadedAlready.addAll(argfiles);

        return newArgs;
    }

    private OptionParser getOptionParser() {
        final OptionParser parser = new OptionParser(false);
        for (final NamedArgumentDefinition argDef : namedArgumentDefinitions){
            final OptionSpecBuilder bld = parser.acceptsAll(argDef.getArgumentAliases(), argDef.getDocString());
            if (argDef.isFlag()) {
                bld.withOptionalArg().withValuesConvertedBy(new StrictBooleanConverter());
            } else {
                bld.withRequiredArg();
            }
        }

        if (positionalArgumentDefinition != null) {
            parser.nonOptions();
        }

        return parser;
    }

    /**
     *  helper to deal with the case of special flags that are evaluated before the options are properly set
     */
    private boolean isSpecialFlagSet(final OptionSet parsedArguments, final String flagName){
        if (parsedArguments.has(flagName)){
            Object value = parsedArguments.valueOf(flagName);
            return  (value == null || !value.equals("false"));
        } else{
            return false;
        }
    }

    /**
     * After command line has been parsed, make sure that all required arguments have values, and that
     * lists with minimum # of elements have sufficient values.
     *
     * @throws CommandLineException if arguments requirements are not satisfied.
     */
    private void validateArgumentValues()  {
        namedArgumentDefinitions = validatePluginArgumentValues(); // trim the list of plugin-derived argument definitions before validation
        for (final NamedArgumentDefinition argumentDefinition : namedArgumentDefinitions) {
            argumentDefinition.validateValues(this);
        }
        if (positionalArgumentDefinition != null) {
            positionalArgumentDefinition.validateValues(this);
        }
    }

    // Once all command line args have been processed, go through the argument definitions and
    // validate any that are plugin class arguments against the controlling descriptor, trimming
    // the list of argument definitions along the way by removing any that have not been set
    // (so validation doesn't complain about missing required arguments for plugins that weren't
    // specified) and throwing for any that have been set but are not allowed. Note that we don't trim
    // the list of plugins themselves (just the argument definitions), since the plugin may contain
    // other arguments that require validation.
    private List<NamedArgumentDefinition> validatePluginArgumentValues() {
        final List<NamedArgumentDefinition> actualArgumentDefinitions = new ArrayList<>();
        for (final NamedArgumentDefinition argumentDefinition : namedArgumentDefinitions) {
            if (!argumentDefinition.isControlledByPlugin()) {
                actualArgumentDefinitions.add(argumentDefinition);
            } else {
                final boolean isAllowed = argumentDefinition.getDescriptorForControllingPlugin().isDependentArgumentAllowed(
                        argumentDefinition.getContainingObject().getClass());
                if (argumentDefinition.getHasBeenSet()) {
                    if (!isAllowed) {
                        // dangling dependent argument; a value was specified but it's containing
                        // (predecessor) plugin argument wasn't specified
                        throw new CommandLineException(
                                String.format(
                                        "Argument \"%s/%s\" is only valid when the argument \"%s\" is specified",
                                        argumentDefinition.getShortName(),
                                        argumentDefinition.getLongName(),
                                        argumentDefinition.getContainingObject().getClass().getSimpleName()));
                    }
                    actualArgumentDefinitions.add(argumentDefinition);
                } else if (isAllowed) {
                    // the predecessor argument was seen, so this value is allowed but hasn't been set; keep the
                    // argument definition to allow validation to check for missing required args
                    actualArgumentDefinitions.add(argumentDefinition);
                }
            }
        }

        // finally, give each plugin a chance to trim down any unseen instances from it's own list
        pluginDescriptors.entrySet().forEach(e -> e.getValue().validateAndResolvePlugins());

        // return the updated list of argument definitions with the new list
        return actualArgumentDefinitions;
    }

    /**
     * Expand any collection value that references a filename that ends in one of the accepted expansion
     * extensions, and add the contents of the file to the list of values for that argument.
     * @param argumentDefinition ArgumentDefinition for the arg being populated
     * @param stringValue the argument value as presented on the command line
     * @param originalValuesForPreservation list of original values provided on the command line
     * @return a list containing the original entries in {@code originalValues}, with any
     * values from list files expanded in place, preserving both the original list order and
     * the file order
     */
    public List<String> expandFromExpansionFile(
            final ArgumentDefinition argumentDefinition,
            final PrintStream messageStream,
            final String stringValue,
            final List<String> originalValuesForPreservation) {
        List<String> expandedValues = new ArrayList<>();
        if (EXPANSION_FILE_EXTENSIONS.stream().anyMatch(ext -> stringValue.endsWith(ext))) {
            // If any value provided for this argument is an expansion file, expand it in place,
            // but  preserve the original values for subsequent retrieval during command line
            // display, since expansion files can result in very large post-expansion command lines
            // (its harmless to update this multiple times).
            expandedValues.addAll(loadCollectionListFile(stringValue, messageStream));
            argumentDefinition.setOriginalCommandLineValues(originalValuesForPreservation);
        } else {
            expandedValues.add(stringValue);
        }
        return expandedValues;
    }

    /**
     * Read a list file and return a list of the collection values contained in it
     * Any line that starts with {@link CommandLineArgumentParser#ARGUMENT_FILE_COMMENT} is ignored.
     *
     * @param collectionListFile a text file containing list values
     * @return false if a fatal error occurred
     */
    private static List<String> loadCollectionListFile(
            final String collectionListFile,
            final PrintStream messageStream) {
        try (BufferedReader reader = new BufferedReader(new FileReader(collectionListFile))){
            final List<String> filteredStrings = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith(ARGUMENT_FILE_COMMENT))
                    .collect(Collectors.toList());
            final List<String> suspiciousString = filteredStrings.stream().filter(s -> s.startsWith("@")).limit(1).collect(Collectors.toList());
            if (!suspiciousString.isEmpty()) {
                // looks suspiciously like a sequence dictionary...
                messageStream.println(String.format(
                        "WARNING: the file %s has a file extension that causes it to be expanded by the argument parser into multiple argument values , " +
                        "but contains lines with leading '@' characters that may indicate this was unintentional (%s).",
                        collectionListFile,
                        suspiciousString));
            }
            return filteredStrings;
        } catch (final IOException e) {
            throw new CommandLineException("I/O error loading list file:" + collectionListFile, e);
        }
    }

    /**
     * Read an argument file and return a list of the args contained in it
     * A line that starts with {@link #ARGUMENT_FILE_COMMENT}  is ignored.
     *
     * @param argumentsFile a text file containing args
     * @return false if a fatal error occurred
     */
    private List<String> loadArgumentsFile(final String argumentsFile) {
        List<String> args = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(argumentsFile))){
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(ARGUMENT_FILE_COMMENT) && !line.trim().isEmpty()) {
                    args.addAll(Arrays.asList(StringUtils.split(line)));
                }
            }
        } catch (final IOException e) {
            throw new CommandLineException("I/O error loading arguments file:" + argumentsFile, e);
        }
        return args;
    }

    private void handleArgumentAnnotation(
            final Argument argumentAnnotation,
            final Object parent,
            final Field field,
            final CommandLinePluginDescriptor<?> controllingDescriptor)
    {
        final NamedArgumentDefinition argumentDefinition =
                new NamedArgumentDefinition(argumentAnnotation, parent, field, controllingDescriptor);

        if (inArgumentMap(argumentDefinition)) {
            throw new CommandLineException.CommandLineParserInternalException(
                    argumentDefinition.getArgumentAliasDisplayString() + " has already been used.");
        } else {
            addAllAliases(argumentDefinition);
            namedArgumentDefinitions.add(argumentDefinition);
        }
    }

    private PositionalArgumentDefinition handlePositionalArgumentAnnotation(
            final PositionalArguments positionalArguments,
            final Object parent,
            final Field field) {
        if (positionalArgumentDefinition != null) {
            throw new CommandLineException.CommandLineParserInternalException
                    ("@PositionalArguments cannot be used more than once in an argument class.");
        }
        return new PositionalArgumentDefinition(positionalArguments, parent, field);
    }

    /**
     * Locates and returns the VALUES of all Argument-annotated fields of a specified type in a given object,
     * pairing each field value with its corresponding Field object.
     *
     * Must be called AFTER argument parsing and value injection into argumentSource is complete (otherwise there
     * will be no values to gather!).
     *
     * Locates Argument-annotated fields of the target type, subtypes of the target type, and Collections of
     * the target type or one of its subtypes. Unpacks Collection fields, returning a separate Pair for each
     * value in each Collection.
     *
     * Searches argumentSource itself, as well as ancestor classes, and also recurses into any ArgumentCollections
     * found.
     *
     * Will return Pairs containing a null second element for fields having no value, including empty Collection fields
     * (these represent arguments of the target type that were not specified on the command line and so never initialized).
     *
     * @param type Target type. Search for Argument-annotated fields that are either of this type, subtypes of this type, or Collections of this type or one of its subtypes.
     * @param <T> Type parameter representing the type to search for and return
     * @return A List of Pairs containing all Argument-annotated field values found of the target type. First element in each Pair
     *         is the ArgumentDefinition object, and the second element is the actual value of the argument field. The second
     *         element will be null for uninitialized fields.
     */
    @Override
    public <T> List<Pair<ArgumentDefinition, T>> gatherArgumentValuesOfType( final Class<T> type ) {
        final List<Pair<ArgumentDefinition, T>> argumentValues = new ArrayList<>();

        // include all named and positional argument definitions
        final List<ArgumentDefinition> allArgDefs = new ArrayList<>(namedArgumentDefinitions.size());
        allArgDefs.addAll(namedArgumentDefinitions);
        if (positionalArgumentDefinition != null) {
            allArgDefs.add(positionalArgumentDefinition);
        }

        for ( final ArgumentDefinition argDef : allArgDefs) {
            if ( type.isAssignableFrom(argDef.getUnderlyingFieldClass()) ) {
                // Consider only fields that are either of the target type, subtypes of the target type,
                // or Collections of the target type or one of its subtypes:

                if ( argDef.isCollection() ) {
                    // Collection arguments are guaranteed by the parsing system to be non-null (at worst, empty)
                    final Collection<?> argumentContainer = (Collection<?>) argDef.getArgumentValue();

                    // Emit a Pair with an explicit null value for empty Collection arguments
                    if (argumentContainer.isEmpty()) {
                        argumentValues.add(Pair.of(argDef, null));
                    }
                    // Unpack non-empty Collections of the target type into individual values,
                    // each paired with the same Field object.
                    else {
                        for (final Object argumentValue : argumentContainer) {
                            argumentValues.add(Pair.of(argDef, type.cast(argumentValue)));
                        }
                    }
                }
                else {
                    // Add values for non-Collection arguments of the target type directly
                    argumentValues.add(Pair.of(argDef, type.cast(argDef.getArgumentValue())));
                }
            }
        }

        return argumentValues;
    }

}
