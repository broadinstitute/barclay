/*
 * The MIT License
 *
 * Copyright (c) 2009 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.broadinstitute.barclay.argparser;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Annotation-driven utility for parsing command-line arguments, checking for errors, and producing usage message.
 * <p/>
 * This class supports options of the form KEY=VALUE, plus positional arguments.  Positional arguments must not contain
 * an equal sign lest they be mistaken for a KEY=VALUE pair.
 * <p/>
 * The caller must supply an object that both defines the command line and has the parsed options set into it.
 * For each possible KEY=VALUE option, there must be a public data member annotated with @Argument.  The KEY name is
 * the name of the data member.  An abbreviated name may also be specified with the shortName attribute of @Argument.
 * If the data member is a List<T>, then the option may be specified multiple times.  The type of the data member,
 * or the type of the List element must either have a ctor T(String), or must be an Enum.  List options must
 * be initialized by the caller with some kind of list.  Any other option that is non-null is assumed to have the given
 * value as a default.  If an option has no default value, and does not have the optional attribute of @Argument set,
 * is required.  For List options, minimum and maximum number of elements may be specified in the @Argument annotation.
 * <p/>
 * A single List data member may be annotated with the @PositionalArguments.  This behaves similarly to a Option
 * with List data member: the caller must initialize the data member, the type must be constructable from String, and
 * min and max number of elements may be specified.  If no @PositionalArguments annotation appears in the object,
 * then it is an error for the command line to contain positional arguments.
 */
public class LegacyCommandLineArgumentParser implements CommandLineParser {
    // For formatting option section of usage message.
    private static final int OPTION_COLUMN_WIDTH = 30;
    private static final int DESCRIPTION_COLUMN_WIDTH = 90;

    protected static final String BETA_PREFIX = "\n\n**BETA FEATURE - WORK IN PROGRESS**\n\n";
    protected static final String EXPERIMENTAL_PREFIX = "\n\n**EXPERIMENTAL FEATURE - USE AT YOUR OWN RISK**\n\n";

    private static final Boolean[] TRUE_FALSE_VALUES = {Boolean.TRUE, Boolean.FALSE};

    private static final String[] PACKAGES_WITH_WEB_DOCUMENTATION = {"picard"};

    private static final String defaultUsagePreamble = "Usage: program [options...]\n";
    private static final String defaultUsagePreambleWithPositionalArguments =
            "Usage: program [options...] [positional-arguments...]\n";
    private static final String OPTIONS_FILE = "OPTIONS_FILE";

    /** name, shortName, description for options built in to framework */
    private static final String[][] FRAMEWORK_OPTION_DOC = {
            {"--help", "-h", "Displays options specific to this tool."},
            {"--stdhelp", "-H", "Displays options specific to this tool AND " +
                    "options common to all Picard command line tools."},
            {"--version", null, "Displays program version."}
    };

    private final static Logger logger = LogManager.getLogger();

    /**
     * A typical command line program will call this to get the beginning of the usage message,
     * and then append a description of the program, like this:
     *
     * getStandardUsagePreamble(getClass()) + "Frobnicates the freebozzle."
     */
    @Override
    public String getStandardUsagePreamble(final Class<?> mainClass) {
        final String preamble = "USAGE: " + mainClass.getSimpleName() + " [options]\n\n" +
                (hasWebDocumentation(mainClass) ?
                        "Documentation: http://broadinstitute.github.io/picard/command-line-overview.html#" +
                                mainClass.getSimpleName() + "\n\n"
                        : "");
        if (mainClass.getAnnotation(ExperimentalFeature.class) != null) {
            return EXPERIMENTAL_PREFIX + preamble;
        } else if (mainClass.getAnnotation(BetaFeature.class) != null) {
            return BETA_PREFIX + preamble;
        } else {
            return preamble;
        }
    }

