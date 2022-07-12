package org.broadinstitute.barclay.argparser;

import org.broadinstitute.barclay.utils.Utils;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Definition for Positional arguments. The type of a Positional argument must be a collection. Positional
 * arguments are always optional.
 */
public class PositionalArgumentDefinition extends ArgumentDefinition {
    final private PositionalArguments positionalArgumentsAnnotation;

    private static final String POSITIONAL_ARGUMENTS_NAME = "Positional Argument";

    /**
     * @param positionalArgumentsAnnotation a {@code PositionalArguments} object. cannot be null.
     * @param containingObject the parent {@code Object} containing this argument. cannot be null.
     * @param argField the {@code Field} for this argument. cannot be null.
     */
    public PositionalArgumentDefinition(
            final PositionalArguments positionalArgumentsAnnotation,
            final Object containingObject,
            final Field argField)
    {
        super(containingObject, argField);

        Utils.nonNull(positionalArgumentsAnnotation);
        Utils.nonNull(containingObject);

        this.positionalArgumentsAnnotation = positionalArgumentsAnnotation;
        if (!isCollectionField(argField)) {
            throw new CommandLineException.CommandLineParserInternalException("@PositionalArguments must be applied to a Collection");
        }
        if (positionalArgumentsAnnotation.minElements() > positionalArgumentsAnnotation.maxElements()) {
            throw new CommandLineException.CommandLineParserInternalException("In @PositionalArguments, minElements cannot be > maxElements");
        }

        intializeCollection("@PositionalArguments");
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getCommandLineDisplayString() {
        // Note: this used to be typed as "List<Object>", but that's too narrow - these are required to
        // be a Collection, not a List
        final Collection<?> positionalArgs;
        try {
            positionalArgs = getOriginalCommandLineValues() == null ?
                    (Collection<Object>) getUnderlyingField().get(getContainingObject()) :
                    getOriginalCommandLineValues();
        } catch (IllegalAccessException e) {
            throw new CommandLineException.ShouldNeverReachHereException("setAccessible(true) was called", e);
        }
        return positionalArgs.stream().map(Object::toString).collect(Collectors.joining(" "));
    }

    public PositionalArguments getPositionalArgumentsAnnotation() { return positionalArgumentsAnnotation; }

    @Override
    @SuppressWarnings("unchecked")
    public void setArgumentValues(
            final CommandLineArgumentParser commandLineArgumentParser,
            final PrintStream messageStream,
            final List<String> stringValues)
    {
        final List<String> expandedValues = stringValues
                .stream()
                .flatMap(s -> commandLineArgumentParser.expandFromExpansionFile(
                        this,
                        messageStream,
                        s,
                        stringValues).stream())
                .collect(Collectors.toList());
        for (final String stringValue : expandedValues) {
            final Object value = constructFromString(stringValue, POSITIONAL_ARGUMENTS_NAME);
            @SuppressWarnings("rawtypes")
            final Collection c;
            try {
                c = (Collection) getUnderlyingField().get(getContainingObject());
            } catch (final IllegalAccessException e) {
                throw new CommandLineException.ShouldNeverReachHereException(e);
            }
            if (c.size() >= positionalArgumentsAnnotation.maxElements()) {
                throw new CommandLineException(
                        String.format("No more than %d positional arguments may be specified.",
                                        positionalArgumentsAnnotation.maxElements())
                );
            }
            c.add(value);
        }
    }

    @Override
    public void validateValues(final CommandLineArgumentParser commandLineArgumentParser) {
        try {
            final Collection<?> c = (Collection<?>) getUnderlyingField().get(getContainingObject());
            if (c.size() < positionalArgumentsAnnotation.minElements()) {
                throw new CommandLineException.MissingArgument(
                        POSITIONAL_ARGUMENTS_NAME,
                        String.format("At least %d positional arguments must be specified.",
                                positionalArgumentsAnnotation.minElements()));
            }
        } catch (final IllegalAccessException e) {
            throw new CommandLineException.ShouldNeverReachHereException("Should never happen",e);
        }
    }

    @Override
    public String getDocString() { return positionalArgumentsAnnotation.doc(); }

    /**
     * Return a string with the usage statement for this positional argument.
     * @param argumentColumnWidth width reserved for argument name column display
     * @param descriptionColumnWidth width reserved for argument description column display
     * @return the usage string for this argument
     */
    public String getArgumentUsage(final int argumentColumnWidth, final int descriptionColumnWidth) {

        final StringBuilder sb = new StringBuilder();
        sb.append("--").append("POSITIONAL");
        sb.append(String.format(" <%s>", getUnderlyingFieldClass().getSimpleName()));

        int labelLength = sb.toString().length();
        int numSpaces = argumentColumnWidth - labelLength;
        if (labelLength > argumentColumnWidth) {
            sb.append("\n");
            numSpaces = argumentColumnWidth;
        }
        Utils.printSpaces(sb, numSpaces);

        String description = getArgumentDescription();
        if (isDeprecated()) {
            description = "This argument is DEPRECATED (" + getDeprecationDetail() + "). " + description;
        }
        final String wrappedDescription = Utils.wrapParagraph(description, descriptionColumnWidth);
        final String[] descriptionLines = wrappedDescription.split("\n");
        for (int i = 0; i < descriptionLines.length; ++i) {
            if (i > 0) {
                Utils.printSpaces(sb, argumentColumnWidth);
            }
            sb.append(descriptionLines[i]);
            sb.append("\n");
        }
        sb.append("\n");

        return sb.toString();
    }

    // Return a usage string representing this argument.
    private String getArgumentDescription() {
        final StringBuilder sb = new StringBuilder();
        if (!getDocString().isEmpty()) {
            sb.append(getDocString());
            sb.append("  ");
        }
        if (isCollection()) {
            sb.append("This argument must be specified at least once. ");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PositionalArgumentDefinition that = (PositionalArgumentDefinition) o;

        return getPositionalArgumentsAnnotation().equals(that.getPositionalArgumentsAnnotation());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getPositionalArgumentsAnnotation().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return POSITIONAL_ARGUMENTS_NAME + positionalArgumentsAnnotation;
    }

}
