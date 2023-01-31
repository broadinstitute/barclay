package org.broadinstitute.barclay.help;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;

import javax.lang.model.element.Element;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * For testing of Bash tab completion generation.
 *
 * Using this to generate tab-completion files requires that there is a
 * wrapper script around the call to java that acts as a user-interface.
 *
 * This is required because of how Bash handles tab completion - it keys off
 * of the first word typed in a line.  When invoking directly from java, Bash
 * will complete for the `java` command, but will not know how to complete for a
 * jar incorporating Barclay-enabled arguments.
 *
 * This is a known issue and is being investigated for remedies in the future.
 */
public class BashTabCompletionDoclet extends HelpDoclet {

    // Barclay BashTabCompletionDoclet custom Command-line Arguments:

    // All these arguments are optional, but it is highly recommended
    // to specify the caller script name.

    final private static String CALLER_SCRIPT_NAME = "-caller-script-name";

    final private static String CALLER_SCRIPT_PREFIX_LEGAL_ARGS = "-caller-pre-legal-args";
    final private static String CALLER_SCRIPT_PREFIX_ARG_VALUE_TYPES = "-caller-pre-arg-val-types";
    final private static String CALLER_SCRIPT_PREFIX_MUTEX_ARGS = "-caller-pre-mutex-args";
    final private static String CALLER_SCRIPT_PREFIX_ALIAS_ARGS = "-caller-pre-alias-args";
    final private static String CALLER_SCRIPT_PREFIX_ARG_MIN_OCCURRENCES = "-caller-pre-arg-min-occurs";
    final private static String CALLER_SCRIPT_PREFIX_ARG_MAX_OCCURRENCES = "-caller-pre-arg-max-occurs";

    final private static String CALLER_SCRIPT_POSTFIX_LEGAL_ARGS = "-caller-post-legal-args";
    final private static String CALLER_SCRIPT_POSTFIX_ARG_VALUE_TYPES = "-caller-post-arg-val-types";
    final private static String CALLER_SCRIPT_POSTFIX_MUTEX_ARGS = "-caller-post-mutex-args";
    final private static String CALLER_SCRIPT_POSTFIX_ALIAS_ARGS = "-caller-post-alias-args";
    final private static String CALLER_SCRIPT_POSTFIX_ARG_MIN_OCCURRENCES = "-caller-post-arg-min-occurs";
    final private static String CALLER_SCRIPT_POSTFIX_ARG_MAX_OCCURRENCES = "-caller-post-arg-max-occurs";

    // =============================================

    // Variables that are set on the command line when running this doclet:

    /**
     * Name of the executable / wrapper script that will actually invoke the java process.
     * This wrapper script would call into the JAR and tell it which class to run.
     */
    private String callerScriptName = null;

    /**
     * Arguments to the executable / wrapper script that come before any Java class names / tools.
     * <p>
     * This is expected to be a space-delimited string with the options themselves as they should be
     * typed by the user.
     * <p>
     * This syntax is used to pass this information to directly to the bash completion script.
     * <p>
     * Example: {@code "--help --info --list --inputFile --outFolder --memSize --multiplier"}
     */
    private String callerScriptPrefixLegalArgs = "";

    /**
     * Types of the arguments that the executable / wrapper script is expecting before any Java class names / tools.
     * The order of these space-delimited types should correspond to the contents of
     * {@link #callerScriptPrefixLegalArgs}
     * <p>
     * This is expected to be a space-delimited string of types.
     * <p>
     * Currently accepted type values are the following (not case-sensitive):
     * <p>
     * {@code file}
     * {@code folder}
     * {@code directory}
     * {@code int}
     * {@code long}
     * {@code double}
     * {@code float}
     * {@code null}   (to be used in the case of an argument that acts as a flag [i.e. one that takes no additional
     * input])
     * <p>
     * This syntax is used to pass this information to directly to the bash completion script.
     * <p>
     * Example: {@code "null null null file directory int double"}
     */
    private String callerScriptPrefixArgValueTypes = "";

