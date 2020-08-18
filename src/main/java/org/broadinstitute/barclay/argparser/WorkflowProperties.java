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
public @interface WorkflowProperties {

    // NOTE: When a new attribute is added here, code should also be added to WDLWorkUnitHandler.addCustomBindings
    // to propagate the value to the freemarker map, and the corresponding expansion of the value should be
    // added to the freemarker WDL template.

    /**
     * @return a WDL-compatible string specifying the runtime memory requirements for this tool. Defaults to "4G".
     */
    String memory() default "4G";

    /**
     * @return number of CPUs to use for this tool
     */
    int cpu() default 2;

    /**
     * @return maximum number of times the workflow execution engine should request a preemptible machine for this
     * task before defaulting back to a non-preemptible one
     */
    int preEmptible() default 3;

    /**
     * @return a WDL-compatible string specifying the runtime disk requirements for this tool. Defaults to
     * "local-disk 40 HDD".
     */
    String disks() default "local-disk 40 HDD";

    /**
     * @return boot disk size of the disk where the docker image is booted
     */
    int bootDiskSizeGb() default 15;
}
