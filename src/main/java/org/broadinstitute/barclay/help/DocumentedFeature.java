/*
* Copyright 2012-2016 Broad Institute, Inc.
* 
* Permission is hereby granted, free of charge, to any person
* obtaining a copy of this software and associated documentation
* files (the "Software"), to deal in the Software without
* restriction, including without limitation the rights to use,
* copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the
* Software is furnished to do so, subject to the following
* conditions:
* 
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
* OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
* THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package org.broadinstitute.barclay.help;

import java.lang.annotation.*;

/**
 * An annotation to identify a class as a target for documentation. Classes tagged with
 * the annotation should have a no-arg constructor that can be called by the doc system.
 *
 * @author depristo
 */
@Documented
@Inherited
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
    public String groupName();

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
