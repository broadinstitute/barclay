package org.broadinstitute.barclay.argparser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotating a command line tool with this interface targets it for WDL generation. The attributes should be
 * WDL 1.0-conforming runtime block values (@see
 * https://github.com/openwdl/wdl/blob/master/versions/1.0/SPEC.md#runtime-section).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeProperties {

    // NOTE: When a new attribute is added here, code should also be added to WDLWorkUnitHandler.addCustomBindings
    // to propagate the value to the freemarker map, and the corresponding expansion of the value should be
    // added to the freemarker WDL template.

    /**
     * @return a WDL-compatible string specifying the runtime memory requirements for this tool
     */
    String memory() default "";

    /**
     * @return a WDL-compatible string specifying the runtime disk requirements for this tool
     */
    String disks() default "";
}
