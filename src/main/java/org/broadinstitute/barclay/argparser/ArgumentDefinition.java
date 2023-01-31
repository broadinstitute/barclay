package org.broadinstitute.barclay.argparser;

import org.broadinstitute.barclay.utils.Utils;

import java.io.PrintStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for positional and named argument definitions.
 */
public abstract class ArgumentDefinition {
    protected static final String OPTION_DOC_PREFIX = "Possible values: {";
    protected static final String OPTION_DOC_SUFFIX = "} ";

    private final Field underlyingField;
    private final Object containingObject;
    private final Class<?> underlyingFieldClass;
    private final boolean isCollection;
    private final DeprecatedFeature deprecatedAnnotation;

    // Original values provided by the user for this argument, to be used when displaying this argument as a
    // command line string representation. This is used instead of post-expansion values, which may be a large list.
    private List<String> originalCommandLineValues;

    /**
     * @param containingObject the parent {@code Object} containing the {@code Field} for this argument. cannot be null.
     * @param underlyingField the {@code Field} object for this argument. cannot be null.
     */
    public ArgumentDefinition(final Object containingObject, final Field underlyingField) {
        Utils.nonNull(underlyingField, "An underlying field must be provided");
        Utils.nonNull(containingObject, "A containing object must be provided");

        this.underlyingField = underlyingField;
        this.containingObject = containingObject;

        this.underlyingField.setAccessible(true);
        this.underlyingFieldClass = getClassForUnderlyingField();
        this.isCollection = isCollectionField(underlyingField);
        this.deprecatedAnnotation = underlyingField.getAnnotation(DeprecatedFeature.class);

        if (!canBeMadeFromString()) {
            throw new CommandLineException.CommandLineParserInternalException(
                    String.format(
                            "Field for argument '%s' must have a String constructor or be an enum",
                            getUnderlyingField().getName()));
        }
    }

    /**
     * Get the underlying {@code Field} for this argument
     * @return the {@code Field} for this argument
     */
    public Field getUnderlyingField() { return underlyingField; }

    /**
     * Get the parent {@code Object} containing the {@code Field} for this argument.
     * @return the parent {@code Object} containing the {@code Field} for this argument. will not be null.
     */
    public Object getContainingObject() { return containingObject; }

    /**
     * Return the {@code Class} for the type of the underlying {@code Field} for this argument. If the underlying
     * field type is primitive a {@code Class} corresponding to the primitive type will be returned.
     * @return The {@code Class} for this argument.
     */
    public Class<?> getUnderlyingFieldClass() { return underlyingFieldClass; }

    /**
     * @return true if this {@link ArgumentDefinition} is backed by a {@code Collection} and can accept multiple values
     */
    public boolean isCollection() { return isCollection; }

    /**
     * Get the {@code Object} representing the current value of the field for this object.
     * @return A boxed value even if the underlying field is a primitive. will not be null.
     */
    public Object getArgumentValue() {
        try {
            return getUnderlyingField().get(getContainingObject());
        } catch (final IllegalAccessException e) {
            throw new CommandLineException.ShouldNeverReachHereException(
                    "This shouldn't happen since we setAccessible(true).", e);
        }
    }

    /**
     * Set the value of this argument to the objects in {@code preprocessedValues}.
     *
     * @param commandLineArgumentParser the {@code CommandLineArgumentParser} managing this argument, used to resolve
     *                                  tag surrogates
     * @param messageStream output stream where error messages should be written
     * @param preprocessedValues the values to be used to populate the argument. these values may be tag surrogates
     *                           created by the {@link TaggedArgumentParser} that must be resolved using {@link
     *                           TaggedArgumentParser#getTaggedOptionForSurrogate(String)}.
     */
    public abstract void setArgumentValues(
            final CommandLineArgumentParser commandLineArgumentParser,
            final PrintStream messageStream,
            final List<String> preprocessedValues);

    /**
     * Conduct any post-parsing validation required for the values for this argument.
     * @param commandLineArgumentParser the {@code CommandLineArgumentParser} controlling this argument definition
     *                          used for any cross-argument validation such as mutex validation
     */
    public abstract void validateValues(CommandLineArgumentParser commandLineArgumentParser);

    /**
     * the doc string for this argument, if any.
     * @return doc string. can be empty.
     */
    public abstract String getDocString();

    /**
     * return true if this argument definition has the {@code @DeprecatedFeature} annotation.
     * @return true if this argument is deprecated, otherwise false.
     */
    public boolean isDeprecated() { return deprecatedAnnotation != null; }

    /**
     * Get the deprecation detail string.
     * @return a String containing the detail (may be null if the argument is not annotated with the {@code
     * @DeprecatedFeature} annotation or if no deprecation detail was provided).
     */
    public String getDeprecationDetail() { return isDeprecated() ? deprecatedAnnotation.detail() : null; }