    /**
     * Sets of arguments to the executable / wrapper script that are mutually exclusive to each other and
     * are expected before any Java class names / tools.
     * <p>
     * This is expected to be a string with mutex information for each argument that is mutually exclusive with another
     * argument.  The format for this string is:
     * <p>
     * {@code FOO;mutexToFoo1[,mutexToFoo2][,mutexToFoo3]... BAR;mutexToBar1[,mutexToBar2][,mutexToBar3]... }
     * <p>
     * where:
     * <p>
     * {@code FOO} is an argument to the wrapper script which is expected before any Java class names / tools
     * {@code mutexToFoo1} is an argument with which {@code FOO} is mutually exclusive without leading decorators
     * (usually - or --)
     * {@code mutexToFoo2} is an argument with which {@code FOO} is mutually exclusive without leading decorators
     * (usually - or --)
     * {@code mutexToFoo3} is an argument with which {@code FOO} is mutually exclusive without leading decorators
     * (usually - or --)
     * and
     * {@code BAR} is an argument to the wrapper script which is expected before any Java class names / tools
     * {@code mutexToBar1} is an argument with which {@code BAR} is mutually exclusive without leading decorators
     * (usually - or --)
     * {@code mutexToBar2} is an argument with which {@code BAR} is mutually exclusive without leading decorators
     * (usually - or --)
     * {@code mutexToBar3} is an argument with which {@code BAR} is mutually exclusive without leading decorators
     * (usually - or --)
     * <p>
     * This can be thought of as a set of such argument relationships and does not have any ordering scheme.
     * <p>
     * This syntax is used to pass this information to directly to the bash completion script.
     * <p>
     * Example: {@code "--help;info,list,inputFile --info;help,list,inputFile"}
     */
    private String callerScriptPrefixMutexArgs = "";

    /**
     * Sets of arguments to the executable / wrapper script that are aliases of each other and
     * are expected before any Java class names / tools.
     * For example, full argument names and short names for those arguments.
     * <p>
     * This is expected to be a string with alias information for each argument that is an alias of another
     * argument.  The format for this string is:
     * <p>
     * {@code FOO;aliasToFoo1[,aliasToFoo2][,aliasToFoo3]... BAR;aliasToBar1[,aliasToBar2][,aliasToBar3]... }
     * <p>
     * where:
     * <p>
     * {@code FOO} is an argument to the wrapper script which is expected before any Java class names / tools
     * {@code aliasToFoo1} is an argument which is an alias to {@code FOO}
     * {@code aliasToFoo2} is an argument which is an alias to {@code FOO}
     * {@code aliasToFoo3} is an argument which is an alias to {@code FOO}
     * and
     * {@code BAR} is an argument to the wrapper script which is expected before any Java class names / tools
     * {@code aliasToBar1} is an argument which is an alias to {@code BAR}
     * {@code aliasToBar2} is an argument which is an alias to {@code BAR}
     * {@code aliasToBar3} is an argument which is an alias to {@code BAR}
     * <p>
     * This can be thought of as a set of such argument relationships and does not have any ordering scheme.
     * <p>
     * This syntax is used to pass this information to directly to the bash completion script.
     * <p>
     * Example: {@code "--help;-h --info;-i --inputFile;-if,-infile,-inny"}
     */
    private String callerScriptPrefixAliasArgs = "";

    /**
     * The minimum number of occurrences of each argument that the executable / wrapper script is expecting
     * before any Java class names / tools.
     * This is expected to be a space-delimited string with the min occurrences as {@code integer} values.
     * <p>
     * The order of these space-delimited values should correspond to the contents of
     * {@link #callerScriptPrefixLegalArgs}
     * <p>
     * This is used in the logic that tracks the number of times an option is specified.
     * <p>
     * This syntax is used to pass this information to directly to the bash completion script.
     * <p>
     * Example: {@code "0 0 0 1 1 0 0 0"}
     */
    private String callerScriptPrefixMinOccurrences = "";

    /**
     * The maximum number of occurrences of each argument that the executable / wrapper script is expecting
     * before any Java class names / tools.
     * This is expected to be a space-delimited string with the max occurrences as {@code integer} values.
     * <p>
     * The order of these space-delimited values should correspond to the contents of
     * {@link #callerScriptPrefixLegalArgs}
     * <p>
     * This is used in the logic that tracks the number of times an option is specified.
     * <p>
     * This syntax is used to pass this information to directly to the bash completion script.
     * <p>
     * Example: {@code "1 1 1 1 1 1 1 1"}
     */
    private String callerScriptPrefixMaxOccurrences = "";


    /**
     * Arguments to the executable / wrapper script that come after any Java class names / tools.  The start of these
     * options is indicated by the user inputting the special option {@code --}
     * <p>
     * The format of this variable is identical to {@link #callerScriptPrefixLegalArgs}
     */
    private String callerScriptPostfixLegalArgs = "";

