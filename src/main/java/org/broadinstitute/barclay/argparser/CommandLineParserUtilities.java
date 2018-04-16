package org.broadinstitute.barclay.argparser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// package private
class CommandLineParserUtilities {

    // This can be moved into CommandlineArgumentParser once there is only one parser implementation.

    /**
     * Return a list of all {@link Field} in the class {@code clazz} and its base classes.
     * @param clazz class to interrogate for {@link Field}
     * @return list of all {@link Field} in the class {@code clazz} and its base classes
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        final List<Field> ret = new ArrayList<>();
        do {
            ret.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return ret;
    }

}
