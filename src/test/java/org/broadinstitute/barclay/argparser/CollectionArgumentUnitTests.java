package org.broadinstitute.barclay.argparser;

import org.broadinstitute.barclay.argparser.parseropt.CommandLineParserOption;
import org.broadinstitute.barclay.argparser.parseropt.ExpandCollectionExtensionOption;
import org.broadinstitute.barclay.argparser.parseropt.SwitchCommandLineParserOptions;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tests for arguments that are collections (not to be confused with ArgumentCollection).
 */
public class CollectionArgumentUnitTests {

    class UninitializedCollections {
        @Argument
        public List<String> LIST;
        @Argument
        public ArrayList<String> ARRAY_LIST;
        @Argument
        public HashSet<String> HASH_SET;
        @PositionalArguments
        public Collection<File> COLLECTION;
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
                        Collections.singleton(SwitchCommandLineParserOptions.APPEND_TO_COLLECTIONS), // append mode
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
            final Set<CommandLineParserOption> parserOptions,
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

    class InitializedCollections {
        @Argument
        public List<String> LIST = makeList("foo", "bar");
    }

    @DataProvider(name="initializedCollections")
    public Object[][] initializedCollections() {
        final String[] inputArgs = new String[] {"--LIST", "baz", "--LIST", "frob"};
        final String[] inputArgsWithNullAtStart = new String[] {"--LIST", "null", "--LIST", "baz", "--LIST", "frob"};
        final String[] inputArgsWithNullMidStream = new String[] {"--LIST", "baz", "--LIST", "null", "--LIST", "frob"};

        return new Object[][]{
                {
                    inputArgs,
                    Collections.singleton(SwitchCommandLineParserOptions.APPEND_TO_COLLECTIONS),
                    makeList("foo", "bar", "baz", "frob")       // original values retained
                },
                {
                    inputArgs,
                    Collections.emptySet(),
                    makeList("baz", "frob")                     // original values replaced
                },
                {
                    inputArgsWithNullAtStart,
                    Collections.singleton(SwitchCommandLineParserOptions.APPEND_TO_COLLECTIONS),
                    makeList("baz", "frob")                     // original values replaced
                },
                {
                    inputArgsWithNullAtStart,
                    Collections.emptySet(),
                    makeList("baz", "frob")                     // original values replaced
                },
                {
                    inputArgsWithNullMidStream,
                    Collections.singleton(SwitchCommandLineParserOptions.APPEND_TO_COLLECTIONS),
                    makeList("frob")                            // reset mid-stream via midstream null
                },
                {
                    inputArgsWithNullMidStream,
                    Collections.emptySet(),
                    makeList("frob")                            // reset mid-stream via midstream null
                },
                {
                    new String[]{},
                    Collections.singleton(SwitchCommandLineParserOptions.APPEND_TO_COLLECTIONS),
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
            final Set<CommandLineParserOption> parserOptions,
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
    // tests for .list files

    class CollectionForListFileArguments {
        @Argument
        public List<String> LIST = makeList("foo", "bar");

        @Argument
        public List<String> LIST2 = makeList("baz");
    }

    @DataProvider(name="listFileArguments")
    public Object[][] listFileArguments() {
        final String[] inputArgs = new String[] { "shmiggle0", "shmiggle1", "shmiggle2" };

        final CommandLineParserOption defaultExpandCollection = new ExpandCollectionExtensionOption();

        return new Object[][] {
                {
                        // replace mode
                        Collections.singleton(defaultExpandCollection),                          // parser options
                        inputArgs,                                                               // args
                        new String[] {"shmiggle0", "shmiggle1", "shmiggle2"},                    // expected result
                },
                {
                        // append mode
                        new HashSet<>(Arrays.asList(
                                defaultExpandCollection,
                                SwitchCommandLineParserOptions.APPEND_TO_COLLECTIONS)),         // parser options
                        inputArgs,                                                              // args
                        new String[] {"foo", "bar", "shmiggle0", "shmiggle1", "shmiggle2"},     // expected result
                },
        };
    }

    // Test that .list files populate collections with file contents, both mpdes
    @Test(dataProvider="listFileArguments")
    public void testCollectionFromListFile(
            final Set<CommandLineParserOption> parserOptions,
            final String [] argList,
            final String[] expectedList) throws IOException
    {
        final File argListFile = createListArgumentFile("argListFile", argList);

        // use a single file argument
        final CollectionForListFileArguments o = new CollectionForListFileArguments();
        final CommandLineParser clp = new CommandLineArgumentParser(
                o,
                Collections.emptyList(),
                parserOptions
        );

        final String[] args = {"--LIST", argListFile.getAbsolutePath()};
        Assert.assertTrue(clp.parseArguments(System.err, args));

        Assert.assertEquals(o.LIST, makeList(expectedList));
    }

    @Test
    public void testDoNotExpandCollectionListFile() {
        final CollectionForListFileArguments o = new CollectionForListFileArguments();
        final CommandLineParser clp = new CommandLineArgumentParser(
                o,
                Collections.emptyList(),
                Collections.emptySet()
        );

        final String noListFile = "no_arg_list_file" + ExpandCollectionExtensionOption.DEFAULT_COLLECTION_LIST_FILE_EXTENSION;

        final String[] args =  {"--LIST", noListFile};
        Assert.assertTrue(clp.parseArguments(System.err, args));

        Assert.assertEquals(o.LIST, makeList(noListFile));
    }

    @DataProvider(name="mixedListFileArguments")
    public Object[][] mixedListFileArguments() {
        final String[] inputArgList1 = { "shmiggle0", "shmiggle1", "shmiggle2" };
        final String[] inputArgList2 = { "test2_shmiggle0", "test2_shmiggle1", "test2_shmiggle2" };

        final CommandLineParserOption defaultExpandCollection = new ExpandCollectionExtensionOption();

        return new Object[][] {
                {
                        // replace mode
                        Collections.singleton(defaultExpandCollection),         // parser options
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
                        new HashSet<>(Arrays.asList(
                                defaultExpandCollection,
                                SwitchCommandLineParserOptions.APPEND_TO_COLLECTIONS)),         // parser options
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

    // Test that .list files intermixed with explicit command line values populate collections correctly, both mpdes
    @Test(dataProvider="mixedListFileArguments")
    public void testCollectionFromListFileMixed(
            final Set<CommandLineParserOption> parserOptions,
            final String [] argList1,
            final String [] argList2,
            final String[] expectedList1,
            final String[] expectedList2
    ) throws IOException {

        // use two file arguments
        File listFile = createListArgumentFile("testFile1", argList1);
        File listFile2 = createListArgumentFile("testFile2", argList2);
        final CollectionForListFileArguments o = new CollectionForListFileArguments();
        final CommandLineParser clp = new CommandLineArgumentParser(
                o,
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
        Assert.assertEquals(o.LIST, makeList(expectedList1));
        Assert.assertEquals(o.LIST2, makeList(expectedList2));
    }

    class UninitializedCollectionThatCannotBeAutoInitializedArguments {
        @Argument
        public Set<String> SET;
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testCollectionThatCannotBeAutoInitialized() {
        final UninitializedCollectionThatCannotBeAutoInitializedArguments o =
                new UninitializedCollectionThatCannotBeAutoInitializedArguments();
        new CommandLineArgumentParser(o);
    }

    //////////////////////////////////////////////////////////////////
    // Helper methods

    private File createListArgumentFile(final String fileName, final String[] argList) throws IOException {
        final File listFile = File.createTempFile(fileName, ".args");
        listFile.deleteOnExit();
        try (final PrintWriter writer = new PrintWriter(listFile)) {
            Arrays.stream(argList).forEach(arg -> writer.println(arg));
        }
        return listFile;
    }

    public static List<String> makeList(final String... list) {
        final List<String> result = new ArrayList<>();
        Collections.addAll(result, list);
        return result;
    }

}
