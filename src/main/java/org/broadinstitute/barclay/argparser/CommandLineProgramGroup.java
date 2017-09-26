package org.broadinstitute.barclay.argparser;

import java.util.Comparator;

/**
 * Annotation interface to allow a command line program to be assigned to an (application-defined) program group for
 * use in managing help output and documentation.
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
