package org.broadinstitute.barclay.help;

import java.util.List;
import java.util.Map;

/**
 * GSON-friendly version of the argument bindings
 */
public class GSONArgument {

    String summary;
    String name;
    String synonyms;
    String type;
    String required;
    String fulltext;
    String defaultValue;
    String minValue;
    String maxValue;
    String minRecValue;
    String maxRecValue;
    String kind;
    List<Map<String, Object>> options;

    public void populate(   final String summary,
                            final String name,
                            final String synonyms,
                            final String type,
                            final String required,
                            final String fulltext,
                            final String defaultValue,
                            final String minValue,
                            final String maxValue,
                            final String minRecValue,
                            final String maxRecValue,
                            final String kind,
                            final List<Map<String, Object>> options
    ) {
        this.summary = summary;
        this.name = name;
        this.synonyms = synonyms;
        this.type = type;
        this.required = required;
        this.fulltext = fulltext;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minRecValue = minRecValue;
        this.maxRecValue = maxRecValue;
        this.kind = kind;
        this.options = options;
    }

}
