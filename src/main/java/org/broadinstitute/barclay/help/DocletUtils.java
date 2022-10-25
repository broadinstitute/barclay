package org.broadinstitute.barclay.help;

import jdk.javadoc.doclet.DocletEnvironment;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Package protected - Methods in the class must ONLY be used by doclets, since the com.sun.javadoc.* classes
 * are not available on all systems, and we don't want the GATK proper to depend on them.
 */
public class DocletUtils {

    public static Class<?> getClassForDeclaredElement(final Element docElement, final DocletEnvironment docEnv) {
         return getClassForClassName(getClassName(docElement, docEnv));
    }

    public static Class<?> getClassForClassName(final String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // we got an Element for a class we can't find.  Maybe in a library or something
            return null;
        }
    }

    /**
     * Reconstitute the class name from the given class JavaDoc object.
     *
     * @param element the Element for a class.
     * @return The (string) class name of the given class. Maybe null if no class can be found.
     */
    protected static String getClassName(final Element element, final DocletEnvironment docEnv) {
        final PackageElement pe = docEnv.getElementUtils().getPackageOf(element);
        if (pe == null || !element.toString().contains(".")) {
            return element.toString();
        }
        final String qualifiedName = pe.getQualifiedName().toString();
        final String className = element.toString().substring(qualifiedName.length() + 1);
        final String qualifiedClassName = className.replaceAll("\\.", "\\$");
        final String s = String.format("%s.%s", pe.getQualifiedName(), qualifiedClassName);
        return s;
    }

    /**
     * Returns the instantiated DocumentedFeature that describes the doc for this class.
     *
     * @param clazz
     * @return DocumentedFeature, or null if this classDoc shouldn't be included/documented
     */
    public static DocumentedFeature getDocumentedFeatureForClass(final Class<?> clazz) {
        if (clazz != null && clazz.isAnnotationPresent(DocumentedFeature.class)) {
            return clazz.getAnnotation(DocumentedFeature.class);
        }
        else {
            return null;
        }
    }

    /**
     * Return the filename of the GATKDoc PHP that would be generated for Class.  This
     * does not guarantee that the docs exist, or that docs would actually be generated
     * for class (might not be annotated for documentation, for example).  But if
     * this class is documented, GATKDocs will write the docs to a file named as returned
     * by this function.
     *
     * @param c
     * @return
     */
    public static String phpFilenameForClass(Class<?> c) {
        return phpFilenameForClass(c, "php");
    }

    public static String phpFilenameForClass(Class<?> c, String extension) {
        return c.getName().replace(".", "_") + "." + extension;
    }

}