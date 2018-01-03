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
    boolean beta;
    boolean experimental;

    public void populate(String summary,
                         Object arguments,
                         String description,
                         String name,
                         String group,
                         boolean beta,
                         boolean experimental
    ) {
        this.summary = summary;
        this.arguments = arguments;
        this.description = description;
        this.name = name;
        this.group = group;
        this.beta = beta;
        this.experimental = experimental;
    }

}