    /**
     * Types of the arguments that the executable / wrapper script is expecting after any Java class
     * names / tools.  The start of these options is indicated by the user inputting the special option {@code --}
     * <p>
     * The order of these space-delimited types should correspond to the contents of
     * {@link #callerScriptPostfixLegalArgs}
     * <p>
     * The format of this variable is identical to {@link #callerScriptPrefixArgValueTypes}
     */
    private String callerScriptPostfixArgValueTypes = "";

    /**
     * Sets of arguments to the executable / wrapper script that are mutually exclusive to each other and
     * are expected after any Java class names / tools.  The start of these options is indicated by the user
     * inputting the special option {@code --}
     * <p>
     * The format of this variable is identical to {@link #callerScriptPrefixMutexArgs}
     */
    private String callerScriptPostfixMutexArgs = "";

    /**
     * Sets of arguments to the executable / wrapper script that are aliases of each other and
     * are expected after any Java class names / tools.  The start of these options is indicated by the user
     * inputting the special option {@code --}
     * <p>
     * The format of this variable is identical to {@link #callerScriptPrefixAliasArgs}
     */
    private String callerScriptPostfixAliasArgs = "";

    /**
     * The minimum number of occurrences of each argument that the executable / wrapper script is expecting
     * after any Java class names / tools.  The start of these options is indicated by the user
     * inputting the special option {@code --}
     * <p>
     * The order of these space-delimited types should correspond to the contents of
     * {@link #callerScriptPostfixLegalArgs}
     * <p>
     * The format of this variable is identical to {@link #callerScriptPrefixMinOccurrences}
     */
    private String callerScriptPostfixMinOccurrences = "";

    /**
     * The maximum number of occurrences of each argument that the executable / wrapper script is expecting
     * after any Java class names / tools.  The start of these options is indicated by the user
     * inputting the special option {@code --}
     * <p>
     * The order of these space-delimited types should correspond to the contents of
     * {@link #callerScriptPostfixLegalArgs}
     * <p>
     * The format of this variable is identical to {@link #callerScriptPrefixMaxOccurrences}
     */
    private String callerScriptPostfixMaxOccurrences = "";

    /**
     * True if the executable / wrapper script has arguments that are expected after any Java class names / tools.
     * The start of these options is indicated by the user inputting the special option {@code --}
     * The value of this is set internally based on the contents of {@link #callerScriptPostfixLegalArgs}
     */
    private boolean hasCallerScriptPostfixArgs = false;

    // =============================================

    private String quoteEachWord(final String sentence) {
        return quoteEachWord(sentence, " ");
    }

    private String quoteEachWord(final String sentence, final String sep) {

        return Stream.of(sentence.split(sep))
                .map(s -> String.format("\"%s\"", s))
                .collect(Collectors.joining(sep));
    }

    @Override
    protected void validateDocletStartingState() {
        if (callerScriptName == null) {
            // The user did not specify the caller script name.
            // We cannot function under these conditions:
            throw new RuntimeException("ERROR: You must specify a caller script name using the option: " + CALLER_SCRIPT_NAME);
        }
    }

