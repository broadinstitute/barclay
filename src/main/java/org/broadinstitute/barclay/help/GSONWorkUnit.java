package org.broadinstitute.barclay.help;

/**
 * GSON-friendly version of the DocWorkUnit
 */
public class GSONWorkUnit {

    String summary;
    Object arguments;
    String description;
    String name;
    String group;

    public void populate(String summary,
                         Object arguments,
                         String description,
                         String name,
                         String group
    ) {
        this.summary = summary;
        this.arguments = arguments;
        this.description = description;
        this.name = name;
        this.group = group;
    }

}
