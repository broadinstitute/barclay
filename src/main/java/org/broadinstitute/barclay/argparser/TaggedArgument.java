package org.broadinstitute.barclay.argparser;

import java.util.Map;

/**
 * Interface for arguments that can have tags and attributes. The command line argument parser
 * looks for argument fields that implement this interface, and propagates tag names and
 * attribute/value pairs to the field.
 *
 * NOTE: No check is done to prevent duplicate tag names from being used more than once
 * in an arugment that is a Collection.
 */
public interface TaggedArgument {

    /**
     * Set the tag name (optional - required only if attributes are present) for this instance.
     * @param tagName
     */
    void setTag(String tagName);

    /**
     * Retrieve the tag name for this instance.
     * @return String representing the tagName. Should not return null.
     */
    String getTag();

    /**
     * Set the attribute/value pairs for this instance.
     * @param attributes Map of attribute names and values for this arguments.
     */
    void setTagAttributes(Map<String, String> attributes);

    /**
     * Get the attribute/value pair Map for this instance. May be empty.
     * @return Map of attribute/value pairs for this instance.
     */
    Map<String, String> getTagAttributes();

}
