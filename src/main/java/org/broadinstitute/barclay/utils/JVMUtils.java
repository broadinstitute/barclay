package org.broadinstitute.barclay.utils;

import java.lang.reflect.Modifier;

public class JVMUtils {
    /**
     * Is the specified class a concrete implementation of baseClass?
     * @param clazz Class to check.
     * @return True if clazz is concrete.  False otherwise.
     */
    public static boolean isConcrete( Class<?> clazz ) {
        return !Modifier.isAbstract(clazz.getModifiers()) &&
                !Modifier.isInterface(clazz.getModifiers());
    }

}
