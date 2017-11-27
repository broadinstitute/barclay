package org.broadinstitute.barclay.argparser;

import java.lang.annotation.*;

/**
 * Marker interface for features that are experimental and not for production use. These tools may never become stable
 * and may be changed dramatically or completely removed.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ExperimentalFeature {
}
