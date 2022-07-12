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
    boolean deprecated;

    public GSONWorkUnit(final DocWorkUnit workUnit) {
        this.summary = workUnit.getProperty("summary").toString();
        this.arguments = workUnit.getProperty("gson-arguments");
        this.description = workUnit.getProperty("description").toString();
        this.name = workUnit.getProperty("name").toString();
        this.group = workUnit.getProperty("group").toString();
        this.beta = Boolean.valueOf(workUnit.getProperty("beta").toString());
        this.experimental = Boolean.valueOf(workUnit.getProperty("experimental").toString());
        this.deprecated = Boolean.valueOf(workUnit.getProperty("deprecated").toString());
    }

}
