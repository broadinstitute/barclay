package org.broadinstitute.barclay.argparser;

import org.apache.commons.lang3.tuple.Pair;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;

/**
 * Interface for command line argument parsers.
 */
public interface CommandLineParser {

    /**
     * Parse command-line arguments in an object passed to the implementing class ctor.
     *
     * @param messageStream Where to write error messages.
     * @param args          Command line tokens.
     * @return true if command line is valid and the program should run, false if help or version was requested
     * @throws CommandLineException if there is an invalid command line
     */
    public boolean parseArguments(final PrintStream messageStream, final String[] args);

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
    public String getCommandLine();

    /**
     * A typical command line program will call this to get the beginning of the usage message,
     * and then append a description of the program, like this:
     *
     * commandLineParser.getStandardUsagePreamble(getClass()) + "Frobnicates the freebozzle."
     */
    public abstract String getStandardUsagePreamble(final Class<?> mainClass);

    public abstract String getVersion();

    /**
     * Return the plugin instance corresponding to the targetDescriptor class
     */
    public default <T> T getPluginDescriptor(Class<T> targetDescriptor) {
        // Throw unless overridden - the legacy command line parser doesn't implement plugins
        throw new CommandLineException.CommandLineParserInternalException(
                "Command line plugins are not implemented by this command line parser"
        );
    }

    /**
     * Print a usage message based on the arguments object passed to the ctor.
     *
     * @param stream      Where to write the usage message.
     * @param printCommon True if common args should be included in the usage message.
     */
    public abstract void usage(final PrintStream stream, final boolean printCommon);

    /**
     * Interface for @Argument annotated enums that have user documentation.
     */
    public interface ClpEnum {
        String getHelpDoc();
    }

    /**
     * Locates and returns the VALUES of all Argument-annotated fields of a specified type in a given object,
     * pairing each field value with its corresponding Field object.
     *
     * Must be called AFTER argument parsing and value injection into argumentSource is complete (otherwise there
     * will be no values to gather!). As a result, this is implemented as a static utility method into which
     * the fully-initialized tool instance must be passed.
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
     * @param argumentSource Object whose fields to search. Must have already undergone argument parsing and argument value injection.
     * @param <T> Type parameter representing the type to search for and return
     * @return A List of Pairs containing all Argument-annotated field values found of the target type. First element in each Pair
     *         is the Field object itself, and the second element is the actual value of the argument field. The second
     *         element will be null for uninitialized fields.
     */
    public static <T> List<Pair<Field, T>> gatherArgumentValuesOfType( final Class<T> type, final Object argumentSource ) {
        List<Pair<Field, T>> argumentValues = new ArrayList<>();

        // Examine all fields in argumentSource (including superclasses)
        for ( Field field : getAllFields(argumentSource.getClass()) ) {
            field.setAccessible(true);

            try {
                // Consider only fields that have Argument annotations and are either of the target type,
                // subtypes of the target type, or Collections of the target type or one of its subtypes:
                if ( field.getAnnotation(Argument.class) != null && type.isAssignableFrom(getUnderlyingType(field)) ) {

                    if ( isCollectionField(field) ) {
                        // Collection arguments are guaranteed by the parsing system to be non-null (at worst, empty)
                        Collection<?> argumentContainer = (Collection<?>)field.get(argumentSource);

                        // Emit a Pair with an explicit null value for empty Collection arguments
                        if ( argumentContainer.isEmpty() ) {
                            argumentValues.add(Pair.of(field, null));
                        }
                        // Unpack non-empty Collections of the target type into individual values,
                        // each paired with the same Field object.
                        else {
                            for ( Object argumentValue : argumentContainer ) {
                                argumentValues.add(Pair.of(field, type.cast(argumentValue)));
                            }
                        }
                    }
                    else {
                        // Add values for non-Collection arguments of the target type directly
                        argumentValues.add(Pair.of(field, type.cast(field.get(argumentSource))));
                    }
                }
                else if ( field.getAnnotation(ArgumentCollection.class) != null ) {
                    // Recurse into ArgumentCollections for more potential matches.
                    argumentValues.addAll(gatherArgumentValuesOfType(type, field.get(argumentSource)));
                }
            }
            catch ( IllegalAccessException e ) {
                throw new CommandLineException.ShouldNeverReachHereException("field access failed after setAccessible(true)");
            }
        }

        return argumentValues;
    }

    /**
     * Returns the type that each instance of the argument needs to be converted to. In
     * the case of primitive fields it will return the wrapper type so that String
     * constructors can be found.
     */
    static Class<?> getUnderlyingType(final Field field) {
        if (isCollectionField(field)) {
            final ParameterizedType clazz = (ParameterizedType) (field.getGenericType());
            final Type[] genericTypes = clazz.getActualTypeArguments();
            if (genericTypes.length != 1) {
                throw new CommandLineException.CommandLineParserInternalException("Strange collection type for field " +
                        field.getName());
            }

            // If the Collection's parametrized type is itself parametrized (eg., List<Foo<Bar>>),
            // return the raw type of the outer parameter (Foo.class, in this example) to avoid a
            // ClassCastException. Otherwise, return the Collection's type parameter directly as a Class.
            return (Class<?>) (genericTypes[0] instanceof ParameterizedType ?
                    ((ParameterizedType)genericTypes[0]).getRawType() :
                    genericTypes[0]);

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

    static List<Field> getAllFields(Class<?> clazz) {
        final List<Field> ret = new ArrayList<>();
        do {
            ret.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return ret;
    }

    public static boolean isCollectionField(final Field field) {
        try {
            field.getType().asSubclass(Collection.class);
            return true;
        } catch (final ClassCastException e) {
            return false;
        }
    }


}