    /**
     * A {@code String} representation of this argument and it's value(s) which would be valid if copied and pasted
     * back as a command line argument
     * @return command line representation of this argument and it's values
     */
    public abstract String getCommandLineDisplayString();

    /**
     * Provide an override value to be used when when displaying this field as part of a command line. This is
     * used to record the original value provided by the user when file expansion was used.
     *
     * @param commandLineDisplayValue
     */
    public void setOriginalCommandLineValues(final List<String> commandLineDisplayValue) {
        this.originalCommandLineValues = commandLineDisplayValue;
    }

    /**
     * Return the original command line values specified for this argument by the user, if any. May be null
     * @return original command line values specified for this argument by the user, if any. May be null.
     */
    public List<String> getOriginalCommandLineValues() {
        return this.originalCommandLineValues;
    }

    /**
     * Returns the {@code Class<?>} type that each instance of the argument needs to be converted to when
     * populated with values. In the case of primitive fields it will return the wrapper type so that {@code String}
     * constructors can be found.
     * @return {@code Class<?>} for this field
     */
    protected Class<?> getClassForUnderlyingField() {
        final Field field = getUnderlyingField();
        if (isCollectionField(field)) {
            final ParameterizedType clazz = (ParameterizedType) (field.getGenericType());
            final Type[] genericTypes = clazz.getActualTypeArguments();
            if (genericTypes.length != 1) {
                throw new CommandLineException.CommandLineParserInternalException(
                        String.format("Strange collection type for field %s", field.getName()));
            }

            // If the Collection's parameterized type is itself parameterized (eg., List<Foo<Bar>>),
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

    /**
     * Initialize a collection value for this field. If the collection can't be instantiated directly
     * because its the underlying type is not a concrete type, an attempt assign an ArrayList will be made.
     *
     * @param annotationType the type of annotation used for ths argument, for error reporting purposes
     */
    protected void intializeCollection(final String annotationType) {
        final Field field = getUnderlyingField();
        final Object callerArguments = containingObject;
        try {
            if (field.get(containingObject) == null) {
                field.set(callerArguments, field.getType().getDeclaredConstructor().newInstance());
            }
        } catch (final Exception ex) {
            // If we can't instantiate the collection, try falling back to Note: I assume this catches Exception to
            // handle the case where the type is not instantiable
            try {
                field.set(callerArguments, new ArrayList<>());
            } catch (final IllegalArgumentException e) {
                throw new CommandLineException.CommandLineParserInternalException(
                        String.format(
                                "Collection member %s of type %s must be explicitly initialized. " +
                                        "It cannot be constructed or auto-initialized with ArrayList.",
                                field.getName(),
                                annotationType));
            } catch (final IllegalAccessException e) {
                throw new CommandLineException.ShouldNeverReachHereException(
                        "We should not have reached here because we set accessible to true", e);
            }
        }
    }

    /**
     * Return true if the {@code Field} is derived from {@code Collection}.
     * @param field input field
     * @return true if field is a {@code Collection}
     */
    protected static boolean isCollectionField(final Field field) {
        try {
            field.getType().asSubclass(Collection.class);
            return true;
        } catch (final ClassCastException e) {
            return false;
        }
    }

    /**
     * Attempt to construct a value for this field from a {@code String} value.
     *
     * @param stringValue the value to be used to initialize the value.
     * @param argumentName the name of this argument
     * @return the constructed object
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Object constructFromString( final String stringValue, final String argumentName) {
        final Class clazz = getUnderlyingFieldClass();
        try {
            if (clazz.isEnum()) {
                try {
                    return Enum.valueOf(clazz, stringValue);
                } catch (final IllegalArgumentException e) {
                    throw new CommandLineException.BadArgumentValue(
                            argumentName,
                            stringValue,
                            String.format("'%s' is not a valid value for %s. Allowed values are %s",
                                    stringValue,
                                    clazz.getSimpleName(),
                                    getEnumOptions(clazz)));
                }
            }
            // Need to use getDeclaredConstructor() instead of getConstructor() in case the constructor
            // is non-public. Set it to be accessible if it isn't already.
            final Constructor<?> ctor = clazz.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(stringValue);
        } catch (final NoSuchMethodException e) {
            // Shouldn't happen because we've checked for presence of ctor
            throw new CommandLineException.ShouldNeverReachHereException(
                    String.format("Cannot find string ctor for %s", clazz.getName()), e);
        } catch (final InstantiationException e) {
            throw new CommandLineException.CommandLineParserInternalException(
                    String.format("Abstract class '%s' cannot be used for an argument value type.", clazz.getSimpleName()), e);
        } catch (final IllegalAccessException e) {
            throw new CommandLineException.CommandLineParserInternalException(
                    String.format("String constructor for argument value type '%s' must be public.", clazz.getSimpleName()), e);
        } catch (final InvocationTargetException e) {
            throw new CommandLineException.BadArgumentValue(
                    argumentName,
                    stringValue,
                    String.format("Failure constructing '%s' from the string '%s'.", clazz.getSimpleName(), stringValue));
        }
    }

    /**
     * Returns the list of possible options values for this argument.
     *
     * <p>
     *     Currently this only make sense with {@link Boolean} and {@link Enum}. Any other class
     *     will result in an empty string.
     * </p>
     *
     * @return String representing the list of possible option values, never {@code null}.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    protected String getOptionsAsDisplayString() {
        final Class<?> clazz = getUnderlyingFieldClass();
        if (clazz == Boolean.class) {
            return String.format("%s%s, %s%s", OPTION_DOC_PREFIX, Boolean.TRUE, Boolean.FALSE, OPTION_DOC_SUFFIX);
        } else if (clazz.isEnum()) {
            final Class<? extends Enum> enumClass = (Class<? extends Enum>)clazz;
            return getEnumOptions(enumClass);
        } else {
            return "";
        }
    }

    /**
     * Decorated the provided description with a deprecation notice if this arg is deprecated.
     * @param description
     * @return the provided description annotated with a deprecation notice if this arg is deprecated
     */
    protected String getDeprecatedArgumentNotice(final String description) {
        return isDeprecated() ?
                "This argument is DEPRECATED (" + getDeprecationDetail() + "). " + description :
                description;
    }

    /**
     * The formatted description for this arg, including a deprecation notice if the arg is marked
     * as deprecated.
     * @param rawDescription the raw description for this arg
     * @param argumentColumnWidth the width reserved for the argument descriptions
     * @param descriptionColumnWidth the display column width to use for formatting
     * @return the description for this arg, formatted for display
     */
    protected String getFormattedDescription(
            final String rawDescription,
            final int argumentColumnWidth,
            final int descriptionColumnWidth) {
        final StringBuilder sb = new StringBuilder();
        final String description = getDeprecatedArgumentNotice(rawDescription);
        final String wrappedDescription = Utils.wrapParagraph(description, descriptionColumnWidth);
        final String[] descriptionLines = wrappedDescription.split("\n");
        for (int i = 0; i < descriptionLines.length; ++i) {
            if (i > 0) {
                Utils.printSpaces(sb, argumentColumnWidth);
            }
            sb.append(descriptionLines[i]);
            sb.append("\n");
        }
        return sb.toString();
    }

    // True if clazz is an enum, or if it has a ctor that takes a single String argument.
    private boolean canBeMadeFromString() {
        final Class<?> clazz = getClassForUnderlyingField();
        if (clazz.isEnum()) {
            return true;
        }
        try {
            // Need to use getDeclaredConstructor() instead of getConstructor() in case the constructor
            // is non-public
            clazz.getDeclaredConstructor(String.class);
            return true;
        } catch (final NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Composes the help string on the possible options an {@link Enum} typed argument can take.
     *
     * @param clazz target enum class. Assumed no to be {@code null}.
     * @param <T> enum class type.
     * @throws CommandLineException if {@code &lt;T&gt;} has no constants.
     * @return never {@code null}.
     */
    private static <T extends Enum<T>> String getEnumOptions(final Class<T> clazz) {
        // We assume that clazz is guaranteed to be a Class<? extends Enum>, thus
        // getEnumConstants() won't ever return a null.
        final T[] enumConstants = clazz.getEnumConstants();
        if (enumConstants.length == 0) {
            throw new CommandLineException(String.format("Bad argument enum type '%s' with no options", clazz.getName()));
        }

        if (CommandLineParser.ClpEnum.class.isAssignableFrom(clazz)) {
            return Stream.of(enumConstants)
                    .map(c -> String.format("%s (%s)", c.name(), ((CommandLineParser.ClpEnum) c).getHelpDoc()))
                    .collect(Collectors.joining("\n"));
        } else {
            return Stream.of(enumConstants)
                    .map(T::name)
                    .collect(Collectors.joining(", ", OPTION_DOC_PREFIX, OPTION_DOC_SUFFIX));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArgumentDefinition that = (ArgumentDefinition) o;

        if (!getUnderlyingField().equals(that.getUnderlyingField())) return false;
        return getContainingObject().equals(that.getContainingObject());
    }

    @Override
    public int hashCode() {
        int result = getUnderlyingField().hashCode();
        result = 31 * result + getContainingObject().hashCode();
        return result;
    }
}
