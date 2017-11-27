/*
 * The MIT License
 *
 * Copyright (c) 2009 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.broadinstitute.barclay.argparser;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.*;
import java.util.*;

public class LegacyCommandLineArgumentParserTest {

    enum FrobnicationFlavor {
        FOO, BAR, BAZ
    }

    @CommandLineProgramProperties(
            summary = "Usage: frobnicate [options] input-file output-file\n\nRead input-file, frobnicate it, and write frobnicated results to output-file\n",
            oneLineSummary = "Read input-file, frobnicate it, and write frobnicated results to output-file",
            programGroup = TestProgramGroup.class
    )
    class FrobnicateOptions {

        @PositionalArguments(minElements = 2, maxElements = 2)
        public List<File> positionalArguments = new ArrayList<File>();

        @Argument(shortName = "T", doc = "Frobnication threshold setting.")
        public Integer FROBNICATION_THRESHOLD = 20;

        @Argument
        public FrobnicationFlavor FROBNICATION_FLAVOR;

        @Argument(doc = "Allowed shmiggle types.", minElements = 1, maxElements = 3)
        public List<String> SHMIGGLE_TYPE = new ArrayList<String>();

        @Argument
        public Boolean TRUTHINESS;
    }

    @CommandLineProgramProperties(
            summary = "Usage: frobnicate [options] input-file output-file\n\nRead input-file, frobnicate it, and write frobnicated results to output-file\n",
            oneLineSummary = "Read input-file, frobnicate it, and write frobnicated results to output-file",
            programGroup = TestProgramGroup.class
    )
    class FrobnicateOptionsWithNullList {

        @PositionalArguments(minElements = 2, maxElements = 2)
        public List<File> positionalArguments = new ArrayList<File>();

        @Argument(shortName = "T", doc = "Frobnication threshold setting.")
        public Integer FROBNICATION_THRESHOLD = 20;

        @Argument
        public FrobnicationFlavor FROBNICATION_FLAVOR;

        @Argument(doc = "Allowed shmiggle types.", minElements = 0, maxElements = 3)
        public List<String> SHMIGGLE_TYPE = new ArrayList<String>();

        @Argument
        public Boolean TRUTHINESS;
    }

    @CommandLineProgramProperties(
            summary = "Usage: framistat [options]\n\nCompute the plebnick of the freebozzle.\n",
            oneLineSummary = "ompute the plebnick of the freebozzle",
            programGroup = TestProgramGroup.class
    )
    class OptionsWithoutPositional {
        public static final int DEFAULT_FROBNICATION_THRESHOLD = 20;
        @Argument(shortName = "T", doc = "Frobnication threshold setting.")
        public Integer FROBNICATION_THRESHOLD = DEFAULT_FROBNICATION_THRESHOLD;

        @Argument
        public FrobnicationFlavor FROBNICATION_FLAVOR;

        @Argument(doc = "Allowed shmiggle types.", minElements = 1, maxElements = 3)
        public List<String> SHMIGGLE_TYPE = new ArrayList<String>();

        @Argument
        public Boolean TRUTHINESS;
    }

    class OptionsWithCaseClash {
        @Argument
        public String FROB;
        @Argument
        public String frob;
    }

    class OptionsWithSameShortName {
        @Argument(shortName = "SAME_SHORT_NAME", optional = true)
        public String SAME_SHORT_NAME;
        @Argument(shortName = "SOMETHING_ELSE", optional = true)
        public String DIFF_SHORT_NAME;
    }

    class MutexOptions {
        @Argument(mutex = {"M", "N", "Y", "Z"})
        public String A;
        @Argument(mutex = {"M", "N", "Y", "Z"})
        public String B;
        @Argument(mutex = {"A", "B", "Y", "Z"})
        public String M;
        @Argument(mutex = {"A", "B", "Y", "Z"})
        public String N;
        @Argument(mutex = {"A", "B", "M", "N"})
        public String Y;
        @Argument(mutex = {"A", "B", "M", "N"})
        public String Z;

    }

    @Test
    public void testUsage() {
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        clp.usage(false, true);
    }

    @Test
    public void testUsageWithDefault() {
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        clp.usage(true, true);
    }

    @Test
    public void testUsageWithoutPositional() {
        final OptionsWithoutPositional fo = new OptionsWithoutPositional();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        clp.usage(false, true);
    }

    @Test
    public void testUsageWithoutPositionalWithDefault() {
        final OptionsWithoutPositional fo = new OptionsWithoutPositional();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        clp.usage(true, true);
    }

    @CommandLineProgramProperties(
            summary = "[oscillation_frequency]\n\nResets oscillation frequency.\n",
            oneLineSummary = "Reset oscillation frequency.",
            programGroup = TestProgramGroup.class
    )
    @BetaFeature
    public class BetaTool {
    }

    @Test
    public void testBetaFeatureUsage() {
        final BetaTool eo = new BetaTool();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(eo);
        final String out = clp.usage(false, false); // without common/hidden args
        final int reqIndex = out.indexOf(LegacyCommandLineArgumentParser.BETA_PREFIX);
        Assert.assertEquals(reqIndex, 0);
    }

    @CommandLineProgramProperties(
            summary = "Experimental.\n",
            oneLineSummary = "Experimental feature",
            programGroup = TestProgramGroup.class
    )
    @ExperimentalFeature
    public class ExperimentalTool {
    }

    @Test
    public void testExperimentalFeatureUsage() {
        final ExperimentalTool eo = new ExperimentalTool();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(eo);
        final String out = clp.usage(false, false); // without common/hidden args
        final int reqIndex = out.indexOf(LegacyCommandLineArgumentParser.EXPERIMENTAL_PREFIX);
        Assert.assertEquals(reqIndex, 0);
    }

    @CommandLineProgramProperties(
            summary = "Experimental and Beta.\n",
            oneLineSummary = "Experimental and Beta feature",
            programGroup = TestProgramGroup.class
    )
    @BetaFeature
    @ExperimentalFeature
    public class ExperimentalAndBetaTool {
    }

    @Test(expectedExceptions= CommandLineException.CommandLineParserInternalException.class)
    public void testBetaExperimentalMutex() {
        final ExperimentalAndBetaTool eo = new ExperimentalAndBetaTool();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(eo);
    }

    /**
     * If the short name is set to be the same as the long name we still want the argument to appear in the commandLine.
     */
    @Test
    public void testForIdenticalShortName() {
        final String[] args = {
                "SAME_SHORT_NAME=FOO",
                "SOMETHING_ELSE=BAR"
        };
        final OptionsWithSameShortName fo = new OptionsWithSameShortName();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        clp.parseArguments(System.err, args);
        final String commandLine = clp.getCommandLine();
        Assert.assertTrue(commandLine.contains("DIFF_SHORT_NAME"));
        Assert.assertTrue(commandLine.contains("SAME_SHORT_NAME"));
    }


    @Test
    public void testPositive() {
        final String[] args = {
                "T=17",
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "SHMIGGLE_TYPE=shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(fo.positionalArguments.size(), 2);
        final File[] expectedPositionalArguments = {new File("positional1"), new File("positional2")};
        Assert.assertEquals(fo.positionalArguments.toArray(), expectedPositionalArguments);
        Assert.assertEquals(fo.FROBNICATION_THRESHOLD.intValue(), 17);
        Assert.assertEquals(fo.FROBNICATION_FLAVOR, FrobnicationFlavor.BAR);
        Assert.assertEquals(fo.SHMIGGLE_TYPE.size(), 2);
        final String[] expectedShmiggleTypes = {"shmiggle1", "shmiggle2"};
        Assert.assertEquals(fo.SHMIGGLE_TYPE.toArray(), expectedShmiggleTypes);
        Assert.assertFalse(fo.TRUTHINESS);
    }

    /**
     * Allow a whitespace btw equal sign and option value.
     */
    @Test
    public void testPositiveWithSpaces() {
        final String[] args = {
                "T=", "17",
                "FROBNICATION_FLAVOR=", "BAR",
                "TRUTHINESS=", "False",
                "SHMIGGLE_TYPE=", "shmiggle1",
                "SHMIGGLE_TYPE=", "shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(fo.positionalArguments.size(), 2);
        final File[] expectedPositionalArguments = {new File("positional1"), new File("positional2")};
        Assert.assertEquals(fo.positionalArguments.toArray(), expectedPositionalArguments);
        Assert.assertEquals(fo.FROBNICATION_THRESHOLD.intValue(), 17);
        Assert.assertEquals(fo.FROBNICATION_FLAVOR, FrobnicationFlavor.BAR);
        Assert.assertEquals(fo.SHMIGGLE_TYPE.size(), 2);
        final String[] expectedShmiggleTypes = {"shmiggle1", "shmiggle2"};
        Assert.assertEquals(fo.SHMIGGLE_TYPE.toArray(), expectedShmiggleTypes);
        Assert.assertFalse(fo.TRUTHINESS);
    }

    @Test
    public void testPositiveWithoutPositional() {
        final String[] args = {
                "T=17",
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "SHMIGGLE_TYPE=shmiggle2",
        };
        final OptionsWithoutPositional fo = new OptionsWithoutPositional();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(fo.FROBNICATION_THRESHOLD.intValue(), 17);
        Assert.assertEquals(fo.FROBNICATION_FLAVOR, FrobnicationFlavor.BAR);
        Assert.assertEquals(fo.SHMIGGLE_TYPE.size(), 2);
        final String[] expectedShmiggleTypes = {"shmiggle1", "shmiggle2"};
        Assert.assertEquals(fo.SHMIGGLE_TYPE.toArray(), expectedShmiggleTypes);
        Assert.assertFalse(fo.TRUTHINESS);
    }

    /**
     * If last character of command line is the equal sign in an option=value pair,
     * make sure no crash, and that the value is empty string.
     */
    @Test
    public void testPositiveTerminalEqualSign() {
        final String[] args = {
                "T=17",
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "SHMIGGLE_TYPE=",
        };
        final OptionsWithoutPositional fo = new OptionsWithoutPositional();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(fo.FROBNICATION_THRESHOLD.intValue(), 17);
        Assert.assertEquals(fo.FROBNICATION_FLAVOR, FrobnicationFlavor.BAR);
        Assert.assertEquals(fo.SHMIGGLE_TYPE.size(), 2);
        final String[] expectedShmiggleTypes = {"shmiggle1", ""};
        Assert.assertEquals(fo.SHMIGGLE_TYPE.toArray(), expectedShmiggleTypes);
        Assert.assertFalse(fo.TRUTHINESS);
    }

    @Test
    public void testDefault() {
        final String[] args = {
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "SHMIGGLE_TYPE=shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(fo.FROBNICATION_THRESHOLD.intValue(), 20);
    }

    @Test
    public void testMissingRequiredArgument() {
        final String[] args = {
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "SHMIGGLE_TYPE=shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertFalse(clp.parseArguments(System.err, args));
    }

    @Test
    public void testBadValue() {
        final String[] args = {
                "FROBNICATION_THRESHOLD=ABC",
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "SHMIGGLE_TYPE=shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertFalse(clp.parseArguments(System.err, args));
    }

    @Test
    public void testBadEnumValue() {
        final String[] args = {
                "FROBNICATION_FLAVOR=HiMom",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "SHMIGGLE_TYPE=shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertFalse(clp.parseArguments(System.err, args));
    }

    @Test
    public void testNotEnoughOfListOption() {
        final String[] args = {
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "positional1",
                "positional2",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertFalse(clp.parseArguments(System.err, args));
    }

    @Test
    public void testTooManyListOption() {
        final String[] args = {
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "SHMIGGLE_TYPE=shmiggle2",
                "SHMIGGLE_TYPE=shmiggle3",
                "SHMIGGLE_TYPE=shmiggle4",
                "positional1",
                "positional2",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertFalse(clp.parseArguments(System.err, args));
    }

    @Test
    public void testTooManyPositional() {
        final String[] args = {
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "SHMIGGLE_TYPE=shmiggle2",
                "positional1",
                "positional2",
                "positional3",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertFalse(clp.parseArguments(System.err, args));
    }

    @Test
    public void testNotEnoughPositional() {
        final String[] args = {
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "SHMIGGLE_TYPE=shmiggle2",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertFalse(clp.parseArguments(System.err, args));
    }

    @Test
    public void testUnexpectedPositional() {
        final String[] args = {
                "T=17",
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "SHMIGGLE_TYPE=shmiggle2",
                "positional"
        };
        final OptionsWithoutPositional fo = new OptionsWithoutPositional();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertFalse(clp.parseArguments(System.err, args));
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testOptionDefinitionCaseClash() {
        final OptionsWithCaseClash options = new OptionsWithCaseClash();
        new LegacyCommandLineArgumentParser(options);
        Assert.fail("Should not be reached.");
    }

    @Test
    public void testOptionUseCaseClash() {
        final String[] args = {
                "FROBNICATION_FLAVOR=BAR",
                "FrOBNICATION_fLAVOR=BAR",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertFalse(clp.parseArguments(System.err, args));
    }

    @Test
    public void testNullValue() {
        final String[] args = {
                "FROBNICATION_THRESHOLD=null",
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=null",
                "positional1",
                "positional2",
        };

        final FrobnicateOptionsWithNullList fownl = new FrobnicateOptionsWithNullList();
        fownl.SHMIGGLE_TYPE.add("shmiggle1"); //providing null value should clear this list

        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fownl);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(fownl.positionalArguments.size(), 2);
        final File[] expectedPositionalArguments = {new File("positional1"), new File("positional2")};
        Assert.assertEquals(fownl.positionalArguments.toArray(), expectedPositionalArguments);
        Assert.assertEquals(fownl.FROBNICATION_THRESHOLD, null); //test null value
        Assert.assertEquals(fownl.SHMIGGLE_TYPE.size(), 0); //test null value for list
        Assert.assertFalse(fownl.TRUTHINESS);

        //verify that required arg can't be set to null
        args[2] = "TRUTHINESS=null";
        final LegacyCommandLineArgumentParser clp2 = new LegacyCommandLineArgumentParser(fownl);
        Assert.assertFalse(clp2.parseArguments(System.err, args));

        //verify that positional arg can't be set to null
        args[2] = "TRUTHINESS=False";
        args[4] = "null";
        final LegacyCommandLineArgumentParser clp3 = new LegacyCommandLineArgumentParser(fownl);
        Assert.assertFalse(clp3.parseArguments(System.err, args));

    }


    @Test
    public void testOptionsFile() throws Exception {
        final File optionsFile = File.createTempFile("clp.", ".options");
        optionsFile.deleteOnExit();
        final PrintWriter writer = new PrintWriter(optionsFile);
        writer.println("T=18");
        writer.println("TRUTHINESS=True");
        writer.println("SHMIGGLE_TYPE=shmiggle0");
        writer.println("STRANGE_OPTION=shmiggle0");
        writer.close();
        final String[] args = {
                "OPTIONS_FILE=" + optionsFile.getPath(),
                // Multiple options files are allowed
                "OPTIONS_FILE=" + optionsFile.getPath(),
                "T=17",
                "FROBNICATION_FLAVOR=BAR",
                "TRUTHINESS=False",
                "SHMIGGLE_TYPE=shmiggle1",
                "positional1",
                "positional2",
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(fo.positionalArguments.size(), 2);
        final File[] expectedPositionalArguments = {new File("positional1"), new File("positional2")};
        Assert.assertEquals(fo.positionalArguments.toArray(), expectedPositionalArguments);
        Assert.assertEquals(fo.FROBNICATION_THRESHOLD.intValue(), 17);
        Assert.assertEquals(fo.FROBNICATION_FLAVOR, FrobnicationFlavor.BAR);
        Assert.assertEquals(fo.SHMIGGLE_TYPE.size(), 3);
        final String[] expectedShmiggleTypes = {"shmiggle0", "shmiggle0", "shmiggle1"};
        Assert.assertEquals(fo.SHMIGGLE_TYPE.toArray(), expectedShmiggleTypes);
        Assert.assertFalse(fo.TRUTHINESS);
    }


    /**
     * In an options file, should not be allowed to override an option set on the command line
     *
     * @throws Exception
     */
    @Test
    public void testOptionsFileWithDisallowedOverride() throws Exception {
        final File optionsFile = File.createTempFile("clp.", ".options");
        optionsFile.deleteOnExit();
        final PrintWriter writer = new PrintWriter(optionsFile);
        writer.println("T=18");
        writer.close();
        final String[] args = {
                "T=17",
                "OPTIONS_FILE=" + optionsFile.getPath()
        };
        final FrobnicateOptions fo = new FrobnicateOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(fo);
        Assert.assertFalse(clp.parseArguments(System.err, args));
    }

    @DataProvider(name = "mutexScenarios")
    public Object[][] mutexScenarios() {
        return new Object[][]{
                {"pass", new String[]{"A=1", "B=2"}, true},
                {"no args", new String[0], false},
                {"1 of group required", new String[]{"A=1"}, false},
                {"mutex", new String[]{"A=1", "Y=3"}, false},
                {"mega mutex", new String[]{"A=1", "B=2", "Y=3", "Z=1", "M=2", "N=3"}, false}
        };
    }

    @Test(dataProvider = "mutexScenarios")
    public void testMutex(final String testName, final String[] args, final boolean expected) {
        final MutexOptions o = new MutexOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(o);
        Assert.assertEquals(clp.parseArguments(System.err, args), expected);
    }

    class UninitializedCollectionOptions {
        @Argument
        public List<String> LIST;
        @Argument
        public ArrayList<String> ARRAY_LIST;
        @Argument
        public HashSet<String> HASH_SET;
        @PositionalArguments
        public Collection<File> COLLECTION;

    }

    @Test
    public void testUninitializedCollections() {
        final UninitializedCollectionOptions o = new UninitializedCollectionOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(o);
        final String[] args = {"LIST=L1", "LIST=L2", "ARRAY_LIST=S1", "HASH_SET=HS1", "P1", "P2"};
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(o.LIST.size(), 2);
        Assert.assertEquals(o.ARRAY_LIST.size(), 1);
        Assert.assertEquals(o.HASH_SET.size(), 1);
        Assert.assertEquals(o.COLLECTION.size(), 2);
    }

    class UninitializedCollectionThatCannotBeAutoInitializedOptions {
        @Argument
        public Set<String> SET;
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testCollectionThatCannotBeAutoInitialized() {
        final UninitializedCollectionThatCannotBeAutoInitializedOptions o = new UninitializedCollectionThatCannotBeAutoInitializedOptions();
        new LegacyCommandLineArgumentParser(o);
        Assert.fail("Exception should have been thrown");
    }

    class CollectionWithDefaultValuesOptions {
        @Argument
        public List<String> LIST = makeList("foo", "bar");
    }

    @Test
    public void testClearDefaultValuesFromListOption() {
        final CollectionWithDefaultValuesOptions o = new CollectionWithDefaultValuesOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(o);
        final String[] args = {"LIST=null"};
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(o.LIST.size(), 0);
    }

    @Test
    public void testClearDefaultValuesFromListOptionAndAddNew() {
        final CollectionWithDefaultValuesOptions o = new CollectionWithDefaultValuesOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(o);
        final String[] args = {"LIST=null", "LIST=baz", "LIST=frob"};
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(o.LIST, makeList("baz", "frob"));
    }

    @Test
    public void testAddToDefaultValuesListOption() {
        final CollectionWithDefaultValuesOptions o = new CollectionWithDefaultValuesOptions();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(o);
        final String[] args = {"LIST=baz", "LIST=frob"};
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(o.LIST, makeList("foo", "bar", "baz", "frob"));
    }

    class ArgsCollection {
        @Argument(fullName = "arg1")
        public int Arg1;
    }

    class ArgsCollectionContainer {

        public ArgsCollectionContainer(){}

        @ArgumentCollection
        public ArgsCollection default_args = new ArgsCollection();

        @Argument(fullName = "somenumber",shortName = "n")
        public int someNumber = 0;
    }

    @Test
    public void testArgumentCollectionFlat() {
        final ArgsCollectionContainer o = new ArgsCollectionContainer();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(o);

        String[] args = {"arg1=42", "somenumber=12"};
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(o.default_args.Arg1, 42);
        Assert.assertEquals(o.someNumber, 12);
    }

    public abstract class ReferenceArgumentCollection {
        public abstract File getReferenceFile();
    }

    public class OptionalReferenceArgumentCollection extends ReferenceArgumentCollection {

        @Argument(doc="A reference is optional", optional=true, common=true)
        public File REFERENCE_SEQUENCE;

        public File getReferenceFile() { return REFERENCE_SEQUENCE; };
    }

    public class RequiredReferenceArgumentCollection extends ReferenceArgumentCollection {

        @Argument(doc="A reference is required", optional=false, common=false)
        public File REFERENCE_SEQUENCE;

        public File getReferenceFile() { return REFERENCE_SEQUENCE; };
    }

    class ArgCollectionToolBase {
        @ArgumentCollection
        public ReferenceArgumentCollection referenceSequence =
                requiresReference() ?
                        new RequiredReferenceArgumentCollection() :
                        new OptionalReferenceArgumentCollection();

        public boolean requiresReference() { return false;}
    }

    class ToolWithRequiredReference extends ArgCollectionToolBase {
        public boolean requiresReference() { return true; }
    }

    class ToolWithOptionalReference extends ArgCollectionToolBase {
        public boolean requiresReference() { return false; }
    }

    // Tool with optional reference, no reference provided
    @Test
    public void testArgumentCollectionNoReferenceProvidedForOptional(){
        final ToolWithOptionalReference o = new ToolWithOptionalReference();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(o);

        String[] args = {}; // no reference provided
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertNull(o.referenceSequence.getReferenceFile());
    }

    // Tool with optional reference, with reference provided
    @Test
    public void testArgumentCollectionWithReferenceProvidedForOptional() {
        final ToolWithOptionalReference o = new ToolWithOptionalReference();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(o);

        String[] args = {"REFERENCE_SEQUENCE=ref.fasta"}; // with reference provided
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(o.referenceSequence.getReferenceFile().getName(), "ref.fasta");
    }

    // Tool with required reference, no reference provided (parseArguments issues error message; returns false)
    @Test
    public void testArgumentCollectionNoReferenceProvidedForRequired() {
        final ToolWithRequiredReference o = new ToolWithRequiredReference();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(o);

        String[] args = {}; // no reference provided
        Assert.assertFalse(clp.parseArguments(System.err, args));
    }

    // Tool with required reference, with reference provided
    @Test
    public void testArgumentCollectionWithReferenceProvidedForRequired(){
        final ToolWithRequiredReference o = new ToolWithRequiredReference();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(o);

        String[] args = {"REFERENCE_SEQUENCE=ref.fasta"}; // with reference provided
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(o.referenceSequence.getReferenceFile().getName(), "ref.fasta");
    }

    //////////////////////////////
    // Nested argument collections

    class ArgumentCollectionContainerInner {
        @ArgumentCollection
        ArgsCollection innerContainerArg = new ArgsCollection();
    }

    class ArgumentCollectionContainerOuter {
        @Argument(fullName = "outerContainerArg")
        public int outerContainerArg;

        @ArgumentCollection
        ArgumentCollectionContainerInner argInner = new ArgumentCollectionContainerInner();
    }

    class ToolWithNestedArgumentCollection  {
        @ArgumentCollection
        public ArgumentCollectionContainerOuter argOuter =  new ArgumentCollectionContainerOuter();
    }

    @Test
    public void testArgumentCollectionNested() {
        final ToolWithNestedArgumentCollection o = new ToolWithNestedArgumentCollection();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(o);

        String[] args = {"outerContainerArg=17", "arg1=92"};
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(o.argOuter.outerContainerArg, 17);
        Assert.assertEquals(o.argOuter.argInner.innerContainerArg.Arg1, 92);
    }

    private List<String> makeList(final String... list) {
        final List<String> result = new ArrayList<>();
        Collections.addAll(result, list);
        return result;
    }

    @DataProvider(name = "testHtmlEscapeData")
    public Object[][] testHtmlEscapeData() {
        final List<Object[]> retval = new ArrayList<>();

        retval.add(new Object[]{"<", "&lt;"});
        retval.add(new Object[]{"x<y", "x&lt;y"});
        retval.add(new Object[]{"x<y<z", "x&lt;y&lt;z"});
        retval.add(new Object[]{"\n", "<p>"});
        retval.add(new Object[]{"<html> x<y </html> y< <strong> x </strong>","<html> x&lt;y </html> y&lt; <strong> x </strong>"});

        return retval.toArray(new Object[0][]);
    }

    @Test(dataProvider = "testHtmlEscapeData")
    public void testHtmlUnescape(final String expected, final String html) {
        Assert.assertEquals(LegacyCommandLineArgumentParser.htmlUnescape(html), expected, "problems");
    }

    @DataProvider(name = "testHTMLConverter")
    public Object[][] testHTMLConverterData() {
        final List<Object[]> retval = new ArrayList<>();

        retval.add(new Object[]{"hello", "hello"});
        retval.add(new Object[]{"", ""});
        retval.add(new Object[]{"hi</th>bye", "hi\tbye"});
        retval.add(new Object[]{"hi<th>bye", "hibye"});
        retval.add(new Object[]{"hi<li>bye", "hi - bye"});
        retval.add(new Object[]{"hi<NOT_A_REAL_TAG>bye", "hibye"});
        retval.add(new Object[]{"</h4><pre>", "\n\n"});
        retval.add(new Object[]{"<a href=\"http://go.here.org\"> string</ a >", " string (http://go.here.org)"});
        retval.add(new Object[]{"<a href=\"http://go.here.org\" > string</ a>", " string (http://go.here.org)"});
        retval.add(new Object[]{"< a href=\"http://go.here.org\"> string<a />", " string (http://go.here.org)"});


        //for some reason, the next test seems to break intelliJ, but it works on the commandline
        retval.add(new Object[]{"hi</li>bye", "hi\nbye"});

        retval.add(new Object[]{"Using read outputs from high throughput sequencing (HTS) technologies, this tool provides " +
                "metrics regarding the quality of read alignments to a reference sequence, as well as the proportion of the reads " +
                "that passed machine signal-to-noise threshold quality filters (Illumina)." +
                "<h4>Usage example:</h4>" +
                "<pre>" +
                "    java -jar picard.jar CollectAlignmentSummaryMetrics \\<br />" +
                "          R=reference_sequence.fasta \\<br />" +
                "          I=input.bam \\<br />" +
                "          O=output.txt" +
                "</pre>" +
                "Please see <a href='http://broadinstitute.github.io/picard/picard-metric-definitions.html#AlignmentSummaryMetrics'>" +
                "the AlignmentSummaryMetrics documentation</a> for detailed explanations of each metric. <br /> <br />" +
                "Additional information about Illumina's quality filters can be found in the following documents on the Illumina website:" +
                "<ul><li><a href=\"http://support.illumina.com/content/dam/illumina-marketing/documents/products/technotes/hiseq-x-percent-pf-technical-note-770-2014-043.pdf\">" +
                "hiseq-x-percent-pf-technical-note</a></li>" +
                "<li><a href=\"http://support.illumina.com/content/dam/illumina-support/documents/documentation/system_documentation/hiseqx/hiseq-x-system-guide-15050091-d.pdf\">" +
                "hiseq-x-system-guide</a></li></ul>" +
                "<hr />",

                "Using read outputs from high throughput sequencing (HTS) technologies, this tool provides " +
                        "metrics regarding the quality of read alignments to a reference sequence, as well as the proportion of the reads " +
                        "that passed machine signal-to-noise threshold quality filters (Illumina)." +
                        "\nUsage example:\n" +
                        "\n" +
                        "    java -jar picard.jar CollectAlignmentSummaryMetrics \\\n" +
                        "          R=reference_sequence.fasta \\\n" +
                        "          I=input.bam \\\n" +
                        "          O=output.txt" +
                        "\n" +
                        "Please see the AlignmentSummaryMetrics documentation (http://broadinstitute.github.io/picard/picard-metric-definitions.html#AlignmentSummaryMetrics) for detailed explanations of each metric. \n \n" +
                        "Additional information about Illumina's quality filters can be found in the following documents on the Illumina website:" +
                        "\n" +
                        " - hiseq-x-percent-pf-technical-note (http://support.illumina.com/content/dam/illumina-marketing/documents/products/technotes/hiseq-x-percent-pf-technical-note-770-2014-043.pdf)\n" +
                        " - hiseq-x-system-guide (http://support.illumina.com/content/dam/illumina-support/documents/documentation/system_documentation/hiseqx/hiseq-x-system-guide-15050091-d.pdf)\n\n" +
                        "\n"});

        return retval.toArray(new Object[0][]);
    }

    @Test(dataProvider = "testHTMLConverter")
    public void testHTMLConverter(String input, String expected) {
        final String converted = LegacyCommandLineArgumentParser.convertFromHtml(input);
        Assert.assertEquals(converted, expected, "common part:\"" + expected.substring(0, lengthOfCommonSubstring(converted, expected)) + "\"\n\n");
    }

    @CommandLineProgramProperties(
            summary = TestParserFail.USAGE_SUMMARY + TestParserFail.USAGE_DETAILS,
            oneLineSummary = TestParserFail.USAGE_SUMMARY,
            programGroup = TestProgramGroup.class
    )
    protected class TestParserFail extends Object {

        static public final String USAGE_DETAILS = "blah &blah; blah ";
        static public final String USAGE_SUMMARY = "This tool offers.....";
    }

    @CommandLineProgramProperties(
            summary = TestParserSucceed.USAGE_SUMMARY + TestParserSucceed.USAGE_DETAILS,
            oneLineSummary = TestParserSucceed.USAGE_SUMMARY,
            programGroup = TestProgramGroup.class
    )
    protected class TestParserSucceed extends Object {

        static public final String USAGE_DETAILS = "This is the first row <p>And this is the second";
        static public final String USAGE_SUMMARY = " X &lt; Y ";
    }

    @CommandLineProgramProperties(
            summary = TestParserLongPreamble.USAGE_SUMMARY + TestParserLongPreamble.USAGE_DETAILS,
            oneLineSummary = TestParserLongPreamble.USAGE_SUMMARY,
            programGroup = TestProgramGroup.class
    )
    protected class TestParserLongPreamble extends Object {

        static public final String USAGE_DETAILS = "This is the first row but it's really long and it has " +
                "lots of words...really big words. Because it knows the best people and really has lots of " +
                "interesting things it needs to get across. Definitely more than can fit in 140 characters...";
        static public final String USAGE_SUMMARY = " X &lt; Y ";
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testNonAsciiAssertion() {
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(new TestParserFail());

        final PrintStream stream = new PrintStream(new NullOutputStream());
        clp.parseArguments(stream, new String[]{});
        clp.usage(true, true);
    }

    @Test
    public void testNonAsciiConverted() {
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(new TestParserSucceed());

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream stream = new PrintStream(byteArrayOutputStream);
        clp.parseArguments(stream, new String[]{});
        stream.append(clp.usage(true, true));

        final String expected = "USAGE: TestParserSucceed [options]\n" +
                "\n" +
                "X < Y This is the first row \n" +
                "And this is the second";
        final String result = byteArrayOutputStream.toString();
        Assert.assertEquals(result.substring(0, expected.length()), expected);
    }


    @Test
    public void testLongPreamble() {
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(new TestParserLongPreamble());

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream stream = new PrintStream(byteArrayOutputStream);
        clp.parseArguments(stream, new String[]{});
        stream.append(clp.usage(true, true));

        final String expected = "USAGE: TestParserLongPreamble [options]\n" +
                "\n" +
                "X < Y This is the first row but it's really long and it has lots of words...really big words. Because it knows the best\n" +
                "people and really has lots of interesting things it needs to get across. Definitely more than can fit in 140\n" +
                "characters...";

        final String result = byteArrayOutputStream.toString();
        Assert.assertEquals(result.substring(0, expected.length()), expected);
    }


    @Test
    public void testNonASCIIAccept() {
        LegacyCommandLineArgumentParser.checkForNonASCII("abc", "ascii passes");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testNonASCIIReject() {
        LegacyCommandLineArgumentParser.checkForNonASCII("\u0080", "non-ascii fails");
    }

    static private int lengthOfCommonSubstring(String lhs, String rhs) {
        int i = 0;
        while (i < Math.min(lhs.length(), rhs.length()) && lhs.charAt(i) == rhs.charAt(i)) i++;

        return i;
    }

    private class NullOutputStream extends OutputStream {
        @Override
        public void write(final int b) throws IOException {

        }
    }

    @CommandLineProgramProperties(
            summary = "Tool with max/min boundaries",
            oneLineSummary = "Tool with max/min boundaries",
            programGroup = TestProgramGroup.class
    )
    private static final class WithBoundaries {
        @Argument(minValue = -10, minRecommendedValue = -2, maxValue = 2, maxRecommendedValue = 10)
        public int INTEGER_VALUE;
    }

    @DataProvider
    public Object[][] valuesForWithBoundaries() {
        return new Object[][] {{-15}, {-5}, {-2}, {1}, {0}, {2}, {5}, {10}};
    }

    @Test(dataProvider = "valuesForWithBoundaries")
    public void testIgnoringBoundaries(final int value) {
        final WithBoundaries withBoundaries = new WithBoundaries();
        final LegacyCommandLineArgumentParser clp = new LegacyCommandLineArgumentParser(withBoundaries);
        clp.parseArguments(System.err, new String[]{"INTEGER_VALUE=" + Integer.toString(value)});
        Assert.assertEquals(withBoundaries.INTEGER_VALUE, value);
    }
}
