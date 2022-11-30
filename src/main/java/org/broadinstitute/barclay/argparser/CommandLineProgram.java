package org.broadinstitute.barclay.argparser;

/**
 * Interface class to facilitate implementing command-line programs.
 *
 * <p>It should be considered as a marker interface to identify runnable Command Line Program
 * classes in different toolkits, to aggregate them when looking for them. In addition, it includes
 * the method {@link #instanceMain(String[])} to be call to parse the arguments and run the
 * actual work.
 *
 * <p>Tool classes may want to extend this interface and {@link CommandLinePluginProvider} to
 * include plugins in their command line.
 *
 * <p>Usage example:
 *
 * <ul>
 *     <li>
 *         Extend the class. Users may want to extend also {@link CommandLinePluginProvider} to
 *         include plugins in the command line.
 *     </li>
 *     <li>
 *         Mark the class with {@link CommandLineProgramProperties} to include information for the
 *         tool. Users may consider to annotate with {@link BetaFeature} tools that are in experimental
 *         phase.
 *     </li>
 *     <li>
 *         Include data members marked with {@link Argument}, {@link ArgumentCollection},
 *         {@link PositionalArguments} or {@link TaggedArgument} to include in the command line.
 *         This members could be also annotated with {@link Hidden} or {@link Advanced} to tweak
 *         how they are display.
 *     </li>
 *     <li>
 *         Implement {@link #instanceMain(String[])} to use {@link CommandLineArgumentParser} to
 *         parse the arguments and use the data members to perform the work.
 *     </li>
 *     <<li>
 *         In a Main class, look for all the classes implementing {@link CommandLineProgram} (e.g.,
 *         using {@link ClassFinder}). Matching the class name with a provided String, the user can
 *         select the tool to be run, and the Main class can call {@link #instanceMain(String[])}
 *         with the provided arguments.
 *     </li>
 * </ul>
 *
 * <p>Note: Barclay does not use or enforce this class for any purpose.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface CommandLineProgram {

    /**
     * Implements the work to be done with the tool.
     *
     * <p>This method should implement:
     *
     * <ul>
     *     <li>Command line parsing using {@link CommandLineParser}</li>
     *     <li>Run the actual work of the tool using data members populated by the parser.</li>
     *     <li>Return the result of the tool.</li>
     * </ul>
     *
     * <p>Note: Most exceptions should be caught by the caller.
     *
     * @param argv arguments to pass to the {@link CommandLineParser}.
     *
     * @return result object after performing the work. May be {@code null}.
     */
    public Object instanceMain(final String[] argv);
}
