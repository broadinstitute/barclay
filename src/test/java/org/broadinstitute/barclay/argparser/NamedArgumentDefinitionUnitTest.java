package org.broadinstitute.barclay.argparser;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NamedArgumentDefinitionUnitTest {

    @DataProvider(name="optionalOrRequiredTests")
    public Object[][] getOptionalOrRequiredTests() {
        return new Object[][] {

                // arg container, argument(field) name, is-optional, default value as string

                // primitive, optional
                { new Object() { @Argument(optional=true) int arg; }, "arg", true, "0" },

                // primitive, required
                // NOTE: non-intuitive case: it looks like its required, but isn't since it always has a value
                { new Object() { @Argument(optional=false) int arg; }, "arg", true, "0" },

                // reference, null, optional
                { new Object() { @Argument(optional=true) Integer arg; }, "arg", true, NamedArgumentDefinition.NULL_ARGUMENT_STRING},

                // reference, null, required
                { new Object() { @Argument(optional=false) Integer arg; }, "arg", false, NamedArgumentDefinition.NULL_ARGUMENT_STRING},

                // reference, initialized, optional
                { new Object() { @Argument(optional=true) Integer arg = Integer.valueOf(27); }, "arg", true, "27" },

                // reference, initialized, required
                // NOTE: non-intuitive case: it looks like its required but isn't since it has a value
                { new Object() { @Argument(optional=false) Integer arg = Integer.valueOf(27); }, "arg", true, "27" },

                // collection, null, optional
                { new Object() { @Argument(optional=true) List<String> arg; }, "arg", true, NamedArgumentDefinition.NULL_ARGUMENT_STRING},

                // collection, null, required
                { new Object() { @Argument(optional=false) List<String> arg; }, "arg", false, NamedArgumentDefinition.NULL_ARGUMENT_STRING},

                // collection, initialized-empty, optional
                { new Object() { @Argument(optional=true) List<String> arg = Collections.emptyList(); },
                        "arg", true, NamedArgumentDefinition.NULL_ARGUMENT_STRING},

                // collection, initialized-empty, required
                // NOTE: empty lists are treated as the same as an uninitialized list (not provided)
                { new Object() { @Argument(optional=false) List<String> arg = Collections.emptyList(); },
                        "arg", false, NamedArgumentDefinition.NULL_ARGUMENT_STRING},

                // collection, populated, optional
                { new Object() { @Argument(optional=true) List<String> arg = Arrays.asList(new String[] { "stuff" }); },
                        "arg", true, "[stuff]" },

                // collection, populated, required
                // NOTE: non-intuitive case: it looks like its required but isn't since it has a value
                { new Object() { @Argument(optional=false) List<String> arg = Arrays.asList(new String[] { "stuff", "more stuff" }); },
                        "arg", true, "[stuff, more stuff]" },
        };
    }

    @Test(dataProvider = "optionalOrRequiredTests")
    public void testIsOptional(
            final Object o,
            final String fieldName,
            final boolean expectedOptional,
            final String unused) {
        final Field field = getFieldForFieldName(o, fieldName);
        final NamedArgumentDefinition argDef =
            new NamedArgumentDefinition(field.getAnnotation(Argument.class), o, field, null);
        Assert.assertEquals(argDef.isOptional(), expectedOptional);
    }

    @Test(dataProvider = "optionalOrRequiredTests")
    public void testDefaultValueAsString(
            final Object o,
            final String fieldName,
            final boolean unused,
            final String expectedDefaultValueAsString) {
        final Field field = getFieldForFieldName(o, fieldName);
        final NamedArgumentDefinition argDef =
                new NamedArgumentDefinition(field.getAnnotation(Argument.class), o, field, null);
        Assert.assertEquals(argDef.getDefaultValueAsString(), expectedDefaultValueAsString);
    }

    @DataProvider(name="aliasTests")
    public Object[][] getAliasTests() {
        return new Object[][] {
                // arg container, arg(field) name, expected longname, expected alias list, expected alias display string
                { new Object() { @Argument int arg; },
                        "arg", "arg", Arrays.asList("arg"), "arg" },
                { new Object() { @Argument(shortName="shortName") int arg; },
                        "arg", "arg", Arrays.asList("shortName", "arg"), "shortName/arg" },
                { new Object() { @Argument(fullName="fullName") int arg; },
                        "arg", "fullName", Arrays.asList("fullName"), "fullName" },
                { new Object() { @Argument(shortName="shortName", fullName="fullName") int arg; },
                        "arg", "fullName", Arrays.asList("shortName", "fullName"), "shortName/fullName" },
        };
    }

    @Test(dataProvider = "aliasTests")
    public void testGetArgumentAliases(
            final Object o,
            final String fieldName,
            final String unusedExpectedLongName,
            final List<String> expectedAliases,
            final String unusedExpectedDisplayString) {
        final Field field = getFieldForFieldName(o, fieldName);
        final NamedArgumentDefinition argDef =
                new NamedArgumentDefinition(field.getAnnotation(Argument.class), o, field, null);
        Assert.assertEquals(argDef.getArgumentAliases(), expectedAliases);
    }

    @Test(dataProvider = "aliasTests")
    public void testGetArgumentAliasDisplayString(
            final Object o,
            final String fieldName,
            final String unusedExpectedLongName,
            final List<String> unusedExpectedAliases,
            final String expectedDisplayString) {
        final Field field = getFieldForFieldName(o, fieldName);
        final NamedArgumentDefinition argDef =
                new NamedArgumentDefinition(field.getAnnotation(Argument.class), o, field, null);
        Assert.assertEquals(argDef.getArgumentAliasDisplayString(), expectedDisplayString);
    }

    @Test(dataProvider = "aliasTests")
    public void testGetLongName(
            final Object o,
            final String fieldName,
            final String expectedLongName,
            final List<String> unusedExpectedAliases,
            final String unusedExpectedDisplayString) {
        final Field field = getFieldForFieldName(o, fieldName);
        final NamedArgumentDefinition argDef =
                new NamedArgumentDefinition(field.getAnnotation(Argument.class), o, field, null);
        Assert.assertEquals(argDef.getLongName(), expectedLongName);
    }

    private Field getFieldForFieldName(final Object argContainer, final String fieldName) {
        for (final Field field : CommandLineParserUtilities.getAllFields(argContainer.getClass())) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        throw new IllegalArgumentException("Can't find field");
    }

    public static class ArgumentLists {
        @Argument
        List<String> required;
        @Argument(optional = true)
        List<String> optional;
        @Argument
        List<String> defaultedRequired = new ArrayList<>(List.of("default"));
        @Argument(optional = true)
        List<String> defaultedOptional = new ArrayList<>(List.of("default"));
    }

    @DataProvider
    public Object[][] setArgumentValuesCollectionsTests() {
        return new Object[][]{
                {"required", Set.of(),
                        List.of("stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"optional", Set.of(),
                        List.of("stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"defaultedRequired", Set.of(),
                        List.of("stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"defaultedOptional", Set.of(),
                        List.of("stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"required", Set.of(),
                        List.of("null", "stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"optional", Set.of(),
                        List.of("null", "stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"defaultedRequired", Set.of(),
                        List.of("null", "stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"defaultedOptional", Set.of(),
                        List.of("null", "stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"required", Set.of(),
                        List.of("stuff", "null", "more stuff"), List.of("more stuff")},
                {"optional", Set.of(),
                        List.of("stuff", "null", "more stuff"), List.of("more stuff")},
                {"defaultedRequired", Set.of(),
                        List.of("stuff", "null", "more stuff"), List.of("more stuff")},
                {"defaultedOptional", Set.of(),
                        List.of("stuff", "null", "more stuff"), List.of("more stuff")},
                {"optional", Set.of(),
                        List.of("stuff", "more stuff", "null"), List.of()},
                {"defaultedOptional", Set.of(),
                        List.of("stuff", "more stuff", "null"), List.of()},

                {"required", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"optional", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"defaultedRequired", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "more stuff"), List.of("default", "stuff", "more stuff")},
                {"defaultedOptional", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "more stuff"), List.of("default", "stuff", "more stuff")},
                {"required", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("null", "stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"optional", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("null", "stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"defaultedRequired", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("null", "stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"defaultedOptional", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("null", "stuff", "more stuff"), List.of("stuff", "more stuff")},
                {"required", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "null", "more stuff"), List.of("more stuff")},
                {"optional", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "null", "more stuff"), List.of("more stuff")},
                {"defaultedRequired", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "null", "more stuff"), List.of("more stuff")},
                {"defaultedOptional", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "null", "more stuff"), List.of("more stuff")},
                {"optional", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "more stuff", "null"), List.of()},
                {"defaultedOptional", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "more stuff", "null"), List.of()},
        };
    }

    @Test(dataProvider = "setArgumentValuesCollectionsTests")
    public void testSetArgumentValuesCollections(
            final String fieldName,
            final Set<CommandLineParserOptions> parserOptions,
            final List<String> newValues,
            final List<String> expectedValues
    ) throws IllegalAccessException {
        final ArgumentLists argLists = new ArgumentLists();
        final Field field = getFieldForFieldName(argLists, fieldName);
        final NamedArgumentDefinition argDef =
                new NamedArgumentDefinition(field.getAnnotation(Argument.class), argLists, field, null);
        final CommandLineArgumentParser clp =
                new CommandLineArgumentParser(argLists, Collections.emptyList(), parserOptions);
        argDef.setArgumentValues(clp, System.out, newValues);
        Assert.assertEquals(field.get(argLists), expectedValues);
    }

    @DataProvider
    public Object[][] setArgumentValuesCollectionsFailures() {
        return new Object[][]{
                {"required", Set.of(),
                        List.of("stuff", "more stuff", "null")},
                {"defaultedRequired", Set.of(),
                        List.of("stuff", "more stuff", "null")},
                {"required", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "more stuff", "null")},
                {"defaultedRequired", Set.of(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                        List.of("stuff", "more stuff", "null")},
        };
    }

    @Test(dataProvider = "setArgumentValuesCollectionsFailures", expectedExceptions = CommandLineException.class)
    public void testSetArgumentValuesCollectionsFailures(
            final String fieldName,
            final Set<CommandLineParserOptions> parserOptions,
            final List<String> newValues
    ) {
        final ArgumentLists argLists = new ArgumentLists();
        final Field field = getFieldForFieldName(argLists, fieldName);
        final NamedArgumentDefinition argDef =
                new NamedArgumentDefinition(field.getAnnotation(Argument.class), argLists, field, null);
        final CommandLineArgumentParser clp =
                new CommandLineArgumentParser(argLists, Collections.emptyList(), parserOptions);
        argDef.setArgumentValues(clp, System.out, newValues);
    }
}
