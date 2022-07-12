package org.broadinstitute.barclay.argparser;

import java.lang.annotation.*;

/**
 * Marker interface for features that are under development and not ready for production use. Mutually exclusive
 * with {@link ExperimentalFeature} and {@link DeprecatedFeature}.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface BetaFeature {
}
