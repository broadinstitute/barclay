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
     * @param tagName May be null to indicate no tag name.
     */
    void setTag(String tagName);

    /**
     * Retrieve the tag name for this instance.
     * @return String representing the tagName. May be null if no tag name was set.
     */
    String getTag();

    /**
     * Set the attribute/value pairs for this instance.
     * @param attributes Map of attribute names and values for this arguments. May be empty, should not be null.
     */
    void setTagAttributes(Map<String, String> attributes);

    /**
     * Get the attribute/value pair Map for this instance. May be empty.
     * @return Map of attribute/value pairs for this instance. May be empty if no tags are present. May not be null.
     */
    Map<String, String> getTagAttributes();

}
