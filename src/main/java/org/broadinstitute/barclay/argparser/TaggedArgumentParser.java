package org.broadinstitute.barclay.argparser;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.barclay.utils.Utils;

import java.util.*;

/**
 * The parser used by the {@link CommandLineArgumentParser} for handling tagged argument strings. Tagged arguments
 * can optionally contain a logical name, and optional attribute/value pairs. They are are of the form:
 *
 *     --argument_name:logical_name argument_value
 * or:
 *     --argument_name:logical_name,key1=value1,key2=value2 argument_value
 *
 * The logical name is optional, but is required if key/value pairs are included. @Argument annotated fields that
 * require tagged values must implement {@link TaggedArgument}.
 *
 * The parsing occurs in two phases:
 *
 * 1) Phase 1 occurs before the option parser is presented with the arguments. In phase 1, the option name is
 * peeled off from the raw option string (which includes that tag string), and the resulting tag string and accompanying
 * raw argument value are stored as a pair in a hash map for later retrieval via a key. The key is a string that is
 * constructed by concatenating the original option name (excluding the actual short/long hyphen prefix), the
 * tag string, and the raw argument value together to ensure the key is unique:
 *
 *      Key: --argument_name:logical_name,key1=value1,key2=value2:argument_value
 *
 * The pair object stored in the map contains only the (previously peeled off) tag string, and the raw argument value that
 * was provided by the user. The command line parser replaces the original arguments provided by the user with the option
 * name and the key:
 *
 *          User provides:              "--argument_name:logical_name,key1=value1,key2=value2" "raw_argument_value"
 *          Parser is presented with:   "--argument_name" "argument_name:logical_name,key1=value1,key2=value2:argument_value"
 *          Map stores:                 Pair("logical_name,key1=value1,key2=value2", "argument_value")
 *
 * 2) In phase 2, which occurs when the underlying argument field is being populated with a value, the key is used to
 * retrieve the original tag string and argument value. The tag string is parsed (logical name and attributes) and used
 * to populate the underlying argument field.
 */
public final class TaggedArgumentParser {

    /**
     * Delimiter between key-value pairs in the "logical_name,key1=value1,key2=value2" syntax.
     */
    private static final String ARGUMENT_KEY_VALUE_PAIR_DELIMITER = ",";

    /**
     * Separator between keys and values in the "logical_name,key1=value1,key2=value2" syntax.
     */
    private static final String ARGUMENT_KEY_VALUE_SEPARATOR = "=";

    /**
     * Separator used between option name and logical name.
     */
    private static final char ARGUMENT_TAG_NAME_SEPARATOR = ':';

    private static final String USAGE = "Tagged arguments must be of the form argument_name or argument_name:logical_name(,key=value)*";

    // Map of surrogate keys to Pair(tag_string, raw_argument_value)
    private Map<String, Pair<String, String>> tagSurrogates = new HashMap<>();

    /**
     * Given an array of raw arguments provided by the user, return an array of args where tagged arguments
     * have been replaced with curated arguments containing a key to be used by the parser to retrieve the actual
     * values.
     * @param argArray raw arguments as provided by the user
     * @return curated string of arguments to be presented to the opt parser
     */
    public String[] preprocessTaggedOptions(final String[] argArray) {
        List<String> finalArgs = new ArrayList<>(argArray.length);

        Iterator<String> argIt = Arrays.asList(argArray).iterator();
        while (argIt.hasNext()) {
            final String arg = argIt.next();
            if (isShortOptionToken(arg)) {
                replaceTaggedOption(CommandLineArgumentParser.SHORT_OPTION_PREFIX, arg.substring(CommandLineArgumentParser.SHORT_OPTION_PREFIX.length()), argIt, finalArgs);
            } else if (isLongOptionToken(arg)) {
                replaceTaggedOption(CommandLineArgumentParser.LONG_OPTION_PREFIX, arg.substring(CommandLineArgumentParser.LONG_OPTION_PREFIX.length()), argIt, finalArgs);
            } else { // Positional arg, etc., just add it
                finalArgs.add(arg);
            }
        }
        return finalArgs.toArray(new String[finalArgs.size()]);
    }

    /**
     * Reset the cached tag surrogate map. The command line parser needs to do two passes on input arguments in
     * order to determine if some special arguments have been included (ie, an arguments file). This is used to
     * clear the tag surrogates generated in the first pass before a second pass is started, when they will be
     * recreated.
     */
    public void resetTagSurrogates() { tagSurrogates.clear(); }

