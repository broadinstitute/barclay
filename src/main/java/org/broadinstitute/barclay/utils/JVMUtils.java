package org.broadinstitute.barclay.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    /**
     * Find the field with the given name in the class.  Will inspect all fields, independent
     * of access level.
     * @param type Class in which to search for the given field.
     * @param fieldName Name of the field for which to search.
     * @return The field, or null if no such field exists.
     */
    public static Field findField(Class<?> type, String fieldName ) {
        while( type != null ) {
            Field[] fields = type.getDeclaredFields();
            for( Field field: fields ) {
                if( field.getName().equals(fieldName) )
                    return field;
            }
            type = type.getSuperclass();
        }
        return null;
    }

}
