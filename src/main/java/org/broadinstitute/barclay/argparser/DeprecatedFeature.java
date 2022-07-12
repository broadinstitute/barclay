package org.broadinstitute.barclay.argparser;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark a feature ({@link Argument} or {@link CommandLineProgramProperties}) as deprecated.
 */
@Target({ElementType.TYPE,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DeprecatedFeature {

    /**
     * @return the doc string associated with this command-line argument. Mutually exclusive with
     * {@link BetaFeature} and {@link ExperimentalFeature}.
     */
    String detail() default "This feature is deprecated and will be removed in a future release.";
}
