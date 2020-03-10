package org.broadinstitute.barclay.argparser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotating a command line tool with this interface targets it for WDL generation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeProperties {

    // NOTE: any new attributes added here should be propagated to the freemarker map in WDLDoclet.addCustomBindings,
    // and expanded as appropriate in the freemarker wdl template.
    /**
     * @return a WDL-compatible string specifying the runtime memory requirements for this tool
     */
    String memory() default "";
}