    /**
     * Determines if a class has web documentation based on its package name
     *
     * @param clazz
     * @return true if the class has web documentation, false otherwise
     */
    public boolean hasWebDocumentation(final Class<?>  clazz) {
        for (final String pkg : PACKAGES_WITH_WEB_DOCUMENTATION) {
            if (clazz.getPackage().getName().startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the link to a FAQ
     */
    public String getFaqLink() {
        return "To get help, see http://broadinstitute.github.io/picard/index.html#GettingHelp";
    }

    // This is the object that the caller has provided that contains annotations,
    // and into which the values will be assigned.
    private final Object callerOptions;

    // For child CommandLineParser, this contains the prefix for the option names, which is needed for generating
    // the command line.  For non-nested, this is the empty string.
    private final String prefix;
    // For non-nested, empty string.  For nested, prefix + "."
    private final String prefixDot;

    // null if no @PositionalArguments annotation
    private Field positionalArguments;
    private int minPositionalArguments;
    private int maxPositionalArguments;

    // List of all the data members with @Argument annotation
    private final List<OptionDefinition> optionDefinitions = new ArrayList<>();

    // Maps long name, and short name, if present, to an option definition that is
    // also in the optionDefinitions list.
    private final Map<String, OptionDefinition> optionMap = new HashMap<>();

    // For printing error messages when parsing command line.
    private PrintStream messageStream;

    // In case implementation wants to get at arg for some reason.
    private String[] argv;

    private String programVersion = null;

    // The command line used to launch this program, including non-null default options that
    // weren't explicitly specified. This is used for logging and debugging.
    private String commandLine = "";

    // The associated program properties using the CommandLineProgramProperties annotation
    private final CommandLineProgramProperties programProperties;

    /**
     * Prepare for parsing command line arguments, by validating annotations.
     *
     * @param callerOptions This object contains annotations that define the acceptable command-line options,
     *                      and ultimately will receive the settings when a command line is parsed.
     */
    public LegacyCommandLineArgumentParser(final Object callerOptions) {
        this(callerOptions, "");
    }

    private String getUsagePreamble() {
        String usagePreamble = "";
        if (null != programProperties) {
            usagePreamble += programProperties.summary();
        } else if (positionalArguments == null) {
            usagePreamble += defaultUsagePreamble;
        } else {
            usagePreamble += defaultUsagePreambleWithPositionalArguments;
        }

        if (null != this.programVersion && 0 < this.programVersion.length()) {
            usagePreamble += "Version: " + getVersion() + "\n";
        }
        //checkForNonASCII(usagePreamble, "preamble");

        return usagePreamble;
    }

    /**
     * @param prefix Non-empty for child options object.
     */
    private LegacyCommandLineArgumentParser(final Object callerOptions, final String prefix) {
        this.callerOptions = callerOptions;

        this.prefix = prefix;
        if (prefix.isEmpty()) {
            prefixDot = "";
        } else {
            prefixDot = prefix + ".";
        }

        createArgumentDefinitions(callerOptions);

        if ((this.callerOptions.getClass().getAnnotation(ExperimentalFeature.class) != null) &&
                (this.callerOptions.getClass().getAnnotation(BetaFeature.class) != null)) {
            throw new CommandLineException.CommandLineParserInternalException("Features cannot be both Beta and Experimental");
        }

        this.programProperties = this.callerOptions.getClass().getAnnotation(CommandLineProgramProperties.class);
    }

    private void createArgumentDefinitions(final Object callerArguments) {
        for (final Field field : CommandLineParserUtilities.getAllFields(callerArguments.getClass())) {
            if (field.getAnnotation(Argument.class) != null && field.getAnnotation(ArgumentCollection.class) != null){
                throw new CommandLineException.CommandLineParserInternalException("An Argument cannot be an argument collection: "
                        +field.getName() + " in " + callerArguments.toString() + " is annotated as both.");
            }
            if (field.getAnnotation(PositionalArguments.class) != null) {
                handlePositionalArgumentAnnotation(field);
            }
            if (field.getAnnotation(Argument.class) != null) {
                handleOptionAnnotation(field, callerArguments);
            }
            if (field.getAnnotation(ArgumentCollection.class) != null) {
                try {
                    field.setAccessible(true);
                    createArgumentDefinitions(field.get(callerArguments));
                } catch (final IllegalAccessException e) {
                    throw new CommandLineException.ShouldNeverReachHereException("should never reach here because we setAccessible(true)", e);
                }
            }
        }
    }

    @Override
    public String getVersion() {
        return this.callerOptions.getClass().getPackage().getImplementationVersion();
    }

    /**
     * Print a usage message based on the options object passed to the ctor.
     *
     * @param printCommon True if common args should be included in the usage message.
     * @param printHidden Ignored in legacy.
     * @return Usage string generated by the command line parser.
     */
    @Override
    public String usage(final boolean printCommon, final boolean printHidden) {
        final StringBuilder sb = new StringBuilder();

        if (!printHidden) {
            logger.warn("Hidden arguments are always printed in LegacyCommandLineArgumentParser");
        }

        if (prefix.isEmpty()) {
            final String preamble = htmlUnescape(convertFromHtml(getStandardUsagePreamble(callerOptions.getClass()) + getUsagePreamble()));
            checkForNonASCII(preamble, "Tool description");
            sb.append(Utils.wrapParagraph(preamble,OPTION_COLUMN_WIDTH + DESCRIPTION_COLUMN_WIDTH));
            sb.append("\nVersion: " + getVersion());
            sb.append("\n");
            sb.append("\n\nOptions:\n\n");

            for (final String[] optionDoc : FRAMEWORK_OPTION_DOC) {
                printOptionParamUsage(sb, optionDoc[0], optionDoc[1], null, optionDoc[2]);
            }
        }

        if (!optionDefinitions.isEmpty()) {
            optionDefinitions.stream().filter(optionDefinition -> printCommon || !optionDefinition.isCommon).forEach(optionDefinition -> printOptionUsage(sb, optionDefinition));
        }

        if (printCommon) {
            final Field fileField;
            try {
                //Temp class OPTIONS_FILE
                class OptionFileContainerForUsage { public File optionFileContainer;}
                fileField = OptionFileContainerForUsage.class.getField("optionFileContainer");
            } catch (final NoSuchFieldException e) {
                throw new CommandLineException("Should never happen", e);
            }
            final OptionDefinition optionsFileOptionDefinition =
                    new OptionDefinition(fileField, null, OPTIONS_FILE, "",
                            "File of OPTION_NAME=value pairs.  No positional parameters allowed.  Unlike command-line options, " +
                                    "unrecognized options are ignored.  " + "A single-valued option set in an options file may be overridden " +
                                    "by a subsequent command-line option.  " +
                                    "A line starting with '#' is considered a comment.",
                            false, false, 0, Integer.MAX_VALUE, null, true, new String[0]);
            printOptionUsage(sb, optionsFileOptionDefinition);
        }
        return sb.toString();
    }

    static void checkForNonASCII(String documentationText, String location) {
        if (documentationText.matches("[^\\p{ASCII}]")) {
            throw new AssertionError("Non-ASCII character used in documentation ("+location+"). Only ASCII characters are allowed.");
        }
        //make sure that html-encoded non-ascii characters are found as well
        if ( Pattern.compile(".*&[a-zA-Z]*?;.*",Pattern.MULTILINE).matcher(documentationText).find()) {
            throw new AssertionError("Non-ASCII character used in documentation ("+location+"). Only ASCII characters are allowed.");
        }
    }
    // package local for testing
    static String convertFromHtml(final String textToConvert) {

        //LinkedHashmap since the order matters
        final Map<String, String> regexps = new LinkedHashMap<>();

        regexps.put("< *a *href=[\'\"](.*?)[\'\"] *>(.*?)</ *a *>","$2 ($1)");
        regexps.put("< *a *href=[\'\"](.*?)[\'\"] *>(.*?)< *a */>","$2 ($1)");
        regexps.put("</ *(br|p|table|h[1-4]|pre|hr|li|ul) *>","\n");
        regexps.put("< *(br|p|table|h[1-4]|pre|hr|li|ul) */>","\n");
        regexps.put("< *(p|table|h[1-4]|ul|pre) *>","\n");
        regexps.put("<li>", " - ");
        regexps.put("</th>", "\t");
        regexps.put("<\\w*?>", "");

        return regexps.entrySet().stream().sequential()
                .reduce(textToConvert, (string, entrySet) -> string.replaceAll(entrySet.getKey(), entrySet.getValue()), (a, b) -> b);
    }

    private static final Map<String, String> htmlToText = new LinkedHashMap<String, String>(){
        private static final long serialVersionUID = 1L;
        {
            put("&lt;","<");
            put("&gt;",">");
            put("&ge;",">=");
            put("&le;","<=");

            put("<p>","\n");
        }
    };

    static String htmlUnescape(String str) {
        // May need more here
        return htmlToText.entrySet().stream().sequential()
                .reduce(str, (string, entrySet) -> string.replace(entrySet.getKey(), entrySet.getValue()), (a, b) -> b);
    }

    /**
     * Parse command-line options, and store values in callerOptions object passed to ctor.
     *
     * @param messageStream Where to write error messages.
     * @param args          Command line tokens.
     * @return true if command line is valid.
     */
    @Override
    public boolean parseArguments(final PrintStream messageStream, final String[] args) {
        this.argv = args;
        this.messageStream = messageStream;
        if (prefix.isEmpty()) {
            commandLine = callerOptions.getClass().getSimpleName();
        }
        for (int i = 0; i < args.length; ++i) {
            final String arg = args[i];
            if (arg.equals("-h") || arg.equals("--help")) {
                messageStream.append(usage(false, true));
                return false;
            }
            if (arg.equals("-H") || arg.equals("--stdhelp")) {
                messageStream.append(usage(true, true));
                return false;
            }

            if (arg.equals("--version")) {
                messageStream.println(getVersion());
                return false;
            }

            final String[] pair = arg.split("=", 2);
            if (pair.length == 2) {
                if (pair[1].isEmpty() && i < args.length - 1) {
                    pair[1] = args[++i];
                }
                if (!parseOption(pair[0], pair[1], false)) {
                    messageStream.println();
                    messageStream.append(usage(true, true));
                    return false;
                }
            } else if (!parsePositionalArgument(arg)) {
                messageStream.println();
                messageStream.append(usage(false, true));
                return false;
            }
        }
        if (!checkNumArguments()) {
            messageStream.println();
            messageStream.append(usage(false, true));
            return false;
        }

        return true;
    }

    /**
     * After command line has been parsed, make sure that all required options have values, and that
     * lists with minimum # of elements have sufficient.
     *
     * @return true if valid
     */
    private boolean checkNumArguments() {
        //Also, since we're iterating over all options and args, use this opportunity to recreate the commandLineString
        final StringBuilder commandLineString = new StringBuilder();
        try {
            for (final OptionDefinition optionDefinition : optionDefinitions) {
                final String fullName = prefixDot + optionDefinition.name;
                final StringBuilder mutextOptionNames = new StringBuilder();
                for (final String mutexOption : optionDefinition.mutuallyExclusive) {
                    final OptionDefinition mutextOptionDef = optionMap.get(mutexOption);
                    if (mutextOptionDef != null && mutextOptionDef.hasBeenSet) {
                        mutextOptionNames.append(' ').append(prefixDot).append(mutextOptionDef.name);
                    }
                }
                if (optionDefinition.hasBeenSet && mutextOptionNames.length() > 0) {
                    messageStream.println("ERROR: Option '" + fullName +
                            "' cannot be used in conjunction with option(s)" +
                            mutextOptionNames.toString());
                    return false;
                }
                if (optionDefinition.isCollection) {
                    final Collection<?> c = (Collection<?>) optionDefinition.field.get(optionDefinition.parent);
                    if (c.size() < optionDefinition.minElements) {
                        messageStream.println("ERROR: Option '" + fullName + "' must be specified at least " +
                                optionDefinition.minElements + " times.");
                        return false;
                    }
                } else if (!optionDefinition.optional && !optionDefinition.hasBeenSet &&
                        !optionDefinition.hasBeenSetFromParent && mutextOptionNames.length() == 0) {
                    messageStream.print("ERROR: Option '" + fullName + "' is required");
                    if (optionDefinition.mutuallyExclusive.isEmpty()) {
                        messageStream.println(".");
                    } else {
                        messageStream.println(" unless any of " + optionDefinition.mutuallyExclusive +
                                " are specified.");
                    }
                    return false;
                }
            }

            if (positionalArguments != null) {
                final Collection<?> c = (Collection<?>) positionalArguments.get(callerOptions);
                if (c.size() < minPositionalArguments) {
                    messageStream.println("ERROR: At least " + minPositionalArguments +
                            " positional arguments must be specified.");
                    return false;
                }
                for (final Object posArg : c) {
                    commandLineString.append(' ').append(posArg.toString());
                }
            }
            //first, append args that were explicitly set
            for (final OptionDefinition optionDefinition : optionDefinitions) {
                if (optionDefinition.hasBeenSet) {
                    commandLineString.append(' ').append(prefixDot).append(optionDefinition.name).append('=').append(
                            optionDefinition.field.get(optionDefinition.parent));
                }
            }
            commandLineString.append("   "); //separator to tell the 2 apart
            //next, append args that weren't explicitly set, but have a default value
            for (final OptionDefinition optionDefinition : optionDefinitions) {
                if (!optionDefinition.hasBeenSet && !optionDefinition.defaultValue.equals("null")) {
                    commandLineString.append(' ').append(prefixDot).append(optionDefinition.name).append('=').append(
                            optionDefinition.defaultValue);
                }
            }
            this.commandLine += commandLineString.toString();
            return true;
        } catch (final IllegalAccessException e) {
            // Should never happen because lack of publicness has already been checked.
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean parsePositionalArgument(final String stringValue) {
        if (positionalArguments == null) {
            messageStream.println("ERROR: Invalid argument '" + stringValue + "'.");
            return false;
        }
        final Object value;
        try {
            value = constructFromString(getUnderlyingType(positionalArguments), stringValue);
        } catch (final CommandLineException e) {
            messageStream.println("ERROR: " + e.getMessage());
            return false;
        }
        final Collection c;
        try {
            c = (Collection) positionalArguments.get(callerOptions);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (c.size() >= maxPositionalArguments) {
            messageStream.println("ERROR: No more than " + maxPositionalArguments +
                    " positional arguments may be specified on the command line.");
            return false;
        }
        c.add(value);
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean parseOption(String key, final String stringValue, final boolean optionsFile) {
        key = key.toUpperCase();
        if (key.equals(OPTIONS_FILE)) {
            commandLine += " " + prefix + OPTIONS_FILE + "=" + stringValue;
            return parseOptionsFile(stringValue);
        }

        final OptionDefinition optionDefinition = optionMap.get(key);
        if (optionDefinition == null) {
            if (optionsFile) {
                // Silently ignore unrecognized option from options file
                return true;
            }
            messageStream.println("ERROR: Unrecognized option: " + key);
            return false;
        }

        if (!optionDefinition.isCollection && optionDefinition.hasBeenSet && !optionDefinition.hasBeenSetFromOptionsFile) {
            messageStream.println("ERROR: Option '" + key + "' cannot be specified more than once.");
            return false;
        }
        final Object value;
        try {
            if (stringValue.equals("null")) {
                //"null" is a special value that allows the user to override any default
                //value set for this arg. It can only be used for optional args. When
                //used for a list arg, it will clear the list.
                if (optionDefinition.optional) {
                    value = null;
                } else {
                    messageStream.println("ERROR: non-null value must be provided for '" + key + "'.");
                    return false;
                }
            } else {
                value = constructFromString(getUnderlyingType(optionDefinition.field), stringValue);
            }
        } catch (final CommandLineException e) {
            messageStream.println("ERROR: " + e.getMessage());
            return false;
        }
        try {
            if (optionDefinition.isCollection) {
                final Collection c = (Collection) optionDefinition.field.get(optionDefinition.parent);
                if (value == null) {
                    //user specified this arg=null which is interpreted as empty list
                    c.clear();
                } else if (c.size() >= optionDefinition.maxElements) {
                    messageStream.println("ERROR: Option '" + key + "' cannot be used more than " +
                            optionDefinition.maxElements + " times.");
                    return false;
                } else {
                    c.add(value);
                }
                optionDefinition.hasBeenSet = true;
                optionDefinition.hasBeenSetFromOptionsFile = optionsFile;
            } else {
                optionDefinition.field.set(optionDefinition.parent, value);
                optionDefinition.hasBeenSet = true;
                optionDefinition.hasBeenSetFromOptionsFile = optionsFile;
            }
        } catch (final IllegalAccessException e) {
            // Should never happen because we only iterate through public fields.
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * Parsing of options from file is looser than normal.  Any unrecognized options are
     * ignored, and a single-valued option that is set in a file may be overridden by a
     * subsequent appearance of that option.
     * A line that starts with '#' is ignored.
     *
     * @param optionsFile
     * @return false if a fatal error occurred
     */
    private boolean parseOptionsFile(final String optionsFile) {
        return parseOptionsFile(optionsFile, true);
    }

    /**
     * @param optionFileStyleValidation true: unrecognized options are silently ignored; and a single-valued option may be overridden.
     *                                  false: standard rules as if the options in the file were on the command line directly.
     * @return
     */
    public boolean parseOptionsFile(final String optionsFile, final boolean optionFileStyleValidation) {
        try (final BufferedReader reader = new BufferedReader(new FileReader(optionsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                final String[] pair = line.split("=", 2);
                if (pair.length == 2) {
                    if (!parseOption(pair[0], pair[1], optionFileStyleValidation)) {
                        messageStream.println();
                        messageStream.append(usage(true, true));
                        return false;
                    }
                } else {
                    messageStream.println("Strange line in OPTIONS_FILE " + optionsFile + ": " + line);
                    messageStream.append(usage(true, true));
                    return false;
                }
            }
            return true;

        } catch (final IOException e) {
            throw new CommandLineException("I/O error loading OPTIONS_FILE=" + optionsFile, e);
        }
    }

    private void printHtmlOptionUsage(final PrintStream stream, final OptionDefinition optionDefinition) {
        final String type = getUnderlyingType(optionDefinition.field).getSimpleName();
        final String optionLabel = prefixDot + optionDefinition.name + " (" + type + ")";
        stream.println("<tr><td>" + optionLabel + "</td><td>" + makeOptionDescription(optionDefinition) + "</td></tr>");
    }

    private void printOptionUsage(final StringBuilder sb, final OptionDefinition optionDefinition) {
        printOptionParamUsage(sb, optionDefinition.name, optionDefinition.shortName,
                getUnderlyingType(optionDefinition.field).getSimpleName(),
                makeOptionDescription(optionDefinition));
    }


    private void printOptionParamUsage(final StringBuilder sb, final String name, final String shortName,
                                       final String type, final String optionDescription) {
        String optionLabel = prefixDot + name;
        if (type != null) optionLabel += "=" + type;

        sb.append(optionLabel);
        if (shortName != null && !shortName.isEmpty()) {
            sb.append("\n");
            optionLabel = prefixDot + shortName;
            if (type != null) optionLabel += "=" + type;
            sb.append(optionLabel);
        }

        int numSpaces = OPTION_COLUMN_WIDTH - optionLabel.length();
        if (optionLabel.length() > OPTION_COLUMN_WIDTH) {
            sb.append("\n");
            numSpaces = OPTION_COLUMN_WIDTH;
        }
        printSpaces(sb, numSpaces);
        checkForNonASCII(optionDescription, name);
        final String wrappedDescription = Utils.wrapParagraph(convertFromHtml(optionDescription), DESCRIPTION_COLUMN_WIDTH);
        final String[] descriptionLines = wrappedDescription.split("\n");
        for (int i = 0; i < descriptionLines.length; ++i) {
            if (i > 0) {
                printSpaces(sb, OPTION_COLUMN_WIDTH);
            }
            sb.append(descriptionLines[i]);
            sb.append("\n");
        }
        sb.append("\n");
    }

    private String makeOptionDescription(final OptionDefinition optionDefinition) {
        final StringBuilder sb = new StringBuilder();
        if (!optionDefinition.doc.isEmpty()) {
            sb.append(optionDefinition.doc);
            sb.append("  ");
        }
        if (optionDefinition.optional) {
            sb.append("Default value: ");
            sb.append(optionDefinition.defaultValue);
            sb.append(". ");
            if (!optionDefinition.defaultValue.equals("null")) {
                sb.append("This option can be set to 'null' to clear the default value. ");
            }
        } else if (!optionDefinition.isCollection) {
            sb.append("Required. ");
        }
        Object[] enumConstants = getUnderlyingType(optionDefinition.field).getEnumConstants();
        if (enumConstants == null && getUnderlyingType(optionDefinition.field) == Boolean.class) {
            enumConstants = TRUE_FALSE_VALUES;
        }

        if (enumConstants != null) {
            final Boolean isClpEnum = enumConstants.length > 0 && (enumConstants[0] instanceof ClpEnum);

            sb.append("Possible values: {");
            if (isClpEnum) sb.append('\n');

            for (int i = 0; i < enumConstants.length; ++i) {
                if (i > 0 && !isClpEnum) {
                    sb.append(", ");
                }
                sb.append(enumConstants[i].toString());

                if (isClpEnum) {
                    sb.append(" (").append(((ClpEnum) enumConstants[i]).getHelpDoc()).append(")\n");
                }
            }
            sb.append("} ");
        }
        if (optionDefinition.isCollection) {
            if (optionDefinition.minElements == 0) {
                if (optionDefinition.maxElements == Integer.MAX_VALUE) {
                    sb.append("This option may be specified 0 or more times. ");
                } else {
                    sb.append("This option must be specified no more than ").append(optionDefinition.maxElements).append(
                            " times. ");
                }
            } else if (optionDefinition.maxElements == Integer.MAX_VALUE) {
                sb.append("This option must be specified at least ").append(optionDefinition.minElements).append(" times. ");
            } else {
                sb.append("This option may be specified between ").append(optionDefinition.minElements).append(
                        " and ").append(optionDefinition.maxElements).append(" times. ");
            }

            if (!optionDefinition.defaultValue.equals("null")) {
                sb.append("This option can be set to 'null' to clear the default list. ");
            }

        }
        if (!optionDefinition.mutuallyExclusive.isEmpty()) {
            sb.append(" Cannot be used in conjuction with option(s)");
            for (final String option : optionDefinition.mutuallyExclusive) {
                final OptionDefinition mutextOptionDefinition = optionMap.get(option);

                if (mutextOptionDefinition == null) {
                    throw new CommandLineException("Invalid option definition in source code.  " + option +
                            " doesn't match any known option.");
                }

                sb.append(' ').append(mutextOptionDefinition.name);
                if (!mutextOptionDefinition.shortName.isEmpty()) {
                    sb.append(" (").append(mutextOptionDefinition.shortName).append(')');
                }
            }
        }
        return sb.toString();
    }

    private void printSpaces(final StringBuilder sb, final int numSpaces) {
        for (int i = 0; i < numSpaces; ++i) {
            sb.append(' ');
        }
    }

    /**
     * @param field the command line parameter as a {@link Field}
     */
    private void handleOptionAnnotation(final Field field, Object parent) {
        try {
            field.setAccessible(true);
            final Argument optionAnnotation = field.getAnnotation(Argument.class);
            final boolean isCollection = isCollectionField(field);
            if (isCollection) {
                if (optionAnnotation.maxElements() == 0) {
                    throw new CommandLineException.CommandLineParserInternalException("@Argument member " + field.getName() +
                            "has maxElements = 0");
                }
                if (optionAnnotation.minElements() > optionAnnotation.maxElements()) {
                    throw new CommandLineException.CommandLineParserInternalException("In @Argument member " + field.getName() +
                            ", minElements cannot be > maxElements");
                }
                if (field.get(parent) == null) {
                    createCollection(field, parent, "@Argument");
                }
            }
            if (!canBeMadeFromString(getUnderlyingType(field))) {
                throw new CommandLineException.CommandLineParserInternalException("@Argument member " + field.getName() +
                        " must have a String ctor or be an enum");
            }

            final OptionDefinition optionDefinition = new OptionDefinition(field,
                    parent,
                    field.getName(),
                    optionAnnotation.shortName(),
                    optionAnnotation.doc(), optionAnnotation.optional() || (field.get(parent) != null),
                    isCollection, optionAnnotation.minElements(),
                    optionAnnotation.maxElements(), field.get(parent), optionAnnotation.common(),
                    optionAnnotation.mutex());

            // log a warning if boundaries are set
            if (optionAnnotation.maxValue() != Double.POSITIVE_INFINITY) {
                logger.warn("Maximum allowed value for argument --{} is not enforced", optionDefinition.name);
            }
            if (optionAnnotation.minValue() != Double.NEGATIVE_INFINITY) {
                logger.warn("Minimum allowed value for argument --{} is not enforced", optionDefinition.name);
            }
            if (optionAnnotation.maxRecommendedValue() != Double.POSITIVE_INFINITY) {
                logger.warn("Maximum recommended value for argument --{} is not checked", optionDefinition.name);
            }
            if (optionAnnotation.minRecommendedValue() != Double.NEGATIVE_INFINITY) {
                logger.warn("Minimum recommended value for argument --{} is not checked", optionDefinition.name);
            }

            for (final String option : optionAnnotation.mutex()) {
                final OptionDefinition mutextOptionDef = optionMap.get(option);
                if (mutextOptionDef != null) {
                    mutextOptionDef.mutuallyExclusive.add(field.getName());
                }
            }
            if (optionMap.containsKey(optionDefinition.name)) {
                throw new CommandLineException.CommandLineParserInternalException(optionDefinition.name + " has already been used.");
            }
            if (!optionDefinition.shortName.isEmpty() && !optionDefinition.shortName.equals(optionDefinition.name)) {
                if (optionMap.containsKey(optionDefinition.shortName)) {
                        throw new CommandLineException.CommandLineParserInternalException(optionDefinition.shortName +
                                " has already been used");
                } else {
                    optionMap.put(optionDefinition.shortName, optionDefinition);
                }
            }
            if (!optionMap.containsKey(optionDefinition.name)) {
                optionMap.put(optionDefinition.name, optionDefinition);
            }
            optionDefinitions.add(optionDefinition);
        } catch (final IllegalAccessException e) {
            throw new CommandLineException.CommandLineParserInternalException(field.getName() +
                    " must have public visibility to have @Argument annotation");
        }
    }

    private void handlePositionalArgumentAnnotation(final Field field) {
        if (positionalArguments != null) {
            throw new CommandLineException.CommandLineParserInternalException
                    ("@PositionalArguments cannot be used more than once in an option class.");
        }
        field.setAccessible(true);
        positionalArguments = field;
        if (!isCollectionField(field)) {
            throw new CommandLineException.CommandLineParserInternalException("@PositionalArguments must be applied to a Collection");
        }

        if (!canBeMadeFromString(getUnderlyingType(field))) {
            throw new CommandLineException.CommandLineParserInternalException("@PositionalParameters member " + field.getName() +
                    "does not have a String ctor");
        }

        final PositionalArguments positionalArgumentsAnnotation = field.getAnnotation(PositionalArguments.class);
        minPositionalArguments = positionalArgumentsAnnotation.minElements();
        maxPositionalArguments = positionalArgumentsAnnotation.maxElements();
        if (minPositionalArguments > maxPositionalArguments) {
            throw new CommandLineException.CommandLineParserInternalException("In @PositionalArguments, minElements cannot be > maxElements");
        }
        try {
            if (field.get(callerOptions) == null) {
                createCollection(field, callerOptions, "@PositionalParameters");
            }
        } catch (final IllegalAccessException e) {
            throw new CommandLineException.CommandLineParserInternalException(field.getName() +
                    " must have public visibility to have @PositionalParameters annotation");

        }
    }

    private boolean isCollectionField(final Field field) {
        try {
            field.getType().asSubclass(Collection.class);
            return true;
        } catch (final ClassCastException e) {
            return false;
        }
    }

    private void createCollection(final Field field, final Object callerOptions, final String annotationType)
            throws IllegalAccessException {
        try {
            field.set(callerOptions, field.getType().newInstance());
        } catch (final Exception ex) {
            try {
                field.set(callerOptions, new ArrayList<>());
            } catch (final IllegalArgumentException e) {
                throw new CommandLineException.CommandLineParserInternalException("In collection " + annotationType +
                        " member " + field.getName() +
                        " cannot be constructed or auto-initialized with ArrayList, so collection must be initialized explicitly.");
            }

        }

    }

    /**
     * Returns the type that each instance of the argument needs to be converted to. In
     * the case of primitive fields it will return the wrapper type so that String
     * constructors can be found.
     */
    private Class<?> getUnderlyingType(final Field field) {
        if (isCollectionField(field)) {
            final ParameterizedType clazz = (ParameterizedType) (field.getGenericType());
            final Type[] genericTypes = clazz.getActualTypeArguments();
            if (genericTypes.length != 1) {
                throw new CommandLineException.CommandLineParserInternalException("Strange collection type for field " +
                        field.getName());
            }
            return (Class) genericTypes[0];

        } else {
            final Class<?> type = field.getType();
            if (type == Byte.TYPE) return Byte.class;
            if (type == Short.TYPE) return Short.class;
            if (type == Integer.TYPE) return Integer.class;
            if (type == Long.TYPE) return Long.class;
            if (type == Float.TYPE) return Float.class;
            if (type == Double.TYPE) return Double.class;
            if (type == Boolean.TYPE) return Boolean.class;

            return type;
        }
    }

    // True if clazz is an enum, or if it has a ctor that takes a single String argument.
    private boolean canBeMadeFromString(final Class<?> clazz) {
        if (clazz.isEnum()) {
            return true;
        }
        try {
            clazz.getConstructor(String.class);
            return true;
        } catch (final NoSuchMethodException e) {
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object constructFromString(final Class clazz, final String s) {
        try {
            if (clazz.isEnum()) {
                try {
                    return Enum.valueOf(clazz, s);
                } catch (final IllegalArgumentException e) {
                    throw new CommandLineException("'" + s + "' is not a valid value for " +
                            clazz.getSimpleName() + ".", e);
                }
            }
            final Constructor ctor = clazz.getConstructor(String.class);
            return ctor.newInstance(s);
        } catch (final NoSuchMethodException e) {
            // Shouldn't happen because we've checked for presence of ctor
            throw new CommandLineException("Cannot find string ctor for " + clazz.getName(), e);
        } catch (final InstantiationException e) {
            throw new CommandLineException("Abstract class '" + clazz.getSimpleName() +
                    "'cannot be used for an option value type.", e);
        } catch (final IllegalAccessException e) {
            throw new CommandLineException("String constructor for option value type '" + clazz.getSimpleName() +
                    "' must be public.", e);
        } catch (final InvocationTargetException e) {
            throw new CommandLineException("Problem constructing " + clazz.getSimpleName() +
                    " from the string '" + s + "'.", e.getCause());
        }
    }

    public String[] getArgv() {
        return argv;
    }

    protected static final class OptionDefinition {
        final Field field;
        final Object parent;
        final String name;
        final String shortName;
        final String doc;
        final boolean optional;
        final boolean isCollection;
        final int minElements;
        final int maxElements;
        final String defaultValue;
        final boolean isCommon;
        boolean hasBeenSet = false;
        boolean hasBeenSetFromOptionsFile = false;
        boolean hasBeenSetFromParent = false;
        final Set<String> mutuallyExclusive;

        private OptionDefinition(final Field field, final Object parent,final String name, final String shortName, final String doc,
                                 final boolean optional, boolean collection, final int minElements,
                                 final int maxElements, final Object defaultValue, final boolean isCommon,
                                 final String[] mutuallyExclusive) {
            this.field = field;
            this.parent = parent;
            this.name = name.toUpperCase();
            this.shortName = shortName.toUpperCase();
            this.doc = doc;
            this.optional = optional;
            isCollection = collection;
            this.minElements = minElements;
            this.maxElements = maxElements;
            if (defaultValue != null) {
                if (isCollection && ((Collection) defaultValue).isEmpty()) {
                    //treat empty collections the same as uninitialized primitive types
                    this.defaultValue = "null";
                } else {
                    //this is an intialized primitive type or a non-empty collection
                    this.defaultValue = defaultValue.toString();
                }
            } else {
                this.defaultValue = "null";
            }
            this.isCommon = isCommon;
            this.mutuallyExclusive = new HashSet<String>(Arrays.asList(mutuallyExclusive));
            if (this.field.getAnnotation(Hidden.class) != null) {
                logger.warn("Hidden annotation is not honored for --{}", this.name);
            }
        }
    }

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
    public String getCommandLine() { return commandLine; }

    /**
     * This method is only needed when calling one of the public methods that doesn't take a messageStream argument.
     */
    public void setMessageStream(final PrintStream messageStream) {
        this.messageStream = messageStream;
    }

    public Object getCallerOptions() {
        return callerOptions;
    }

    @Override
    public <T> List<Pair<ArgumentDefinition, T>> gatherArgumentValuesOfType(final Class<T> type ) {
        throw new RuntimeException("Not implemented");
    }
}
