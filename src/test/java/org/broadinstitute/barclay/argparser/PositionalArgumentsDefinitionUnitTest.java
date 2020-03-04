package org.broadinstitute.barclay.argparser;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class PositionalArgumentsDefinitionUnitTest {

    @Test(expectedExceptions = CommandLineException.class)
    public void testMinElements() {
        class MinElements {
            @PositionalArguments(minElements = 2)
            List<String> stringArg;
        }
        final MinElements o = new MinElements();
        final Field field = getFieldForFieldName(o, "stringArg");
        final PositionalArgumentDefinition def =
                new PositionalArgumentDefinition(field.getAnnotation(PositionalArguments.class), o, field);
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        def.setArgumentValues(clp, System.out, Arrays.asList(new String[] { "arg1"}));

        // we only validate minimum number of arguments on validation, after we've seen all of the values
        def.validateValues(clp);
    }

    @Test(expectedExceptions = CommandLineException.class)
    public void testMaxElements() {
        class MaxElements {
            @PositionalArguments(minElements = 1, maxElements=1)
            List<String> stringArg;
        }
        final MaxElements o = new MaxElements();
        final Field field = getFieldForFieldName(o, "stringArg");
        final PositionalArgumentDefinition def =
                new PositionalArgumentDefinition(field.getAnnotation(PositionalArguments.class), o, field);
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);

        // exceeding the maximum number of arguments is validated when args are added
        def.setArgumentValues(clp, System.out, Arrays.asList(new String[] { "arg1", "arg2", "arg3"}));
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testMinElementsGreaterThanMaxElements() {
        class MinGreaterThanMax {
            @PositionalArguments(minElements = 179, maxElements=3)
            List<String> stringArg;
        }
        final MinGreaterThanMax o = new MinGreaterThanMax();
        final Field field = getFieldForFieldName(o, "stringArg");
        new PositionalArgumentDefinition(field.getAnnotation(PositionalArguments.class), o, field);
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testNonCollection() {
        class NonCollectionPositional {
            @PositionalArguments
            int intArg = 3;
        }
        final NonCollectionPositional o = new NonCollectionPositional();
        final Field field = getFieldForFieldName(o, "intArg");
        new PositionalArgumentDefinition(field.getAnnotation(PositionalArguments.class), o, field);
    }

    @DataProvider(name="commandLineDisplayString")
    public Object[][] getCommandLineStringTestArgList() {
        return new Object[][] {
                { new String[] {}, "" },
                { new String[] { "arg1"}, "arg1" },
                { new String[] { "arg1", "arg2" }, "arg1 arg2" }
        };
    }

    @Test(dataProvider = "commandLineDisplayString")
    public void testGetCommandLineDisplayString(final String[] args, final String expecteDisplayString) {
        class MinAndMaxElements {
            @PositionalArguments(minElements = 1, maxElements = 3)
            List<String> stringArg;
        }
        final MinAndMaxElements o = new MinAndMaxElements();
        final Field field = getFieldForFieldName(o, "stringArg");
        final PositionalArgumentDefinition def =
                new PositionalArgumentDefinition(field.getAnnotation(PositionalArguments.class), o, field);
        def.setArgumentValues(new CommandLineArgumentParser(o), System.out, Arrays.asList(args));
        Assert.assertEquals(def.getCommandLineDisplayString(), expecteDisplayString);
    }

    private Field getFieldForFieldName(final Object argContainer, final String fieldName) {
        for (final Field field : CommandLineParserUtilities.getAllFields(argContainer.getClass())) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        throw new IllegalArgumentException("Can't find field");
    }

}
