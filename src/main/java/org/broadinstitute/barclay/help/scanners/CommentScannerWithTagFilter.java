package org.broadinstitute.barclay.help.scanners;

import com.sun.source.doctree.DocTree;
import com.sun.source.util.DocTreeScanner;
import jdk.javadoc.doclet.DocletEnvironment;
import org.broadinstitute.barclay.utils.Utils;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/*
 * A {@link DocTreeScanner} scanner to retrieve the javadoc comment for an Element, with any embedded tags
 * removed, given an existing DocTree.
 */
class CommentScannerWithTagFilter extends DocTreeScanner<Void, Void> {
    final DocTree docTree;
    final DocletEnvironment docEnv;
    final private List<String> targetDocCommentParts = new ArrayList<>();

    /**
     * For internal use only. External callers should use
     * {@link JavaLanguageModelScanners#getDocCommentWithoutTags(DocletEnvironment, DocTree)} getDocCommentWithoutTags}
     *
     * @param docEnv the {@link DocletEnvironment}
     * @param docTree the {@link DocTree} for which the javadoc comment should be retrieved
     */
    CommentScannerWithTagFilter(final DocletEnvironment docEnv, final DocTree docTree) {
        Utils.nonNull(docEnv, "doclet environment");
        Utils.nonNull(docTree, "docTree");

        this.docEnv = docEnv;
        this.docTree = docTree;
    }

    @Override
    public Void scan(final DocTree t, final Void unused) {
        Utils.nonNull(docTree, "DocTree");

        if (t.getKind().equals(DocTree.Kind.TEXT)) {
            // skip embedded tags and only retain text
            targetDocCommentParts.add(t.toString());
            return null;
        }
        return super.scan(t, null);
    }

    /**
     * @return the javadoc comment for the {@link Element}, with any embedded tags removed. If no javadoc
     * comment is present, an empty string will be returned.
     */
     String getComment() {
        return targetDocCommentParts.stream().collect(Collectors.joining(" "));
    }

}
