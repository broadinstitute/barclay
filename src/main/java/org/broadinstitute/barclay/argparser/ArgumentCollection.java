package org.broadinstitute.barclay.argparser;

import java.lang.annotation.*;

/**
 * Used to annotate a field in a CommandLineProgram that holds an instance containing @Argument-annotated
 * fields.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
public @interface ArgumentCollection {
    /** Text that appears for this group of options in text describing usage of the command line program. */
    String doc() default "";
}