    @Override
    public Set<Option> getSupportedOptions() {
        final Set<Option> tabCompletionOptions = new LinkedHashSet<>() {{
            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_NAME) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    // Remove the last period and anything after it:
                    final int lastDotIndex = arguments.get(0).lastIndexOf('.');
                    if ( lastDotIndex != -1 ) {
                        callerScriptName = arguments.get(0).substring(0, lastDotIndex);
                    }
                    else {
                        callerScriptName = arguments.get(0);
                    }
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_PREFIX_LEGAL_ARGS) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    callerScriptPrefixLegalArgs = arguments.get(0);
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_PREFIX_ARG_VALUE_TYPES) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    callerScriptPrefixArgValueTypes = quoteEachWord(arguments.get(0));
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_PREFIX_MUTEX_ARGS) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    callerScriptPrefixMutexArgs = quoteEachWord(arguments.get(0));
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_PREFIX_ALIAS_ARGS) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    // We have to format this option to contain quotes around each word:
                    callerScriptPrefixAliasArgs = quoteEachWord(arguments.get(0));
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_PREFIX_ARG_MIN_OCCURRENCES) {
                    @Override
                public boolean process(String option, List<String> arguments) {
                    callerScriptPrefixMinOccurrences = arguments.get(0);
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_PREFIX_ARG_MAX_OCCURRENCES) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    callerScriptPrefixMaxOccurrences = arguments.get(0);
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_POSTFIX_LEGAL_ARGS) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    callerScriptPostfixLegalArgs = arguments.get(0);
                    hasCallerScriptPostfixArgs = !callerScriptPostfixLegalArgs.isEmpty();
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_POSTFIX_ARG_VALUE_TYPES) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    // We have to format this option to contain quotes around each word:
                    callerScriptPostfixArgValueTypes = quoteEachWord(arguments.get(0));
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_POSTFIX_MUTEX_ARGS) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    // We have to format this option to contain quotes around each word:
                    callerScriptPostfixMutexArgs = quoteEachWord(arguments.get(0));
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_POSTFIX_ALIAS_ARGS) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    // We have to format this option to contain quotes around each word:
                    callerScriptPostfixAliasArgs = quoteEachWord(arguments.get(0));
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_POSTFIX_ARG_MIN_OCCURRENCES) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    callerScriptPostfixMinOccurrences = arguments.get(0);
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(CALLER_SCRIPT_POSTFIX_ARG_MAX_OCCURRENCES) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    callerScriptPostfixMaxOccurrences = arguments.get(0);
                    return true;
                }
            });

        }};

        tabCompletionOptions.addAll(super.getSupportedOptions());
        return tabCompletionOptions;
    }

    /**
     * Filter out features that are not command line programs by selecting only classes with
     * {@link CommandLineProgramProperties}.
     * @param documentedFeature feature that is being considered for inclusion in the docs
     * @param clazz class that is being considered for inclusion in the docs
     * @return
     */
    @Override
    public boolean includeInDocs(final DocumentedFeature documentedFeature, final Class<?> clazz) {
        return super.includeInDocs(documentedFeature, clazz) &&
                clazz.getAnnotation(CommandLineProgramProperties.class) != null;
    }

    /**
     * Create a work unit and handler capable of handling the feature specified by the input arguments.
     * Returns null if no appropriate handler is found or doc shouldn't be documented at all.
     */
    @Override
    public DocWorkUnit createWorkUnit(
            final Element targetElement,
            final Class<?> clazz,
            final DocumentedFeature documentedFeature)
    {
        return new DocWorkUnit(
                new BashTabCompletionDocWorkUnitHandler(this),
                targetElement,
                clazz,
                documentedFeature);
    }

    @Override
    protected void processWorkUnitTemplate(
            final Configuration cfg,
            final DocWorkUnit workUnit,
            final List<Map<String, String>> indexByGroupMaps,
            final List<Map<String, String>> featureMaps) {

            // For the Bash Test Doclet, this is a no-op.
            // We only care about the index file.
    }

    /**
     * The Index file in the Bash Completion Doclet is what generates the actual tab-completion script.
     *
     * This will actually write out the shell completion output file.
     * The Freemarker instance will see a top-level map that has two keys in it.
     *
     * The first key is for caller script options:
     *
     * SimpleMap callerScriptOptions = SimpleMap {
     *
     *   "callerScriptName"                 : caller script name
     *
     *   "callerScriptPrefixLegalArgs"      : caller Script Prefix Legal Args
     *   "callerScriptPrefixArgValueTypes"  : caller Script Prefix Arg Value Types
     *   "callerScriptPrefixMutexArgs"      : caller Script Prefix Mutex Args
     *   "callerScriptPrefixAliasArgs"      : caller Script Prefix Alias Args
     *   "callerScriptPrefixMinOccurrences" : caller Script Prefix Min Occurrences
     *   "callerScriptPrefixMaxOccurrences" : caller Script Prefix Max Occurrences
     *   "hasCallerScriptPrefixArgs"        : has Caller Script Prefix Args
     *
     *   "callerScriptPostfixLegalArgs"      : caller Script Postfix Legal Args
     *   "callerScriptPostfixArgValueTypes"  : caller Script Postfix Arg Value Types
     *   "callerScriptPostfixMutexArgs"      : caller Script Postfix Mutex Args
     *   "callerScriptPostfixAliasArgs"      : caller Script Postfix Alias Args
     *   "callerScriptPostfixMinOccurrences" : caller Script Postfix Min Occurrences
     *   "callerScriptPostfixMaxOccurrences" : caller Script Postfix Max Occurrences
     *   "hasCallerScriptPostfixArgs"        : has Caller Script Postfix Args
     *
     * }
     *
     * The second key is for tool options:
     *
     * SimpleMap tools = SimpleMap { ToolName : MasterPropertiesMap }
     *
     *     where
     *
     *     MasterPropertiesMap is a map containing the following Keys:
     *         all
     *         common
     *         positional
     *         hidden
     *         advanced
     *         deprecated
     *         optional
     *         dependent
     *         required
     *
     *         Each of those keys maps to a List&lt;SimpleMap&gt; representing each property.
     *         These property maps each contain the following keys:
     *
     *             kind
     *             name
     *             summary
     *             fulltext
     *             otherArgumentRequired
     *             synonyms
     *             exclusiveOf
     *             type
     *             options
     *             attributes
     *             required
     *             minRecValue
     *             maxRecValue
     *             minValue
     *             maxValue
     *             defaultValue
     *             minElements
     *             maxElements
     *
     * @param cfg
     * @param workUnitList
     * @param groupMaps
     * @throws IOException
     */
    @Override
    protected void processIndexTemplate(
            final Configuration cfg,
            final List<DocWorkUnit> workUnitList,
            final List<Map<String, String>> groupMaps
    ) throws IOException {
        // Create a root map for all the work units so we can access all the info we need:
        final Map<String, Object> propertiesMap = new HashMap<>();
        workUnits.stream().forEach( workUnit -> propertiesMap.put(workUnit.getName(), workUnit.getRootMap()) );

        // Add everything into a nice package that we can iterate over
        // while exposing the command line program names as keys:
        final Map<String, Object> rootMap = new HashMap<>();
        rootMap.put("tools", propertiesMap);

        // Add the caller script options into another top-level tree node:
        final Map<String, Object> callerScriptOptionsMap = new HashMap<>();
        callerScriptOptionsMap.put("callerScriptName", callerScriptName);

        callerScriptOptionsMap.put("callerScriptPrefixLegalArgs", callerScriptPrefixLegalArgs);
        callerScriptOptionsMap.put("callerScriptPrefixArgValueTypes", callerScriptPrefixArgValueTypes);
        callerScriptOptionsMap.put("callerScriptPrefixMutexArgs", callerScriptPrefixMutexArgs);
        callerScriptOptionsMap.put("callerScriptPrefixAliasArgs", callerScriptPrefixAliasArgs);
        callerScriptOptionsMap.put("callerScriptPrefixMinOccurrences", callerScriptPrefixMinOccurrences);
        callerScriptOptionsMap.put("callerScriptPrefixMaxOccurrences", callerScriptPrefixMaxOccurrences);

        callerScriptOptionsMap.put("callerScriptPostfixLegalArgs", callerScriptPostfixLegalArgs);
        callerScriptOptionsMap.put("callerScriptPostfixArgValueTypes", callerScriptPostfixArgValueTypes);
        callerScriptOptionsMap.put("callerScriptPostfixMutexArgs", callerScriptPostfixMutexArgs);
        callerScriptOptionsMap.put("callerScriptPostfixAliasArgs", callerScriptPostfixAliasArgs);
        callerScriptOptionsMap.put("callerScriptPostfixMinOccurrences", callerScriptPostfixMinOccurrences);
        callerScriptOptionsMap.put("callerScriptPostfixMaxOccurrences", callerScriptPostfixMaxOccurrences);
        if ( hasCallerScriptPostfixArgs ) {
            callerScriptOptionsMap.put("hasCallerScriptPostfixArgs", "true");
        }
        else {
            callerScriptOptionsMap.put("hasCallerScriptPostfixArgs", "false");
        }

        rootMap.put("callerScriptOptions", callerScriptOptionsMap);

        // Get or create a template
        final Template template = cfg.getTemplate(getIndexTemplateName());

        // Create the output file
        final File indexFile = new File(getDestinationDir(),
                getIndexBaseFileName() + "." + getIndexFileExtension()
        );

        // Run the template and merge in the data
        try (final FileOutputStream fileOutStream = new FileOutputStream(indexFile);
             final OutputStreamWriter outWriter = new OutputStreamWriter(fileOutStream)) {
            template.process(rootMap, outWriter);
        } catch (TemplateException e) {
            throw new DocException("Freemarker Template Exception during documentation index creation", e);
        }
    }

    /**
     * @return the name of the index template to be used for this doclet
     */
    @Override
    public String getIndexTemplateName() { return "bash-completion.ftl"; }

    /**
     * @return The base filename for the index file associated with this doclet.
     */
    @Override
    public String getIndexBaseFileName() { return callerScriptName + "-completion"; }

}
