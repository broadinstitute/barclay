package org.broadinstitute.barclay.argparser;

import java.lang.annotation.*;

/**
 * Marker interface for features that are under development and not ready for production use.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface BetaFeature {
}
