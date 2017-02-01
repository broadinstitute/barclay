package org.broadinstitute.barclay.help;

import java.lang.annotation.*;

/**
 * An annotation to identify a class as a target for documentation. Classes tagged with
 * the annotation should have a no-arg constructor that can be called by the doc system.
 *
 * The properties of this annotation all have defaults so that this annotation can be used as a tag
 * interface for classes that are also annotated with CommandLineProgramProperties. The doc gen system
 * will attempt to retrieve any values that are missing from the DocumentedFeature annotation with values
 * from the {@code CommandLineProgramProperties} and {@code CommandLineProgramGroup} annotations.
 *
 * @author depristo
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DocumentedFeature {
    /**
     * Should we actually document this feature, even though it's annotated?
     */
    public boolean enable() default true;
    /**
     * The overall group name (walkers, readfilters) this feature is associated with
     * @return The overall group name (walkers, readfilters) this feature is associated with
     */
    public String groupName() default "";

    /**
     * A human readable summary of the purpose of this group of features
     * @return A human readable summary of the purpose of this group of features
     */
    public String groupSummary() default "";

    /**
     * A human readable summary of the purpose of this feature
     * @return A human readable summary of the purpose of this feature
     */
    public String summary() default "";

    /**
     * Are there links to other docs that we should include?  Must reference a class that itself uses
     * the DocumentedFeature documentedFeatureObject.
     */
    public Class<?>[] extraDocs() default {};
}
