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

//TODO: this class should really be consolidated with DocWorkUnit

/**
 * Documentation unit. Represents a single DocumentedFeature-annotated feature.
 *
 * @author depristo
 */
 public class DocumentedFeatureObject {
    private final Class<?> classToDoc; //class are we documenting
    private final boolean enable;
    private final String summary;
    private final String groupName;
    private final String groupSummary;
    private final Class<?>[] extraDocs;

    public DocumentedFeatureObject(
            final Class<?> classToDoc,
            final boolean enable,
            final String summary,
            final String groupName,
            final String groupSummary,
            final Class<?>[] extraDocs) {
        this.classToDoc = classToDoc;
        this.enable = enable;
        this.summary = summary;
        this.groupName = groupName;
        this.groupSummary = groupSummary;
        this.extraDocs = extraDocs;
    }

    public Class<?> getClassToDoc() { return classToDoc; }
    public boolean enable() { return enable; }
    public String summary() { return summary; }
    public String groupName() { return groupName; }
    public String groupSummary() { return groupSummary; }
    public Class<?>[] extraDocs() { return extraDocs; }
}
