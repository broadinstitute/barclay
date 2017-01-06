package org.broadinstitute.barclay.argparser;

import java.util.Comparator;

/**
 * Interface for groups of CommandLinePrograms.
 * @author Nils Homer
 */
public interface CommandLineProgramGroup {

    /** Gets the name of this group. **/
    public String getName();
    /** Gets the description of this group. **/
    public String getDescription();
    /** Compares two program groups by name. **/
    public static Comparator<CommandLineProgramGroup> comparator = (a, b) -> a.getName().compareTo(b.getName());
}
