package org.broadinstitute.barclay.help.scanners;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.util.DocTreeScanner;
import jdk.javadoc.doclet.DocletEnvironment;
import org.broadinstitute.barclay.utils.Utils;

import javax.lang.model.element.Element;

/**
 * A {@link DocTreeScanner} to retrieve a javadoc comment, if any, for a given Element.
 */
class CommentScanner extends DocTreeScanner<Void, Void> {
    final DocletEnvironment docEnv;
    final Element targetElement;
    String targetComment;

    /**
     * For internal use only. External callers should use
     * {@link JavaLanguageModelScanners#getDocComment(DocletEnvironment, Element)} getDocComment}
     *
     * @param docEnv the {@link DocletEnvironment}
     * @param targetElement the {@link Element} for which the documentation comment should be retrieved
     */
    CommentScanner(final DocletEnvironment docEnv, final Element targetElement) {
        Utils.nonNull(docEnv, "doclet env");
        Utils.nonNull(targetElement, "target element");

        this.docEnv = docEnv;
        this.targetElement = targetElement;
    }

    @Override
    public Void scan(final DocTree docTree, final Void unused) {
        Utils.nonNull(docTree, "DocTree");

        if (docTree.getKind().equals(DocTree.Kind.DOC_COMMENT)) {
            targetComment = docTree.toString();
            return null;
        }

//        System.out.println("docTree type: " + docTree);
//
//        switch (docTree.getKind()) {
//            case DOC_COMMENT:
//                targetComment = docTree.toString();
//                break;
//
//            case COMMENT:
//                System.out.println("Unrecognized docTree type: " + docTree.getKind() + " :" + docTree);
//                break;
//
//            case VALUE:
//                final Element fieldElement = JavaLanguageModelScanners.getElementForField(
//                        docEnv, workUnit.getDocElement(), argDef.getUnderlyingField());
//
//                final ValueTree vt = (ValueTree) docTree;
//                final ReferenceTree refTree = vt.getReference();
//                final String sig = refTree.getSignature();
//                final String ctS = vt.toString();
//                System.out.println("Unrecognized docTree type: " + docTree.getKind() + " :" + docTree);
//                break;
//
//            case REFERENCE:
//                final ReferenceTree rt = (ReferenceTree) docTree;
//                final String rtS = rt.toString();
//                System.out.println("Unrecognized docTree type: " + docTree.getKind() + " :" + docTree);
//                break;
//            default:
//                //System.out.println("Unrecognized docTree type: " + docTree);
//                break;
//        }
        return super.scan(docTree, null);
    }

    /**
     * @return the comment for the {@link Element}. May be null
     */
    String getComment() {
        return targetComment;
    }

}

