package org.broadinstitute.barclay.argparser;

import java.lang.annotation.*;

/**
 * Indicates that an argument is considered an advanced option.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Advanced {
}
