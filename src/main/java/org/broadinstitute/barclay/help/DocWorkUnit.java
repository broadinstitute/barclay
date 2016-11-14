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

import com.sun.javadoc.ClassDoc;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple collection of all relevant information about something the HelpDoclet can document
 *
 * Created by IntelliJ IDEA.
 * User: depristo
 * Date: 7/24/11
 * Time: 7:59 PM
 */
public class DocWorkUnit implements Comparable<DocWorkUnit> {
    /**
     * The class that's being documented
     */
    protected final Class<?> clazz;
    /**
     * The name of the thing we are documenting
     */
    protected final String name;
    /**
     * The name of the documentation group (e.g., walkers, read filters) class belongs to
     */
    protected final String group;
    /**
     * The documentation handler for this class
     */
    protected final DocumentedFeatureHandler handler;
    /**
     * The javadoc documentation for clazz
     */
    protected final ClassDoc classDoc;
    /**
     * The documentedFeatureObject that lead to this Class being in Doc
     */
    protected final DocumentedFeatureObject documentedFeatureObject;
    /**
     * When was this walker built, and what's the absolute version number
     */
    protected final String buildTimestamp;
    protected final String absoluteVersion;

    // set by the handler
    protected String summary;

    // needs to be accessible from DocWorkUnits in specialized doc subclasses
    public Map<String, Object> rootMap; // this is where the actual doc content gets stored

    public DocWorkUnit(
            final String name,
            final String group,
            final DocumentedFeatureObject annotation,
            final DocumentedFeatureHandler handler,
            final ClassDoc classDoc,
            final Class<?> clazz,
            final String buildTimestamp,
            final String absoluteVersion) {
        this.documentedFeatureObject = annotation;
        this.name = name;
        this.group = group;
        this.handler = handler;
        this.classDoc = classDoc;
        this.clazz = clazz;
        this.buildTimestamp = buildTimestamp;
        this.absoluteVersion = absoluteVersion;
    }

    /**
     * Called by the Doclet to set handler provided context for this work unit
     *
     * @param summary
     * @param rootMap
     */
    public void setHandlerContent(final String summary, final Map<String, Object> rootMap) {
        this.summary = summary;
        this.rootMap = rootMap;
    }

    /**
     * Return a String -> String map suitable for FreeMarker to create an index to this WorkUnit
     *
     * @return
     */
    public Map<String, String> indexDataMap() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("name", name);
        data.put("summary", summary);
        data.put("filename", getTargetFileName());
        data.put("group", group);
        return data;
    }

    public String getTargetFileName() { return handler.getDestinationFilename(classDoc, clazz); }

    /**
     * Sort in order of the name of this WorkUnit
     *
     * @param other
     * @return
     */
    public int compareTo(DocWorkUnit other) {
        return this.name.compareTo(other.name);
    }
}