    /**
     * Process a single option and add it to the curated args list. If the option is tagged, add the
     * curated key and value. Otherwise just add the raw option.
     *
     * @param optionPrefix the actual option prefix used for this option, either "-" or "--"
     * @param optionString the option string including everything but the prefix
     * @param userArgIt iterator of raw arguments provided by the user, used to retrieve the value corresponding
     *                    to this option
     * @param finalArgList the curated argument list
     */
    private void replaceTaggedOption(
            final String optionPrefix,          // the option prefix used (short or long)
            final String optionString,          // the option string, minus prefix, including any tags/attributes
            final Iterator<String> userArgIt,   // the original, raw, un-curated input argument list
            final List<String> finalArgList     // final, curated argument list
    )
    {
        final int separatorIndex = optionString.indexOf(TaggedArgumentParser.ARGUMENT_TAG_NAME_SEPARATOR);
        if (separatorIndex == -1) { // no tags, consume one argument and get out
            detectAndRejectHybridSyntax(optionString);
            finalArgList.add(optionPrefix + optionString);
        } else {
            final String optionName = optionString.substring(0, separatorIndex);
            detectAndRejectHybridSyntax(optionName);
            if (userArgIt.hasNext()) {
                if (optionName.isEmpty()) {
                    throw new CommandLineException("Zero length argument name found in tagged argument: " + optionString);
                }
                final String tagNameAndValues = optionString.substring(separatorIndex+1, optionString.length());
                if (tagNameAndValues.isEmpty()) {
                    throw new CommandLineException("Zero length tag name found in tagged argument: " + optionString);
                }
                final String argValue = userArgIt.next();
                if (isLongOptionToken(argValue) || isShortOptionToken(argValue)) {
                    // An argument value is required, and there isn't one to consume
                    throw new CommandLineException("No argument value found for tagged argument: " + optionString);
                }

                // Replace the original prefix/option/attribute string with the original prefix/option name, and
                // replace it's companion argument value with the surrogate key to be used later to retrieve the
                // actual values
                final String pairKey = getSurrogateKeyForTaggedOption(optionString, argValue, tagNameAndValues);
                finalArgList.add(optionPrefix + optionName);
                finalArgList.add(pairKey);
            } else {
                // the option appears to be tagged, but we're already at the end of the argument list,
                // and there is no companion value to use
                throw new CommandLineException("No argument value found for tagged argument: " + optionString);
            }
        }
    }

    /**
     * Reject attempts to use hybrid Barclay/legacy syntax that contains embedded "=". Most of the time
     * this works because jopt accepts "-O=value". But if "value" contains what appears to be tagging
     * syntax (ie., an embedded ":"), the tag parser will fail and give misleading error messages. So instead of
     * allowing it some cases and having strange failures in others, require users to always use correct
     * (Barclay style) syntax.
     * @param optionName name of the option being inspected
     */
    private void detectAndRejectHybridSyntax(final String optionName) {
        if (optionName.contains(ARGUMENT_KEY_VALUE_SEPARATOR)) {
            throw new CommandLineException(String.format("Can't parse option name containing an embedded '=' (%s)", optionName));
        }
    }

    /**
     * Attempt to retrieve an option pair from the map using a surrogate key.
     * @param putativeSurrogateKey putative key to try to retrieve from the surrogate map
     * @return tagged option pair for this surrogate, or null if no entry
     */
    public Pair<String, String> getTaggedOptionForSurrogate(final String putativeSurrogateKey) {
        return tagSurrogates.get(putativeSurrogateKey);
    }

    // See if the opt parser would think this is a short option ("-")
    private static boolean isShortOptionToken(final String argument) {
        return argument.startsWith( CommandLineArgumentParser.SHORT_OPTION_PREFIX )
                && !CommandLineArgumentParser.SHORT_OPTION_PREFIX.equals( argument )
                && !isLongOptionToken( argument );
    }

    // See if the opt parser would think this is a long option ("--")
    private static boolean isLongOptionToken(final String argument) {
        return argument.startsWith( CommandLineArgumentParser.LONG_OPTION_PREFIX );
    }

    // Stores the option string and value in the tagSurrogates hash map and returns a surrogate key.
    private String getSurrogateKeyForTaggedOption(
            final String rawOptionString,   // the raw option string provided by the user, including the prefix and option name
            final String rawArgumentValue,  // the raw argument value provided by the user
            final String tagString          // the tag string that has been peeled off of the raw option string, including logical name and any attributes
    )
    {
        final String surrogateKey = makeSurrogateKey(rawOptionString, rawArgumentValue);
        if (null != tagSurrogates.put(surrogateKey, Pair.of(tagString, rawArgumentValue))) {
            throw new CommandLineException.BadArgumentValue(
                    String.format("The argument value: \"%s %s\" was duplicated on the command line", rawOptionString, rawArgumentValue));
        }
        return surrogateKey;
    }

    /**
     * Construct a surrogate key from the option name/tag string (as provided by the user) and raw argument value (as
     * provided by the user). In order to ensure uniqueness of the key, it needs to include all elements from the
     * command line, including:
     *
     *  -the option name (short/long) used
     *  -the tag string used
     *  -the argument value used
     *
     * @param rawOptionString the raw option string as provided by the user, without the option prefix
     * @param rawArgumentValue the entire raw argument value provided by the user
     * @return a surrogate key that can be included as a substitute value in the command line args presented to
     * command line the parser
     */
    private String makeSurrogateKey(
            final String rawOptionString,
            final String rawArgumentValue
    ) {
        // The keys strings are never parsed, but the separator is added to the
        // middle to make visual key inspection easier
        return rawOptionString + ARGUMENT_TAG_NAME_SEPARATOR + rawArgumentValue;
    }

