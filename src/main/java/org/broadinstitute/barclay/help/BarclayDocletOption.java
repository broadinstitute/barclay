package org.broadinstitute.barclay.help;

import jdk.javadoc.doclet.Doclet;

import java.util.List;

/**
 * Class to represent an individual Barclay doclet option.
 */
abstract public class BarclayDocletOption implements Doclet.Option {
    // record to keep all of the components required for an option
    private record DocletOptionRecord(
            List<String> aliases,
            String description,
            int argCount,
            Doclet.Option.Kind kind,
            String parameters) { }
    private final DocletOptionRecord optionRecord;

    /**
     * @param aliases list of aliases for this option
     * @param description description for this option
     * @param argCount number of arguments for this option, may be 0
     * @param kind the {@link Doclet.Option.Kind} for this option
     * @param parameters {@link Doclet.Option} syntax describing the types of the option parameters
     */
    public BarclayDocletOption(final List<String> aliases,
                               final String description,
                               final int argCount,
                               final Doclet.Option.Kind kind,
                               final String parameters) {
        optionRecord = new DocletOptionRecord(aliases, description, argCount, kind, parameters);
    }

    @Override
    public int getArgumentCount() { return optionRecord.argCount();}

    @Override
    public String getDescription() { return optionRecord.description(); }

    @Override
    public Kind getKind() { return optionRecord.kind(); }

    @Override
    public List<String> getNames() { return optionRecord.aliases(); }

    @Override
    public String getParameters() { return optionRecord.parameters(); }

    @Override
    public int hashCode() {
        return optionRecord.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return optionRecord.equals(obj);
    }

    @Override
    public String toString() {
        return optionRecord.toString();
    }
}
