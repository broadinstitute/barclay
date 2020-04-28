package org.broadinstitute.barclay.help;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Methods for handling transformations from Java (arg names, Java argument types) to WDL-compatible names and types.
 */
public class WDLTransforms {
    private final static Set<String> wdlReservedWords =
            Stream.of(
                    "after",
                    "alias",
                    "Array",
                    "as",
                    "boolean",
                    "call",
                    "else",
                    "equal",
                    "float",
                    "if",
                    "import",
                    "in",
                    "input",
                    "integer",
                    "meta",
                    "none",
                    "not",
                    "null",
                    "object",
                    "output",
                    "parameter_meta",
                    "runtime",
                    "scatter",
                    "string",
                    "struct",
                    "task",
                    "then",
                    "version",
                    "workflow").collect(Collectors.toCollection(HashSet::new));

    // Map of Java argument types that the WDL generator knows how to convert to a WDL type, along with the
    // corresponding string substitution that needs to be run on the (Barclay-generated) string that describes
    // the type. From a purely string perspective, some of these transforms are no-ops in that no actual
    // conversion is required because the type names are identical in Java and WDL (i.e, File->File or
    // String->String), but they're included here for completeness, and to document the allowed type transitions.
    private final static Map<Class<?>, ImmutablePair<String, String>> javaToWDLTypeMap =
            new HashMap<Class<?>, ImmutablePair<String, String>>() {
                private static final long serialVersionUID = 1L;

                {
                    put(String.class, new ImmutablePair<>("String", "String"));

                    // primitive (or boxed primitive) types
                    put(boolean.class, new ImmutablePair<>("boolean", "Boolean"));
                    put(Boolean.class, new ImmutablePair<>("Boolean", "Boolean"));

                    put(byte.class, new ImmutablePair<>("byte", "Int"));
                    put(Byte.class, new ImmutablePair<>("Byte", "Int"));

                    put(int.class, new ImmutablePair<>("int", "Int"));
                    put(Integer.class, new ImmutablePair<>("Integer", "Int"));

                    //NOTE: WDL has no long type, map to Int
                    put(long.class, new ImmutablePair<>("long", "Int"));
                    put(Long.class, new ImmutablePair<>("Long", "Int"));

                    put(float.class, new ImmutablePair<>("float", "Float"));
                    put(Float.class, new ImmutablePair<>("Float", "Float"));
                    put(double.class, new ImmutablePair<>("double", "Float"));
                    put(Double.class, new ImmutablePair<>("Double", "Float"));

                    // File/Path Types
                    put(File.class, new ImmutablePair<>("File", "File"));
                }
            };

    // Map of Java collection argument types that the WDL generator knows how to convert to a WDL type, along with the
    // corresponding string substitution that needs to be run on the (Barclay-generated) string that describes
    // the type.
    private final static Map<Class<?>, ImmutablePair<String, String>> javaCollectionToWDLCollectionTypeMap =
            new HashMap<Class<?>, ImmutablePair<String, String>>() {
                private static final long serialVersionUID = 1L;

                {
                    put(List.class, new ImmutablePair<>("List", "Array"));
                    //TODO: there are a few GATK @Arguments that are typed as "ArrayList", fix these...
                    put(ArrayList.class, new ImmutablePair<>("ArrayList", "Array"));
                    put(Set.class, new ImmutablePair<>("Set", "Array"));
                    put(EnumSet.class, new ImmutablePair<>("EnumSet", "Array"));
                }
            };

    /**
     * Mangle {@code candidateName} if it is not WDL-compatible (ie. is a WDL reserved word, or contains
     * embedded "-" characters) to prevent WDL compiler errors.
     *
     * @param candidateArgName
     * @return mangled name if {@code candidateName} is not WDL-compatible, otherwise {@code candidateName}
     */
    public static String transformJavaNameToWDLName(final String candidateArgName) {
        return transformWDLReservedWord(candidateArgName.replace("-", "_"));
    }

    /**
     * Mangle {@code candidateName} if it is a WDL reserved word to prevent WDL compiler errors.
     *
     * @param candidateArgName
     * @return mangled name if {@code candidateName} is a WDL reserved word, otherwise {@code candidateName}
     */
    public static String transformWDLReservedWord(final String candidateArgName) {
        return wdlReservedWords.contains(candidateArgName) ?
                candidateArgName + "_arg" :
                candidateArgName;
    }

    /**
     * Given an argument class, return a String pair representing the string that should be replaced (the Java
     * type), and the string to substitute (the corresponding WDL type), i.e., for an argument with type Java
     * Integer.class, return the Pair ("Integer", "Int") to convert from the Java type to the corresponding WDL
     * type.
     *
     * @param argumentClass Class of the argument being converter
     * @return a String pair representing the original and replacement type text, or null if no conversion is available
     */
    public static Pair<String, String> transformToWDLType(final Class<?> argumentClass) {
        return WDLTransforms.javaToWDLTypeMap.get(argumentClass);
    }

    /**
     * Given a Java collection class, return a String pair representing the string that should be replaced (the
     * Java type),
     * and the string to substitute (the corresponding WDL type), i.e., for an argument with type Java List.class,
     * return the Pair ("List", "Array") to convert from the Java type to the corresponding WDL collection type.
     *
     * @param argumentCollectionClass collection Class of the argument being converter
     * @return a String pair representing the original and replacement type text, or null if no conversion is available
     */
    public static Pair<String, String> tranformToWDLCollectionType(final Class<?> argumentCollectionClass) {
        return javaCollectionToWDLCollectionTypeMap.get(argumentCollectionClass);
    }

}


