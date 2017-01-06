package org.broadinstitute.barclay.help;

import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ProgramElementDoc;
import org.broadinstitute.barclay.utils.JVMUtils;

import java.lang.reflect.Field;

/**
 * Package protected - Methods in the class must ONLY be used by doclets, since the com.sun.javadoc.* classes
 * are not available on all systems, and we don't want the GATK proper to depend on them.
 */
class DocletUtils {

    protected static Class<?> getClassForDoc(ProgramElementDoc doc) throws ClassNotFoundException {
        return Class.forName(getClassName(doc, true));
    }

    protected static Field getFieldForFieldDoc(FieldDoc fieldDoc) {
        try {
            Class<?> clazz = getClassForDoc(fieldDoc.containingClass());
            return JVMUtils.findField(clazz, fieldDoc.name());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reconstitute the class name from the given class JavaDoc object.
     *
     * @param doc the Javadoc model for the given class.
     * @return The (string) class name of the given class.
     */
    protected static String getClassName(ProgramElementDoc doc, boolean binaryName) {
        PackageDoc containingPackage = doc.containingPackage();
        String className = doc.name();
        if (binaryName) {
            className = className.replaceAll("\\.", "\\$");
        }
        return containingPackage.name().length() > 0 ?
                String.format("%s.%s", containingPackage.name(), className) :
                String.format("%s", className);
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