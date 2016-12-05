package org.broadinstitute.barclay.argparser;

import java.lang.annotation.*;

/**
 * Indicates that an argument should not be presented in the help system.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Hidden {
}