    /**
     * Parse a tag string and populate a TaggedArgument with values.
     *
     * @param taggedArg TaggedArgument to receive tags
     * @param longArgName name of the argument being tagged
     * @param tagString tag string (including logical name and attributes, no option name)
     */
    public static void populateArgumentTags(final TaggedArgument taggedArg, final String longArgName, final String tagString) {
        if (tagString == null) {
            taggedArg.setTag(null);
            taggedArg.setTagAttributes(Collections.emptyMap());
        } else {
            final ParsedArgument pa = ParsedArgument.of(longArgName, tagString);
            taggedArg.setTag(pa.getName());
            taggedArg.setTagAttributes(pa.keyValueMap());
        }
    }

    /**
     * Given a TaggedArgument implementation and a long argument name, return a string representation of argument,
     * including the tag and attributes, for display purposes.
     *
     * @param taggedArg implementation of TaggedArgument interface
     * @return a display string representing the tag name and attributes. May be empty if no tag name/attributes are present.
     */
    public static String getDisplayString(final String longArgName, final TaggedArgument taggedArg) {
        Utils.nonNull(longArgName);
        Utils.nonNull(taggedArg);

        StringBuilder sb = new StringBuilder();
        sb.append(longArgName);
        if (taggedArg.getTag() != null) {
            sb.append(ARGUMENT_TAG_NAME_SEPARATOR);
            sb.append(taggedArg.getTag());

            if (taggedArg.getTagAttributes() != null) {
                taggedArg.getTagAttributes().entrySet().stream()
                        .forEach(
                                entry -> {
                                    sb.append(ARGUMENT_KEY_VALUE_PAIR_DELIMITER);
                                    sb.append(entry.getKey().toString());
                                    sb.append(ARGUMENT_KEY_VALUE_SEPARATOR);
                                    sb.append(entry.getValue().toString());
                                });
            }
        }

        return sb.toString();
    }

    /**
     * Represents a parsed, tagged argument.
     *
     * May have attributes.
     */
    private static final class ParsedArgument{
        private final String name;
        private final Map<String, String> keyValueMap;

        /**
         * Parses an argument value String of the forms:
         *
         * "logical_name(,key=value)*"
         *
         * into logical name and key=value pairs.
         *
         * @param rawTagValue tag string value from the command line (does not include the argument name) to parse
         * @return The argument parsed from the provided string.
         */
        public static ParsedArgument of(final String longArgName, final String rawTagValue) {
            final String[] tokens = rawTagValue.split(ARGUMENT_KEY_VALUE_PAIR_DELIMITER, -1);

            if (tokens.length == 0) {
                throw new CommandLineException.BadArgumentValue(longArgName, rawTagValue, USAGE);
            }
            // first token is required to be a name
            if (tokens[0].contains(ARGUMENT_KEY_VALUE_SEPARATOR)) {
                throw new CommandLineException.BadArgumentValue("Missing tag name for argument: " + rawTagValue);
            }
            if ( Arrays.stream(tokens).anyMatch(String::isEmpty)) {
                throw new CommandLineException.BadArgumentValue(longArgName, rawTagValue, "Empty tag or attribute encountered. " + USAGE);
            }

            final ParsedArgument pa = new ParsedArgument(tokens[0]);
            if (tokens.length == 1) {
                return pa;
            } else {
                // User specified a logical name (and optional list of key-value pairs)
                for (int i = 1; i < tokens.length; i++){
                    final String[] kv = tokens[i].split(ARGUMENT_KEY_VALUE_SEPARATOR, -1);
                    if (kv.length != 2 || kv[0].isEmpty() || kv[1].isEmpty()){
                        throw new CommandLineException.BadArgumentValue("", rawTagValue, USAGE);
                    }
                    if (pa.containsKey(kv[0])){
                        throw new CommandLineException.BadArgumentValue("", rawTagValue, "Duplicate key " + kv[0] + "\n" + USAGE);
                    }
                    pa.addKeyValue(kv[0], kv[1]);
                }
                return pa;
            }
        }

        private ParsedArgument(final String name) {
            this.name=name;
            this.keyValueMap = new LinkedHashMap<>(2);
        }

        public String getName() {
            return name;
        }

        /**
         * Returns an immutable view of the key-value map.
         */
        public Map<String, String> keyValueMap() {
            return Collections.unmodifiableMap(keyValueMap);
        }

        public void addKeyValue(final String k, final String v) {
            keyValueMap.put(k, v);
        }

        private boolean containsKey(final String k) {
            return keyValueMap.containsKey(k);
        }
    }

}
