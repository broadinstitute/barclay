package org.broadinstitute.barclay.argparser;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Tests for arguments that are collections (not to be confused with ArgumentCollection).
 */
public class CollectionArgumentUnitTests {

    private class UninitializedCollections {
        @Argument
        private List<String> LIST;
        @Argument
        private ArrayList<String> ARRAY_LIST;
        @Argument
        private HashSet<String> HASH_SET;
        @PositionalArguments
        private Collection<File> COLLECTION;
    }

    @DataProvider(name="uninitializedCollections")
    public Object[][] uninitializedCollections() {
        String[] inputArgs = new String[] {"--LIST", "L1", "--LIST", "L2", "--ARRAY_LIST", "S1", "--HASH_SET", "HS1", "P1", "P2"};

        List<File> expectedFileList = new ArrayList<>();
        expectedFileList.add(new File("P1"));
        expectedFileList.add(new File("P2"));

        return new Object[][] {
                // for these two tests, we expect the same results since both append and replace modes have
                // the same behavior on collections with no initial values
                {
                        inputArgs,
                        Collections.EMPTY_SET,  // replace mode
                        makeList("L1", "L2"),
                        makeList("S1"),
                        makeList("HS1"),
                        expectedFileList
                },
                {
                        inputArgs,
                        Collections.singleton(CommandLineParserOptions.APPEND_TO_COLLECTIONS), // append mode
                        makeList("L1", "L2"),
                        makeList("S1"),
                        makeList("HS1"),
                        expectedFileList
                }
        };
    }

