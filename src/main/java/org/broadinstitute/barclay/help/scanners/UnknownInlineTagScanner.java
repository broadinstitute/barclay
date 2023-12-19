package org.broadinstitute.barclay.help.scanners;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.util.DocTreeScanner;
import jdk.javadoc.doclet.DocletEnvironment;
import org.broadinstitute.barclay.utils.Utils;

import javax.lang.model.element.Element;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * A DocTree scanner to retrieve unknown inline tags (not block tags) that are embedded in a javadoc comment
 * for a given Element.
 */
public class UnknownInlineTagScanner extends DocTreeScanner<Void, Void> {
    final DocTree docTree;
    final DocletEnvironment docEnv;
    final private Map<String, List<String>> inlineTags = new HashMap<>();

    /**
     * For internal use only. External callers should use
     * {@link JavaLanguageModelScanners#getUnknownInlineTags(DocletEnvironment, Element)}}
     *
     * @param docEnv the {@link DocletEnvironment}
     * @param docTree the {@link DocTree} for which the javadoc comment should be retrieved
     */
     UnknownInlineTagScanner(final DocletEnvironment docEnv, final DocTree docTree) {
        Utils.nonNull(docEnv, "doclet environment");
        Utils.nonNull(docTree, "docTree");

        this.docEnv = docEnv;
        this.docTree = docTree;
    }

    @Override
    public Void scan(final DocTree t, final Void unused) {
        Utils.nonNull(docTree, "DocTree");

        // unknown inline tags might be custom tags, so break them down and return the parts
        // for the caller to parse
        if (t != null && t.getKind().equals(DocTree.Kind.UNKNOWN_INLINE_TAG)) {
            final UnknownInlineTagTree tagTree = (UnknownInlineTagTree) t;
            final List<String> tagParts = tagTree.getContent().stream().map(Object::toString).toList();
            inlineTags.put(tagTree.getTagName(), tagParts);
            return null;
        }
        return super.scan(t, null);
    }

    /**
     * @return a map of inline tags embedded in the doc comment for the {@link Element}. If no tags
     * are present, an empty map will be returned.
     */
    Map<String, List<String>> getInlineTags() {
        return inlineTags;
    }

}
