package org.broadinstitute.barclay.help.scanners;

import com.sun.source.doctree.DocCommentTree;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.broadinstitute.barclay.help.DocWorkUnit;
import org.broadinstitute.barclay.help.HelpDoclet;
import org.broadinstitute.barclay.utils.Utils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.util.ElementFilter;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface to javax.lang.model methods to find and retrieve various java language elements needed by
 * the Barclay doc and help tasks.
 */
public class JavaLanguageModelScanners {
    private final static String EMPTY_COMMENT = "";

    /**
     * @param helpDoclet the {@link HelpDoclet} used for the Barlcay doc task being executed, may not be null
     * @param docEnv the {@link DocletEnvironment} for the Barclay doc task being executed, may not be null
     * @param reporter Reporter to use to display messages, may not be null
     * @param includedElements the set of {@link Element}s included in the Barclay doc run, may not be null
     * @return the set of {@DocWorkUnit}s for the {@code includedElements}
     */
    public static Set<DocWorkUnit> getWorkUnits(
            final HelpDoclet helpDoclet,
            final DocletEnvironment docEnv,
            final Reporter reporter,
            final Set<? extends Element> includedElements) {
        Utils.nonNull(helpDoclet, "helps doclet");
        Utils.nonNull(docEnv, "doclet environment");
        Utils.nonNull(reporter, "reporter");
        Utils.nonNull(includedElements, "includedElements");

        final DocumentedFeatureScanner documentedFeatureScanner = new DocumentedFeatureScanner(helpDoclet, docEnv, reporter);
        documentedFeatureScanner.scan(ElementFilter.typesIn(includedElements), null);
        return documentedFeatureScanner.getWorkUnits();
    }

    /**
     * @param docEnv the {@link DocletEnvironment} for the Barlcay doc task being executed, may not be null
     * @param targetElement the {@link Element} for which the javadoc comment should be retrieved, may not be null
     * @return the javadoc comment for the target {@link Element}, or an empty string if no comment is present
     */
    public static String getDocComment(final DocletEnvironment docEnv, final Element targetElement) {
        Utils.nonNull(docEnv, "doclet env");
        Utils.nonNull(targetElement, "target element");

        final CommentScanner docScanner = new CommentScanner(docEnv, targetElement);
        final DocCommentTree docTree = docEnv.getDocTrees().getDocCommentTree(targetElement);
        if (docTree == null) {
            return EMPTY_COMMENT;
        }
        docScanner.scan(docTree, null);
        return docScanner.getComment();
    }

    /**
     * @param docEnv the {@link DocletEnvironment} for the Barclay doc task being executed, may not be null
     * @param targetElement the {@link Element} for which the javadoc comment should be retrieved, may not be null
     * @return the javadoc comment, if any, with all embedded tags removed, otherwise an empty String
     */
    public static String getDocCommentWithoutTags(final DocletEnvironment docEnv, final Element targetElement) {
        Utils.nonNull(docEnv, "doclet env");
        Utils.nonNull(targetElement, "targetElement");

        final DocCommentTree docTree = docEnv.getDocTrees().getDocCommentTree(targetElement);
        if (docTree == null) {
            return "";
        }
        final CommentScannerWithTagFilter docScanner = new CommentScannerWithTagFilter(docEnv, docTree);
        docScanner.scan(docTree, null);
        return docScanner.getCommentWithoutTags();
    }

    /**
     * @param docEnv the {@link DocletEnvironment} for the Barclay doc task being executed, may not be null
     * @param targetElement the {@link Element} for which the javadoc comment first sentence should be retrieved,
     *                     may not be null
     * @return the first sentence of the javadoc comment for the target {@link Element}, or an empty string if
     * no comment is present
     */
    public static String getDocCommentFirstSentence(final DocletEnvironment docEnv, final Element targetElement) {
        final DocCommentTree docTree = docEnv.getDocTrees().getDocCommentTree(targetElement);
        final StringBuilder sb = new StringBuilder();
        if (docTree != null) {
            docTree.getFirstSentence().forEach(t -> sb.append(t.toString()));
        }
        return sb.toString();
    }

    /**
     * It's possible in some cases for a field to not have a corresponding element within a given enclosing
     * element, even if the field is declared within the class represented by the (enclosing) element, i.e.,
     * this can happen if the field is initialized by instantiating an anonymous class.
     *
     * @param docEnv the {@link DocletEnvironment} for the Barclay doc run, may not be null
     * @param enclosingClassElement the element corresponding to the enclosing class in which the target
     *                         {@link Field} is declared, may not be null
     * @param targetField the target {@link Field} for which an element should be found
     * @param targetElementKind the target {@link ElementKind} to look for, may not be null
     * @return the Element for the Field {@code targetField}, or null if no element can be located
     */
    public static Element getElementForField(
            final DocletEnvironment docEnv,
            final Element enclosingClassElement,
            final Field targetField,
            final ElementKind targetElementKind) {
        Utils.nonNull(docEnv, "doclet environment");
        Utils.nonNull(enclosingClassElement, "enclosing element");
        Utils.nonNull(targetField, "target field");
        Utils.nonNull(targetElementKind, "target element kind");

        final FieldScanner fieldScanner = new FieldScanner(docEnv, targetField.getName(), targetElementKind);
        fieldScanner.scan(enclosingClassElement);
        return fieldScanner.getFieldElement();
    }

    /**
     * @param docEnv the {@link DocletEnvironment}, may not be null
     * @param targetElement the target {@link Element} to use to find inline tags, may not be null
     * @return map of tag name to tag parts list, or an empty map if no tags present, for all unknown inline tags;
     * it is the caller's responsibility to extract and recognize doclet-specific tags from the list returned
     */
    public static Map<String, List<String>> getUnknownInlineTags(final DocletEnvironment docEnv, final Element targetElement) {
        Utils.nonNull(docEnv, "doclet env");
        Utils.nonNull(targetElement, "targetElement");

        final DocCommentTree docTree = docEnv.getDocTrees().getDocCommentTree(targetElement);
        if (docTree != null) {
            final UnknownInlineTagScanner unknownInlineTagScanner = new UnknownInlineTagScanner(docEnv, docTree);
            unknownInlineTagScanner.scan(docTree, null);
            return unknownInlineTagScanner.getInlineTags();
        }
        return new LinkedHashMap<>();
    }
}
