package org.broadinstitute.barclay.argparser;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate which fields of a CommandLineProgram are options given at the command line.
 * If a command line call looks like "cmd -option foo -x y bar baz" the CommandLineProgram
 * would have annotations on fields to handle the values of option and x. The java type of the option
 * will be inferred from the type of the field or from the generic type of the collection
 * if this option is allowed more than once. The type must be an enum or
 * have a constructor with a single String parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Argument {

    /**
     * The full name of the command-line argument.  Full names should be
     * prefixed on the command-line with a double dash (--).
     * @return Selected full name, or "" to use the default.
     */
    String fullName() default "";

    /**
     * Specified short name of the command.  Short names should be prefixed
     * with a single dash.  Argument values can directly abut single-char
     * short names or be separated from them by a space.
     * @return Selected short name, or "" for none.
     */
    String shortName() default "";

    /**
     * Documentation for the command-line argument.  Should appear when the
     * --help argument is specified.
     * @return Doc string associated with this command-line argument.
     */
    String doc() default "Undocumented option";

    /**
     * If set to false, a {@link org.broadinstitute.barclay.argparser.CommandLineException.MissingArgument} will be thrown
     * if the option is not specified.
     * If 2 options are mutually exclusive and both are required it will be interpreted as one or the other is required
     * and an exception will only be thrown if neither are specified.
     * An argument with a non-null default value specified will ignore this flag and always be treated as optional
     */
    boolean optional() default false;

    /**
     * Array of option names that cannot be used in conjunction with this one.
     * If 2 options are mutually exclusive and both have optional=false it will be
     * interpreted as one OR the other is required and an exception will only be thrown if
     * neither are specified.
     */
    String[] mutex() default {};

    /**
     * Is this an Option common to all command line programs.  If it is then it will only
     * be displayed in usage info when H or STDHELP is used to display usage.
     */
    boolean common() default false;

    /**
     * Does this option have special treatment in the argument parsing system.
     * Some examples are arguments_file and help, which have special behavior in the parser.
     * This is intended for documenting these options.
     */
    boolean special() default false;

    /**
     * Are the contents of this argument private and should be kept out of logs.
     * Examples of sensitive arguments are encryption and api keys.
     */
    boolean sensitive() default false;

    /**
     * Overwrite default order in which Option are printed in usage by explicitly setting a
     * print position e.g. printOrder=1 is printed before printOrder=2.
     * Options without printOrder automatically receive a printOrder that (1) is a multiple of 1000
     * and (2) reflects the order's default position. This gives you the option to insert your own options between
     * options inherited from super classes (which order you do not control).
     * The default ordering follows (1)the option declaration position in the class and (2) sub-classes options printed
     *  before superclass options.
     *
     * @author charles girardot
     */
    int printOrder() default Integer.MAX_VALUE;

    /** The minimum number of times that this option is required. */
    int minElements() default 0;

    /** The maximum number of times this option is allowed. */
    int maxElements() default Integer.MAX_VALUE;

    /**
     * This boolean determines if this annotation overrides a parent annotation. If that is the case then
     * the options of the parent annotation are overridden with this annotation.
     */
    boolean overridable() default false;

    /**
     * Hard lower bound on the allowed value for the annotated argument -- generates an exception if violated.
     * Enforced only for numeric types whose values are explicitly specified on the command line.
     *
     * @return Hard lower bound on the allowed value for the annotated argument, or Double.NEGATIVE_INFINITY
     *         if there is none.
     */
    double minValue() default Double.NEGATIVE_INFINITY;

    /**
     * Hard upper bound on the allowed value for the annotated argument -- generates an exception if violated.
     * Enforced only for numeric types whose values are explicitly specified on the command line.
     *
     * @return Hard upper bound on the allowed value for the annotated argument, or Double.POSITIVE_INFINITY
     *         if there is none.
     */
    double maxValue() default Double.POSITIVE_INFINITY;

    /**
     * Soft lower bound on the allowed value for the annotated argument -- generates a warning if violated.
     * Enforced only for numeric types whose values are explicitly specified on the command line.
     *
     * @return Soft lower bound on the allowed value for the annotated argument, or Double.NEGATIVE_INFINITY
     *         if there is none.
     */
    double minRecommendedValue() default Double.NEGATIVE_INFINITY;

    /**
     * Soft upper bound on the allowed value for the annotated argument -- generates a warning if violated.
     * Enforced only for numeric types whose values are explicitly specified on the command line.
     *
     * @return Soft upper bound on the allowed value for the annotated argument, or Double.POSITIVE_INFINITY
     *         if there is none.
     */
    double maxRecommendedValue() default Double.POSITIVE_INFINITY;

}
