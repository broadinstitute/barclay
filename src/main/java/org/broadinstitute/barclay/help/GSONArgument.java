package org.broadinstitute.barclay.help;

import java.util.List;
import java.util.Map;

/**
 * GSON-friendly version of the argument bindings
 */
public class GSONArgument {

    final String summary;
    final String name;
    final String synonyms;
    final String type;
    final String required;
    final String fulltext;
    final String defaultValue;
    final String minValue;
    final String maxValue;
    final String minRecValue;
    final String maxRecValue;
    final String kind;
    final boolean deprecated;
    final String deprecationDetail;
    List<Map<String, Object>> options;

    @SuppressWarnings("unchecked")
    public GSONArgument(final Map<String, Object> argMap) {
        this.summary = argMap.get("summary").toString();
        this.name = argMap.get("name").toString();
        this.synonyms = argMap.get("synonyms").toString();
        this.type = argMap.get("type").toString();
        this.required = argMap.get("required").toString();
        this.fulltext = argMap.get("fulltext").toString();
        this.defaultValue = argMap.get("defaultValue").toString();
        this.minValue = argMap.get("minValue").toString();
        this.maxValue = argMap.get("maxValue").toString();
        this.minRecValue = argMap.get("minRecValue").toString();
        this.maxRecValue = argMap.get("maxRecValue").toString();
        this.kind = argMap.get("kind").toString();
        this.deprecated = argMap.get("deprecated").equals(Boolean.TRUE);
        this.deprecationDetail = this.deprecated ?
                    argMap.get("deprecationDetail").toString() :
                    null;
        this.options = (List<Map<String, Object>>) argMap.get("options");
    }

}