    @Test(dataProvider="uninitializedCollections")
    public void testUninitializedCollections(
            final String[] args,
            final Set<CommandLineParserOptions> parserOptions,
            final List<String> expectedList,
            final List<String> expectedArrayList,
            final List<String> expectedHashSet,
            final List<File> expectedCollection)
    {
        final UninitializedCollections o = new UninitializedCollections();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o, Collections.emptyList(), parserOptions);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(o.LIST, expectedList);
        Assert.assertEquals(o.ARRAY_LIST, expectedArrayList);
        Assert.assertEquals(o.HASH_SET, expectedHashSet);
        Assert.assertEquals(o.COLLECTION, expectedCollection);
    }

    private class InitializedCollections {
        @Argument
        private List<String> LIST = makeList("foo", "bar");
    }

    @DataProvider(name="initializedCollections")
    public Object[][] initializedCollections() {
        final String[] inputArgs = new String[] {"--LIST", "baz", "--LIST", "frob"};
        final String[] inputArgsWithNullAtStart = new String[] {"--LIST", "null", "--LIST", "baz", "--LIST", "frob"};
        final String[] inputArgsWithNullMidStream = new String[] {"--LIST", "baz", "--LIST", "null", "--LIST", "frob"};

        return new Object[][]{
                {
                    inputArgs,
                    Collections.singleton(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                    makeList("foo", "bar", "baz", "frob")       // original values retained
                },
                {
                    inputArgs,
                    Collections.emptySet(),
                    makeList("baz", "frob")                     // original values replaced
                },
                {
                    inputArgsWithNullAtStart,
                    Collections.singleton(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                    makeList("baz", "frob")                     // original values replaced
                },
                {
                    inputArgsWithNullAtStart,
                    Collections.emptySet(),
                    makeList("baz", "frob")                     // original values replaced
                },
                {
                    inputArgsWithNullMidStream,
                    Collections.singleton(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                    makeList("frob")                            // reset mid-stream via midstream null
                },
                {
                    inputArgsWithNullMidStream,
                    Collections.emptySet(),
                    makeList("frob")                            // reset mid-stream via midstream null
                },
                {
                    new String[]{},
                    Collections.singleton(CommandLineParserOptions.APPEND_TO_COLLECTIONS),
                    makeList("foo", "bar")
                },
                {
                    new String[]{},
                    Collections.singleton(Collections.emptySet()),
                    makeList("foo", "bar")
                }
        };
    }

    @Test(dataProvider="initializedCollections")
    public void testInitializedCollections(
            final String[] args,
            final Set<CommandLineParserOptions> parserOptions,
            final List<String> expectedResult) {
        final InitializedCollections o = new InitializedCollections();
        final CommandLineParser clp = new CommandLineArgumentParser(
                o,
                Collections.emptyList(),
                parserOptions
        );
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(o.LIST, expectedResult);
    }

    //////////////////////////////////////////////////////////////////
    // tests for .list/.args file expansion

    private abstract class CollectionForListFileArguments {
        abstract List<String> getList1();
        abstract List<String> getList2();
    }

    private class EnabledListFileArguments extends CollectionForListFileArguments {

        @Argument
        private List<String> LIST = makeList("foo", "bar");

        @Argument
        private List<String> LIST2 = makeList("baz");


        public List<String> getList1() { return LIST; };
        public List<String> getList2() { return LIST2; };
    }

    private class SuppressedListFileArguments extends CollectionForListFileArguments {

        @Argument(suppressFileExpansion = true)
        private List<String> LIST = makeList("foo", "bar");

        @Argument(suppressFileExpansion = true)
        private List<String> LIST2 = makeList("baz");

        public List<String> getList1() { return LIST; };
        public List<String> getList2() { return LIST2; };
    }

    @DataProvider(name="expansionFileArguments")
    public Object[][] expansionFileArguments() throws IOException {
        final String[] expansionFileContents = new String[] { "shmiggle0", "shmiggle1", "shmiggle2" };
        return new Object[][] {

                // enabled file expansion tests
                {
                        // replace mode
                        new EnabledListFileArguments(),
                        Collections.EMPTY_SET,                                                  // parser options
                        createTemporaryExpansionFile("enabledExpansion",
                                CommandLineArgumentParser.EXPANSION_FILE_EXTENSIONS_ARGS),// expansion file
                        expansionFileContents,                                                  // args for expansion file
                        new String[] {"shmiggle0", "shmiggle1", "shmiggle2"},                   // expected result
                        false                                                                   // expect temp file name
                },
                {
                        // append mode
                        new EnabledListFileArguments(),
                        Collections.singleton(CommandLineParserOptions.APPEND_TO_COLLECTIONS),  // parser options
                        createTemporaryExpansionFile("enabledExpansion",
                                CommandLineArgumentParser.EXPANSION_FILE_EXTENSIONS_ARGS),// expansion file
                        expansionFileContents,                                                  // args for expansion file
                        new String[] {"foo", "bar", "shmiggle0", "shmiggle1", "shmiggle2"},     // expected result
                        false                                                                   // expect temp file name
                },
                {
                        // replace mode
                        new EnabledListFileArguments(),
                        Collections.EMPTY_SET,                                                  // parser options
                        createTemporaryExpansionFile("enabledExpansion",
                                CommandLineArgumentParser.EXPANSION_FILE_EXTENSION_LIST),// expansion file
                        expansionFileContents,                                                  // args for expansion file
                        new String[] {"shmiggle0", "shmiggle1", "shmiggle2"},                   // expected result
                        false                                                                   // expect temp file name
                },
                {
                        // append mode
                        new EnabledListFileArguments(),
                        Collections.singleton(CommandLineParserOptions.APPEND_TO_COLLECTIONS),  // parser options
                        createTemporaryExpansionFile("enabledExpansion",
                                CommandLineArgumentParser.EXPANSION_FILE_EXTENSION_LIST),// expansion file
                        expansionFileContents,                                                  // args for expansion file
                        new String[] {"foo", "bar", "shmiggle0", "shmiggle1", "shmiggle2"},     // expected result
                        false                                                                   // expect temp file name
                },

                // suppressed file expansion file tests
                {
                        // replace mode
                        new SuppressedListFileArguments(),
                        Collections.EMPTY_SET,                                                  // parser options
                        createTemporaryExpansionFile("enabledExpansion",
                                CommandLineArgumentParser.EXPANSION_FILE_EXTENSIONS_ARGS),// expansion file
                        expansionFileContents,                                                  // args for expansion file
                        new String[] {},                                                        // expected result
                        true,
                },
                {
                        // append mode
                        new SuppressedListFileArguments(),
                        Collections.singleton(CommandLineParserOptions.APPEND_TO_COLLECTIONS),  // parser options
                        createTemporaryExpansionFile("enabledExpansion",
                                CommandLineArgumentParser.EXPANSION_FILE_EXTENSIONS_ARGS),// expansion file
                        expansionFileContents,                                                  // args for expansion file
                        new String[] {"foo", "bar"},                                            // expected result
                        true,
                },
                {
                        // replace mode
                        new SuppressedListFileArguments(),
                        Collections.EMPTY_SET,                                                  // parser options
                        createTemporaryExpansionFile("enabledExpansion",
                                CommandLineArgumentParser.EXPANSION_FILE_EXTENSION_LIST),// expansion file
                        expansionFileContents,                                                  // args for expansion file
                        new String[] {},                                                        // expected result
                        true,
                },
                {
                        // append mode
                        new SuppressedListFileArguments(),
                        Collections.singleton(CommandLineParserOptions.APPEND_TO_COLLECTIONS),  // parser options
                        createTemporaryExpansionFile("enabledExpansion",
                                CommandLineArgumentParser.EXPANSION_FILE_EXTENSION_LIST),// expansion file
                        expansionFileContents,                                                  // args for expansion file
                        new String[] {"foo", "bar"},                                            // expected result
                        true,
                },
        };
    }

    // Test that .list and .args files populate collections with file contents, both parser modes
    @Test(dataProvider="expansionFileArguments")
    public void testCollectionFromExpansionFile(
            final CollectionForListFileArguments argumentContainer,
            final Set<CommandLineParserOptions> parserOptions,
            final File argListFile,
            final String [] expansionFileContents,
            final String[] expectedList,
            final boolean expectFileNameInOutput    // true when expansion is suppressed
    ) throws IOException
    {
        populateExpansionFile(argListFile, expansionFileContents);

        // use a single file argument
        final CommandLineParser clp = new CommandLineArgumentParser(
                argumentContainer,
                Collections.emptyList(),
                parserOptions
        );

        final String[] args = {"--LIST", argListFile.getAbsolutePath()};
        Assert.assertTrue(clp.parseArguments(System.err, args));

        // if the results are expected to include the file name because expansion was suppressed,
        // add the filename to the list of expected outputs
        final List<String> actualResult = argumentContainer.getList1();
        final List<String> expectedResult;
        if (expectFileNameInOutput) {
            expectedResult = makeList(expectedList);
            expectedResult.add(argListFile.getAbsolutePath());
        } else {
            expectedResult = makeList(expectedList);
        }
        Assert.assertEquals(argumentContainer.getList1(), expectedResult);
    }

    private class PositionalCollection {
        @PositionalArguments
        private List<String> positionalCollection = new ArrayList<>();
    }

    @Test
    public void testPositionalCollectionFromExpansionFile() throws IOException
    {
        final PositionalCollection positionalCollection = new PositionalCollection();
        final CommandLineParser clp = new CommandLineArgumentParser(positionalCollection);

        final File expansionFile = createTemporaryExpansionFile("enabledExpansion",
                CommandLineArgumentParser.EXPANSION_FILE_EXTENSION_LIST);

        final String[] expansionFileContentsArray = new String[] {"value1", "value2", "value3"};
        populateExpansionFile(expansionFile, expansionFileContentsArray);

        // use a single file argument
        Assert.assertTrue(clp.parseArguments(System.err, new String[] { expansionFile.getAbsolutePath() }));

        Assert.assertEquals(positionalCollection.positionalCollection, Arrays.asList(expansionFileContentsArray));
    }

    @DataProvider(name="mixedListFileArguments")
    public Object[][] mixedListFileArguments() {
        final String[] inputArgList1 = { "shmiggle0", "shmiggle1", "shmiggle2" };
        final String[] inputArgList2 = { "test2_shmiggle0", "test2_shmiggle1", "test2_shmiggle2" };
        return new Object[][] {
                {
                        // replace mode
                        new EnabledListFileArguments(),
                        Collections.EMPTY_SET,                                  // parser options
                        inputArgList1,                                          // args list 1
                        inputArgList2,                                          // args list 2
                        new String[] {"shmiggle0", "shmiggle1", "shmiggle2"},   // expected result list 1
                        new String[] {                                          // expected result list 2
                                "commandLineValue",
                                "test2_shmiggle0",
                                "test2_shmiggle1",
                                "test2_shmiggle2",
                                "anotherCommandLineValue"
                        },
                },
                {
                        // append mode
                        new EnabledListFileArguments(),
                        Collections.singleton(CommandLineParserOptions.APPEND_TO_COLLECTIONS),  // parser options
                        inputArgList1,                                                          // args list 1
                        inputArgList2,                                                          // args list 2
                        new String[] {"foo", "bar", "shmiggle0", "shmiggle1", "shmiggle2"},     // expected result list 1
                        new String[] {                                                          // expected result list 2
                                "baz",
                                "commandLineValue",
                                "test2_shmiggle0",
                                "test2_shmiggle1",
                                "test2_shmiggle2",
                                "anotherCommandLineValue"
                        },
                },
        };
    }

    // Test that .list files intermixed with explicit command line values populate collections correctly, both modes
    @Test(dataProvider="mixedListFileArguments")
    public void testCollectionFromListFileMixed(
            final CollectionForListFileArguments argumentContainer,
            final Set<CommandLineParserOptions> parserOptions,
            final String [] argList1,
            final String [] argList2,
            final String[] expectedList1,
            final String[] expectedList2
    ) throws IOException {

        // use two file arguments
        final File listFile = createTemporaryExpansionFile("testFile1", CommandLineArgumentParser.EXPANSION_FILE_EXTENSIONS_ARGS);
        populateExpansionFile(listFile, argList1);
        final File listFile2 = createTemporaryExpansionFile("testFile2", CommandLineArgumentParser.EXPANSION_FILE_EXTENSION_LIST);
        populateExpansionFile(listFile2, argList2);
        final CommandLineParser clp = new CommandLineArgumentParser(
                argumentContainer,
                Collections.emptyList(),
                parserOptions
        );

        // mix command line values and file values
        final String[] args = new String[]{
                "--LIST2", "commandLineValue",
                "--LIST", listFile.getAbsolutePath(),
                "--LIST2", listFile2.getAbsolutePath(),
                "--LIST2", "anotherCommandLineValue"};

        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(argumentContainer.getList1(), makeList(expectedList1));
        Assert.assertEquals(argumentContainer.getList2(), makeList(expectedList2));
    }

    @Test
    public void testGetCommandLineWithExpansionFile() throws IOException{
        final File expansionFile = createTemporaryExpansionFile("expansionCommandLine", ".txt");
        // mix command line values and file values
        final String[] args = new String[] {
                "--LIST", "commandLineValue",
                "--LIST", expansionFile.getAbsolutePath(),
                "--LIST2", "anotherCommandLineValue"};

        final EnabledListFileArguments fo = new EnabledListFileArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        Assert.assertTrue(clp.parseArguments(System.err, args));
         final String expectedCommandLine =
                 String.format("EnabledListFileArguments --LIST commandLineValue --LIST %s --LIST2 anotherCommandLineValue", expansionFile.getAbsolutePath());
        Assert.assertEquals(clp.getCommandLine(), expectedCommandLine);
    }

    private class NonCollectionFileExpansionSuppression {
        @Argument(suppressFileExpansion = true)
        private int bogusFileExpansionArg;
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testRejectNonCollectionFileExpansionSuppression() {
        final NonCollectionFileExpansionSuppression fo = new NonCollectionFileExpansionSuppression();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
    }

    private class UninitializedCollectionThatCannotBeAutoInitializedArguments {
        @Argument
        private Set<String> SET;
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testCollectionThatCannotBeAutoInitialized() {
        final UninitializedCollectionThatCannotBeAutoInitializedArguments o =
                new UninitializedCollectionThatCannotBeAutoInitializedArguments();
        new CommandLineArgumentParser(o);
    }

    //////////////////////////////////////////////////////////////////
    // Helper methods

    private File createTemporaryExpansionFile(final String fileNameRoot, final String expansionExtension) throws IOException {
        final File listFile = File.createTempFile(fileNameRoot, expansionExtension);
        listFile.deleteOnExit();
        return listFile;
    }

    private File populateExpansionFile(final File expansionFile, final String[] argList) throws IOException {
        try (final PrintWriter writer = new PrintWriter(expansionFile)) {
            Arrays.stream(argList).forEach(arg -> writer.println(arg));
        }
        return expansionFile;
    }

    public static List<String> makeList(final String... list) {
        final List<String> result = new ArrayList<>();
        Collections.addAll(result, list);
        return result;
    }

}
