package org.broadinstitute.barclay.argparser;

import java.lang.annotation.*;

/**
 * Annotates a command line program with various properties, such as usage (short and long),
 * as well as to which program group it belongs.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface CommandLineProgramProperties {
    /**
     * @return a summary of what the program does
     */
    String summary();

    /**
     * @return a very short summary for the main menu list of all programs
     */
    String oneLineSummary();

    /**
     * @return an example command line for this program
     */
    String usageExample() default "The author of this program hasn't included any example usage, please complain to them.";

    /**
     * Assign this command line program to an (application-defined) program group for use in managing help output
     * and documentation.
     * @return CommandLineProgramGroup for this command line program
     */
    Class<? extends CommandLineProgramGroup> programGroup();

    /**
     * Property for use by client applications that may want to hide some commandline programs from interactive
     * command line help output. This property is for use by consuming applications only. Barclay does not use
     * or enforce this property for any purpose (including generated documentation, which is governed separately by
     * the {@link org.broadinstitute.barclay.help.DocumentedFeature} annotation).
     *
     * @return {@code true} if this command line program should be omitted by the Barclay client application from
     * interactive help output, otherwise {@code false}
     */
    boolean omitFromCommandLine() default false;
}
