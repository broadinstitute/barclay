package org.broadinstitute.barclay.help.scanners;

import jdk.javadoc.doclet.DocletEnvironment;
import org.broadinstitute.barclay.utils.Utils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.util.ElementScanner14;
import java.lang.reflect.Field;

/**
 * An {@link ElementScanner14} for finding the {@link Element} that corresponds to a given {@link Field}, given
 * an Element that represents the class in which the field is declared.
 *
 * It's possible in some cases for a field to not have a corresponding element within a given enclosing
 * element, even if the field is declared within the class represented by the (enclosing) element, i.e.,
 * this can happen if the field is initialized by instantiating an anonymous class.
 */
public class FieldScanner extends ElementScanner14<Void, Void> {
    final DocletEnvironment docEnv;
    final String queryFieldName;
    final ElementKind elementKind;
    Element resultElement;

    /*
     * For internal use only. External callers should use
     * {@link #findElementForField(DocletEnvironment, Field, Element)}
     *
     * @param docEnv the {@link DocletEnvironment}
     * @param queryFieldName name of the {@link Field} to interrogate
     */
     FieldScanner(final DocletEnvironment docEnv, final String queryFieldName, final ElementKind elementKind ) {
        Utils.nonNull(docEnv, "doclet environment");
        Utils.nonNull(queryFieldName, "queryFieldName");

        this.docEnv = docEnv;
        this.queryFieldName = queryFieldName;
        this.elementKind = elementKind;
    }

    @Override
    public Void scan(final Element e, final Void unused) {
        if (e.getSimpleName().toString().equals(queryFieldName)) {
            final ElementKind k = e.getKind();
            if (k == elementKind) {
                // we need to make sure we only select FIELD elements since the same
                // queryname can show up as paramaters, etc.
                resultElement = e;
                return null;
            }
        }
        return super.scan(e, unused);
    }

    /**
     * @return the resulting {@link Element}, if any for the {@link Field} provided
     */
    final Element getFieldElement() { return resultElement; }

}
