package org.broadinstitute.barclay.argparser;

import org.apache.commons.lang3.tuple.Pair;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Consumer;

public final class CommandLineArgumentParserTest {
    enum FrobnicationFlavor {
        FOO, BAR, BAZ
    }

    @CommandLineProgramProperties(
            summary = "Usage: frobnicate [arguments] input-file output-file\n\nRead input-file, frobnicate it, and write frobnicated results to output-file\n",
            oneLineSummary = "Read input-file, frobnicate it, and write frobnicated results to output-file",
            programGroup = TestProgramGroup.class
    )
    class FrobnicateArguments {
        @ArgumentCollection
        SpecialArgumentsCollection specialArgs = new SpecialArgumentsCollection();

        @PositionalArguments(minElements=2, maxElements=2)
        public List<File> positionalArguments = new ArrayList<>();

        @Argument(shortName="T", doc="Frobnication threshold setting.")
        public Integer FROBNICATION_THRESHOLD = 20;

        @Argument
        public FrobnicationFlavor FROBNICATION_FLAVOR;

        @Argument(doc="Allowed shmiggle types.", optional = false)
        public List<String> SHMIGGLE_TYPE = new ArrayList<>();

        @Argument
        public Boolean TRUTHINESS = false;
    }

    @CommandLineProgramProperties(
            summary = "Usage: framistat [arguments]\n\nCompute the plebnick of the freebozzle.\n",
            oneLineSummary = "ompute the plebnick of the freebozzle",
            programGroup = TestProgramGroup.class
    )
    class ArgumentsWithoutPositional {
        public static final int DEFAULT_FROBNICATION_THRESHOLD = 20;
        @Argument(shortName="T", doc="Frobnication threshold setting.")
        public Integer FROBNICATION_THRESHOLD = DEFAULT_FROBNICATION_THRESHOLD;

        @Argument
        public FrobnicationFlavor FROBNICATION_FLAVOR;

        @Argument(doc="Allowed shmiggle types.", optional = false)
        public List<String> SHMIGGLE_TYPE = new ArrayList<>();

        @Argument
        public Boolean TRUTHINESS;
    }

    class MutexArguments {
        @Argument(mutex={"M", "N", "Y", "Z"})
        public String A;
        @Argument(mutex={"M", "N", "Y", "Z"})
        public String B;
        @Argument(mutex={"A", "B", "Y", "Z"})
        public String M;
        @Argument(mutex={"A", "B", "Y", "Z"})
        public String N;
        @Argument(mutex={"A", "B", "M", "N"})
        public String Y;
        @Argument(mutex={"A", "B", "M", "N"})
        public String Z;

    }

    class MixedCardinalityMutexArguments {
        @Argument(optional=false, mutex={"scalar"})
        public List<String> collection;
        @Argument(optional=false, mutex={"collection"})
        public String scalar;
    }

    @CommandLineProgramProperties(
            summary = "[oscillation_frequency]\n\nResets oscillation frequency.\n",
            oneLineSummary = "Reset oscillation frequency.",
            programGroup = TestProgramGroup.class
    )
    public class RequiredOnlyArguments {
        @Argument(doc="Oscillation frequency.", optional = false)
        public String OSCILLATION_FREQUENCY;
    }

    @Test
    public void testRequiredOnlyUsage() {
        final RequiredOnlyArguments nr = new RequiredOnlyArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(nr);
        final String out = clp.usage(false, false); // without common/hidden args
        final int reqIndex = out.indexOf("Required Arguments:");
        Assert.assertTrue(reqIndex > 0);
        Assert.assertTrue(out.indexOf("Optional Arguments:", reqIndex) < 0);
        Assert.assertTrue(out.indexOf("Advanced Arguments:", reqIndex) < 0);
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
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(eo);
        final String out = clp.usage(false, false); // without common/hidden args
        final int reqIndex = out.indexOf(CommandLineArgumentParser.BETA_PREFIX);
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
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(eo);
        final String out = clp.usage(false, false); // without common/hidden args
        final int reqIndex = out.indexOf(CommandLineArgumentParser.EXPERIMENTAL_PREFIX);
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
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(eo);
    }

    class ArrayArgument {
        // arrays have no string constructor
        @Argument
        public String[] stringArrayArgument = new String[] { "foo", "bar" };
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testArrayArgument() {
        final ArrayArgument arrayArg = new ArrayArgument();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(arrayArg);
        Assert.assertTrue(clp.parseArguments(System.err, new String[]{"--stringArrayArgument", "somestring"}));
    }

    class AbbreviatableArgument{
        public static final String ARGUMENT_NAME = "longNameArgument";
        @Argument(fullName= ARGUMENT_NAME)
        public boolean longNameArgument;
    }

    @Test(expectedExceptions = CommandLineException.class)
    public void testAbbreviationsAreRejected() {
        final AbbreviatableArgument abrv = new AbbreviatableArgument();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(abrv);
        //argument name is valid when it isn't abbreviated
        Assert.assertTrue(clp.parseArguments(System.err, new String[]{"--" + AbbreviatableArgument.ARGUMENT_NAME}));

        //should throw when the abbreviated name is used
        clp.parseArguments(System.err, new String[]{"--" + AbbreviatableArgument.ARGUMENT_NAME.substring(0,5)});
    }

    @CommandLineProgramProperties(
            summary = "[oscillation_frequency]\n\nRecalibrates overthruster oscillation. \n",
            oneLineSummary = "Recalibrates the overthruster.",
            programGroup = TestProgramGroup.class
    )
    public class OptionalOnlyArguments {
        @Argument(doc="Oscillation frequency.", optional = true)
        public String OSCILLATION_FREQUENCY = "20";
    }

    @Test
    public void testOptionalOnlyUsage() {
        final OptionalOnlyArguments oo = new OptionalOnlyArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(oo);
        final String out = clp.usage(false, false); // without common/hidden args
        final int reqIndex = out.indexOf("Required Arguments:");
        Assert.assertTrue(reqIndex < 0);
        Assert.assertTrue(out.indexOf("Optional Arguments:", reqIndex) > 0);
        Assert.assertEquals(out.indexOf("Conditional Arguments:", reqIndex), -1);
        Assert.assertEquals(out.indexOf("Advanced Arguments:", reqIndex), -1);
    }

    /**
     * Validate the text emitted by a call to usage by ensuring that required arguments are
     * emitted before optional ones.
     */
    private void validateRequiredOptionalUsage(final CommandLineArgumentParser clp, final boolean withDefault, final boolean hasAdvanced) {
        final String out = clp.usage(withDefault, false); // with common args, without hidden args
        // Required arguments should appear before optional ones
        final int reqIndex = out.indexOf("Required Arguments:");
        Assert.assertTrue(reqIndex > 0);
        Assert.assertTrue(out.indexOf("Optional Arguments:", reqIndex) > 0);
        Assert.assertEquals(out.indexOf("Conditional Arguments:", reqIndex), -1);
        Assert.assertEquals(out.indexOf("Advanced Arguments:", reqIndex) != -1, hasAdvanced);
    }

    @Test
    public void testRequiredOptionalWithDefaultUsage() {
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        // FrobnicateArguments has a SpecialArgumentsCollection that contains an @Advanced argument ("called showHidden")
        validateRequiredOptionalUsage(clp, true, true); // with common args
    }

    @Test
    public void testRequiredOptionalWithoutDefaultUsage() {
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        // FrobnicateArguments has a SpecialArgumentsCollection that contains an @Advanced argument ("called showHidden")
        validateRequiredOptionalUsage(clp, false, true); // without common args
    }

    @Test
    public void testWithoutPositionalWithDefaultUsage() {
        final ArgumentsWithoutPositional fo = new ArgumentsWithoutPositional();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        // does not have the special showHidden advanced argument
        validateRequiredOptionalUsage(clp, true, false); // with common args
    }

    @Test
    public void testWithoutPositionalWithoutDefaultUsage() {
        final ArgumentsWithoutPositional fo = new ArgumentsWithoutPositional();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        // does not have the special showHidden advanced argument
        validateRequiredOptionalUsage(clp, false, false); // without common args
    }

    @Test
    public void testPositive() {
        final String[] args = {
                "-T","17",
                "-FROBNICATION_FLAVOR","BAR",
                "-TRUTHINESS",
                "-SHMIGGLE_TYPE","shmiggle1",
                "-SHMIGGLE_TYPE","shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(fo.positionalArguments.size(), 2);
        final File[] expectedPositionalArguments = { new File("positional1"), new File("positional2")};
        Assert.assertEquals(fo.positionalArguments.toArray(), expectedPositionalArguments);
        Assert.assertEquals(fo.FROBNICATION_THRESHOLD.intValue(), 17);
        Assert.assertEquals(fo.FROBNICATION_FLAVOR, FrobnicationFlavor.BAR);
        Assert.assertEquals(fo.SHMIGGLE_TYPE.size(), 2);
        final String[] expectedShmiggleTypes = {"shmiggle1", "shmiggle2"};
        Assert.assertEquals(fo.SHMIGGLE_TYPE.toArray(), expectedShmiggleTypes);
        Assert.assertTrue(fo.TRUTHINESS);
    }

    @Test
    public void testGetCommandLine() {
        final String[] args = {
                "-T","17",
                "-FROBNICATION_FLAVOR","BAR",
                "-TRUTHINESS",
                "-SHMIGGLE_TYPE","shmiggle1",
                "-SHMIGGLE_TYPE","shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(clp.getCommandLine(),
                "FrobnicateArguments " +
                        "positional1 positional2 --FROBNICATION_THRESHOLD 17 --FROBNICATION_FLAVOR BAR " +
                        "--SHMIGGLE_TYPE shmiggle1 --SHMIGGLE_TYPE shmiggle2 --TRUTHINESS true --help false " +
                        "--version false --showHidden false");
    }

    private static class WithSensitiveValues {

        @Argument(sensitive = true)
        public String secretValue;

        @Argument
        public String openValue;
    }

    @Test
    public void testGetCommandLineWithSensitiveArgument(){
        final String supersecret = "supersecret";
        final String unclassified = "unclassified";
        final String[] args = {
                "--secretValue", supersecret,
                "--openValue", unclassified
        };
        final WithSensitiveValues sv = new WithSensitiveValues();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(sv);
        Assert.assertTrue(clp.parseArguments(System.err, args));

        final String commandLine = clp.getCommandLine();

        Assert.assertTrue(commandLine.contains(unclassified));
        Assert.assertFalse(commandLine.contains(supersecret));

        Assert.assertEquals(sv.openValue, unclassified);
        Assert.assertEquals(sv.secretValue, supersecret);
    }

    @Test
    public void testDefault() {
        final String[] args = {
                "--FROBNICATION_FLAVOR","BAR",
                "--TRUTHINESS",
                "--SHMIGGLE_TYPE","shmiggle1",
                "--SHMIGGLE_TYPE","shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(fo.FROBNICATION_THRESHOLD.intValue(), 20);
    }

    @Test(expectedExceptions = CommandLineException.MissingArgument.class)
    public void testMissingRequiredArgument() {
        final String[] args = {
                "--TRUTHINESS","False",
                "--SHMIGGLE_TYPE","shmiggle1",
                "--SHMIGGLE_TYPE","shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        clp.parseArguments(System.err, args);
    }

    class CollectionRequired{
        @Argument(optional = false)
        List<Integer> ints;
    }

    @Test(expectedExceptions = CommandLineException.MissingArgument.class)
    public void testMissingRequiredCollectionArgument(){
        final String[] args = {};
        final CollectionRequired cr = new CollectionRequired();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(cr);
        clp.parseArguments(System.err, args);
    }

    @Test( expectedExceptions = CommandLineException.BadArgumentValue.class)
    public void testBadValue() {
        final String[] args = {
                "--FROBNICATION_THRESHOLD","ABC",
                "--FROBNICATION_FLAVOR","BAR",
                "--TRUTHINESS","False",
                "--SHMIGGLE_TYPE","shmiggle1",
                "--SHMIGGLE_TYPE","shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        clp.parseArguments(System.err, args);
    }

    @Test(expectedExceptions = CommandLineException.BadArgumentValue.class)
    public void testBadEnumValue() {
        final String[] args = {
                "--FROBNICATION_FLAVOR","HiMom",
                "--TRUTHINESS","False",
                "--SHMIGGLE_TYPE","shmiggle1",
                "--SHMIGGLE_TYPE","shmiggle2",
                "positional1",
                "positional2",
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        clp.parseArguments(System.err, args);
    }

    @Test(expectedExceptions = CommandLineException.MissingArgument.class)
    public void testNotEnoughOfListArgument() {
        final String[] args = {
                "--FROBNICATION_FLAVOR","BAR",
                "--TRUTHINESS","False",
                "positional1",
                "positional2",
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        clp.parseArguments(System.err, args);
    }

    @Test(expectedExceptions = CommandLineException.class)
    public void testTooManyPositional() {
        final String[] args = {
                "--FROBNICATION_FLAVOR","BAR",
                "--TRUTHINESS","False",
                "--SHMIGGLE_TYPE","shmiggle1",
                "--SHMIGGLE_TYPE","shmiggle2",
                "positional1",
                "positional2",
                "positional3",
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        clp.parseArguments(System.err, args);
    }

    @Test(expectedExceptions = CommandLineException.MissingArgument.class)
    public void testNotEnoughPositional() {
        final String[] args = {
                "--FROBNICATION_FLAVOR","BAR",
                "--TRUTHINESS","False",
                "--SHMIGGLE_TYPE","shmiggle1",
                "--SHMIGGLE_TYPE","shmiggle2",
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        clp.parseArguments(System.err, args);
    }

    @Test( expectedExceptions = CommandLineException.class)
    public void testUnexpectedPositional() {
        final String[] args = {
                "--T","17",
                "--FROBNICATION_FLAVOR","BAR",
                "--TRUTHINESS","False",
                "--SHMIGGLE_TYPE","shmiggle1",
                "--SHMIGGLE_TYPE","shmiggle2",
                "positional"
        };
        final ArgumentsWithoutPositional fo = new ArgumentsWithoutPositional();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        clp.parseArguments(System.err, args);
    }

    @Test(expectedExceptions = CommandLineException.class)
    public void testArgumentUseClash() {
        final String[] args = {
                "--FROBNICATION_FLAVOR", "BAR",
                "--FROBNICATION_FLAVOR", "BAZ",
                "--SHMIGGLE_TYPE", "shmiggle1",
                "positional1",
                "positional2",
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        clp.parseArguments(System.err, args);
    }

    @Test
    public void testArgumentsFile() throws Exception {
        final File argumentsFile = File.createTempFile("clp.", ".arguments");
        argumentsFile.deleteOnExit();
        try (final PrintWriter writer = new PrintWriter(argumentsFile)) {
            writer.println("-T 18");
            writer.println("--TRUTHINESS");
            writer.println("--SHMIGGLE_TYPE shmiggle0");
            writer.println("--" + SpecialArgumentsCollection.ARGUMENTS_FILE_FULLNAME + " " + argumentsFile.getPath());
            //writer.println("--STRANGE_ARGUMENT shmiggle0");
        }
        final String[] args = {
                "--"+SpecialArgumentsCollection.ARGUMENTS_FILE_FULLNAME, argumentsFile.getPath(),
                // Multiple arguments files are allowed
                "--"+SpecialArgumentsCollection.ARGUMENTS_FILE_FULLNAME, argumentsFile.getPath(),
                "--FROBNICATION_FLAVOR","BAR",
                "--TRUTHINESS",
                "--SHMIGGLE_TYPE","shmiggle0",
                "--SHMIGGLE_TYPE","shmiggle1",
                "positional1",
                "positional2",
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(fo.positionalArguments.size(), 2);
        final File[] expectedPositionalArguments = { new File("positional1"), new File("positional2")};
        Assert.assertEquals(fo.positionalArguments.toArray(), expectedPositionalArguments);
        Assert.assertEquals(fo.FROBNICATION_THRESHOLD.intValue(), 18);
        Assert.assertEquals(fo.FROBNICATION_FLAVOR, FrobnicationFlavor.BAR);
        Assert.assertEquals(fo.SHMIGGLE_TYPE.size(), 3);
        final String[] expectedShmiggleTypes = {"shmiggle0", "shmiggle0", "shmiggle1"};
        Assert.assertEquals(fo.SHMIGGLE_TYPE.toArray(), expectedShmiggleTypes);
        Assert.assertTrue(fo.TRUTHINESS);
    }


    /**
     * In an arguments file, should not be allowed to override an argument set on the command line
     * @throws Exception
     */
    @Test( expectedExceptions = CommandLineException.class)
    public void testArgumentsFileWithDisallowedOverride() throws Exception {
        final File argumentsFile = File.createTempFile("clp.", ".arguments");
        argumentsFile.deleteOnExit();
        try (final PrintWriter writer = new PrintWriter(argumentsFile)) {
            writer.println("--T 18");
        }
        final String[] args = {
                "--T","17",
                "--"+SpecialArgumentsCollection.ARGUMENTS_FILE_FULLNAME ,argumentsFile.getPath()
        };
        final FrobnicateArguments fo = new FrobnicateArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(fo);
        clp.parseArguments(System.err, args);
    }

    @DataProvider(name="failingMutexScenarios")
    public Object[][] failingMutexScenarios() {
        return new Object[][] {
                { "no args", new MutexArguments(), new String[0] },
                { "1 of group required", new MutexArguments(), new String[] {"-A","1"} },
                { "mutex", new MutexArguments(), new String[]  {"-A","1", "-Y","3"} },
                { "mega mutex", new MutexArguments(), new String[]  {"-A","1", "-B","2", "-Y","3", "-Z","1", "-M","2", "-N","3"} },
                { "collection and scalar mutex mix", new MixedCardinalityMutexArguments(), new String[] { "-collection", "s1", "-scalar", "s2"}}
        };
    }

    @DataProvider(name="passingMutexScenarios")
    public Object[][] passingMutexScenarios() {
        return new Object[][] {
                { "simple mutex", new MutexArguments(), new String[] { "-A", "1", "-B", "2"} },
                { "collection mixed", new MixedCardinalityMutexArguments(), new String[] { "-collection", "s1"} },
                { "collection multiple mixed", new MixedCardinalityMutexArguments(), new String[] { "-collection", "s1", "-collection", "s2"} },
                { "scalar mixed", new MixedCardinalityMutexArguments(), new String[] { "-scalar", "s1"} }
        };
    }

    @Test(dataProvider="passingMutexScenarios")
    public void passingMutexCheck(final String testName, final Object o, final String[] args){
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        Assert.assertTrue(clp.parseArguments(System.err, args));
    }

    @Test(dataProvider="failingMutexScenarios", expectedExceptions = CommandLineException.class)
    public void testFailingMutex(final String testName, final Object o, final String[] args) {
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        clp.parseArguments(System.err, args);
    }

    class DanglingMutexArguments {
        @Argument(mutex={"B"})
        public String A;

        @Argument(mutex={"nonexistent", "Z"})
        public String B;

        @Argument(mutex={"A", "B"})
        public String Z;
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testDanglingMutex() {
        final DanglingMutexArguments o = new DanglingMutexArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        Assert.assertTrue(clp.parseArguments(System.err, new String[]{"-B", "b"}));
    }

    @Test
       public void testFlagNoArgument(){
        final BooleanFlags o = new BooleanFlags();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        Assert.assertTrue(clp.parseArguments(System.err, new String[]{"--flag1"}));
        Assert.assertTrue(o.flag1);
    }

    @Test
    public void testFlagsWithArguments(){
        final BooleanFlags o = new BooleanFlags();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        Assert.assertTrue(clp.parseArguments(System.err, new String[]{"--flag1", "false", "--flag2", "false"}));
        Assert.assertFalse(o.flag1);
        Assert.assertFalse(o.flag2);
    }

    class ArgsCollection {
        @Argument(fullName = "arg1")
        public int Arg1;
    }

    class ArgsCollectionHaver{

        public ArgsCollectionHaver(){}

        @ArgumentCollection
        public ArgsCollection default_args = new ArgsCollection();

        @Argument(fullName = "somenumber",shortName = "n")
        public int someNumber = 0;
    }

    @Test
    public void testArgumentCollection(){
        final ArgsCollectionHaver o = new ArgsCollectionHaver();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);

        String[] args = {"--arg1", "42", "--somenumber", "12"};
        Assert.assertTrue(clp.parseArguments(System.err, args));
        Assert.assertEquals(o.someNumber, 12);
        Assert.assertEquals(o.default_args.Arg1, 42);

    }

    class BooleanFlags{
        @Argument
        public Boolean flag1 = false;

        @Argument
        public boolean flag2 = true;

        @Argument
        public boolean flag3 = false;

        @Argument(mutex="flag1")
        public boolean antiflag1 = false;

        @ArgumentCollection
        SpecialArgumentsCollection special = new SpecialArgumentsCollection();
    }

    @Test
    public void testCombinationOfFlags(){
        final BooleanFlags o = new BooleanFlags();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);

        clp.parseArguments(System.err, new String[]{"--flag1", "false", "--flag2"});
        Assert.assertFalse(o.flag1);
        Assert.assertTrue(o.flag2);
        Assert.assertFalse(o.flag3);
    }

    class WithBadField{
        @Argument
        @ArgumentCollection
        public boolean badfield;
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testBadFieldCausesException(){
        WithBadField o = new WithBadField();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
    }

    class PrivateArgument{
        @Argument
        private boolean privateArgument = false;

        @Argument(optional = true)
        private List<Integer> privateCollection = new ArrayList<>();

        @ArgumentCollection
        private BooleanFlags booleanFlags= new BooleanFlags();

        @PositionalArguments()
        List<Integer> positionals = new ArrayList<>();
    }

    @Test
    public void testFlagWithPositionalFollowing(){
        PrivateArgument o = new PrivateArgument();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        Assert.assertTrue(clp.parseArguments(System.err, new String[]{"--flag1","1","2" }));
        Assert.assertTrue(o.booleanFlags.flag1);
        Assert.assertEquals(o.positionals, Arrays.asList(1, 2));
    }

    @Test
    public void testPrivateArgument(){
        PrivateArgument o = new PrivateArgument();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        Assert.assertTrue(clp.parseArguments(System.err, new String[]{"--privateArgument",
                "--privateCollection", "1", "--privateCollection", "2", "--flag1"}));
        Assert.assertTrue(o.privateArgument);
        Assert.assertEquals(o.privateCollection, Arrays.asList(1,2));
        Assert.assertTrue(o.booleanFlags.flag1);
    }

    /**
     * Test that the special flag --version is handled correctly
     * (no blowup)
     */
    @Test
    public void testVersionSpecialFlag(){
        final BooleanFlags o = new BooleanFlags();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);

        final String[] versionArgs = {"--" + SpecialArgumentsCollection.VERSION_FULLNAME};
        String out = captureStderr(() -> {
                    Assert.assertFalse(clp.parseArguments(System.err, versionArgs));
            });
        Assert.assertTrue(out.contains("Version:"));

        Assert.assertTrue(clp.parseArguments(System.err, new String[]{"--version","false"}));
        Assert.assertFalse(clp.parseArguments(System.err, new String[]{"--version", "true"}));
    }

    /**
     * Test that the special flag --help is handled correctly
     * (no blowup)
     */
    @Test
    public void testHelp(){
        final BooleanFlags o = new BooleanFlags();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);

        final String[] versionArgs = {"--" + SpecialArgumentsCollection.HELP_FULLNAME};
        String out = captureStderr(() -> {
            Assert.assertFalse(clp.parseArguments(System.err, versionArgs));
        });
        Assert.assertTrue(out.contains("USAGE:"));

        Assert.assertTrue(clp.parseArguments(System.err, new String[]{"--help","false"}));
        Assert.assertFalse(clp.parseArguments(System.err, new String[]{"--help", "true"}));
    }

    class NameCollision{
        @ArgumentCollection
        public ArgsCollection argsCollection = new ArgsCollection();

        //this arg name collides with one in ArgsCollection
        @Argument(fullName = "arg1")
        public int anArg;
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testArgumentNameCollision(){
        final NameCollision collides = new NameCollision();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(collides);

        clp.parseArguments(System.err, new String[]{"--arg1", "101"});
    }
    /**
     * captures {@link System#err} while runnable is executing
     * @param runnable a code block to execute
     * @return everything written to {@link System#err} by runnable
     */
    public static String captureStderr(Runnable runnable){
        return captureSystemStream(runnable, System.err, System::setErr);
    }

    private static String captureSystemStream(Runnable runnable, PrintStream stream, Consumer<? super PrintStream> setter){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        setter.accept(new PrintStream(out));
        try {
            runnable.run();
        } finally{
            setter.accept(stream);
        }
        return out.toString();
    }


    @CommandLineProgramProperties(
            summary = "tool with nullable arguments",
            oneLineSummary = "tools with nullable arguments",
            programGroup = TestProgramGroup.class
    )
    public class WithNullableArguments {
        // Integer without boundaries and null should be allowed
        @Argument(doc = "Integer with null value allowed", optional = true)
        public Integer nullInteger = null;
        // Double without boundaries and null should be allowed
        @Argument(doc = "Double with null value allowed", optional = true)
        public Double nullDouble= null;
    }

    @DataProvider(name = "nullableArgs")
    public Object[][] getNullableArguments() {
        return new Object[][] {
                // null values
                {new String[]{}, null, null},
                {new String[]{"--nullInteger", "null"}, null, null},
                {new String[]{"--nullDouble", "null"}, null, null},
                {new String[]{"--nullInteger", "null", "--nullDouble", "null"}, null, null},
                // with values
                {new String[]{"--nullInteger", "1"}, 1, null},
                {new String[]{"--nullDouble", "2"}, null, 2d},
                {new String[]{"--nullInteger", "1", "--nullDouble", "2"}, 1, 2.d},
        };
    }

    @Test(dataProvider = "nullableArgs")
    public void testWithinBoundariesArguments(final String[] argv, final Integer expectedInteger, final Double expectedDouble) throws Exception {
        final WithNullableArguments o = new WithNullableArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        Assert.assertTrue(clp.parseArguments(System.err, argv));
        Assert.assertEquals(o.nullInteger, expectedInteger);
        Assert.assertEquals(o.nullDouble, expectedDouble);
    }

    @CommandLineProgramProperties(
            summary = "tool with non-nullable arguments",
            oneLineSummary = "tools with non-nullable arguments",
            programGroup = TestProgramGroup.class
    )
    public class NonNullableArguments {
        @Argument(doc = "Primitive int", optional = true)
        public int primitiveInt;

        @Argument(doc = "Primitive double", optional = true)
        public double primitiveDouble;

        @Argument(doc = "Primitive boolean", optional = true)
        public boolean primitiveBoolean;
    }

    @DataProvider(name = "nonNullableArgs")
    public Object[][] getNonNullableArguments() {
        return new Object[][] {
                // null values
                {new String[]{"--primitiveInt", "null"}},
                {new String[]{"--primitiveDouble", "null"}},
                // this case triggers a very different code path; because its a flag option, its not required to
                // have an argument value, and because the value "null" fails StrictBooleanConverter, the parser
                // treats it is a flag with no argument value, followed by a positional argument value "null"
                {new String[]{"--primitiveBoolean", "null"}},
        };
    }

    @Test(dataProvider = "nonNullableArgs", expectedExceptions = CommandLineException.BadArgumentValue.class)
    public void testSetNonNullableArgumentsToNull(final String[] args) throws Exception {
        final NonNullableArguments o = new NonNullableArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        Assert.assertTrue(clp.parseArguments(System.err, args));
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testWithBoundariesArgumentsForNoNumeric() {
        @CommandLineProgramProperties(summary = "broken tool",
                oneLineSummary = "broken tool",
                programGroup = TestProgramGroup.class)
        class WithBoundariesArgumentsForString {
            @Argument(doc = "String argument", optional = true, minValue = 0, maxValue = 30)
            public String stringArg = "string";
        }
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(new WithBoundariesArgumentsForString());
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testWithDoubleBoundariesArgumentsForInteger() {
        @CommandLineProgramProperties(summary = "broken tool",
                oneLineSummary = "broken tool",
                programGroup = TestProgramGroup.class)
        class WithDoubleBoundariesArgumentsForInteger {
            @Argument(doc = "Integer argument", minValue = 0.1, maxValue = 0.5)
            public Integer integerArg;
        }
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(new WithDoubleBoundariesArgumentsForInteger());
    }

    @CommandLineProgramProperties(
            summary = "tool with boundaries",
            oneLineSummary = "tools with boundaries",
            programGroup = TestProgramGroup.class
    )
    public class WithBoundariesArguments {
        // recommended values are not explicitly verified by the tests, but do force the code through the warning code paths
        @Argument(doc = "Double argument in the range [10, 20]", optional = true, minValue = 10, minRecommendedValue = 16, maxRecommendedValue = 17, maxValue = 20)
        public Double doubleArg = 15d;
        // recommended values are not explicitly verified by the tests, but do force the code through the warning code paths
        @Argument(doc = "Integer in the range [0, 30]", optional = true, minValue = 0, minRecommendedValue = 10, maxRecommendedValue = 15, maxValue = 30)
        public int integerArg = 20;
    }

    @DataProvider
    public Object[][] withinBoundariesArgs() {
        return new Object[][]{
            {new String[]{"--integerArg", "0"}, 15, 0},
            {new String[]{"--integerArg", "10"}, 15, 10},
            {new String[]{"--integerArg", "30"}, 15, 30},
            {new String[]{"--doubleArg", "10"}, 10, 20},
            {new String[]{"--doubleArg", "12"}, 12, 20},
            {new String[]{"--doubleArg", "16"}, 16, 20},
            {new String[]{"--doubleArg", "18"}, 18, 20},
            {new String[]{"--doubleArg", "20"}, 20, 20}
        };
    }

    @Test(dataProvider = "withinBoundariesArgs")
    public void testWithinBoundariesArguments(final String[] argv, final double expectedDouble, final int expectedInteger) throws Exception {
        final WithBoundariesArguments o = new WithBoundariesArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        Assert.assertTrue(clp.parseArguments(System.err, argv));
        Assert.assertEquals(o.doubleArg, expectedDouble);
        Assert.assertEquals(o.integerArg, expectedInteger);
    }

    @DataProvider
    public Object[][] outOfRangeArgs() {
        return new Object[][]{
                {new String[]{"--integerArg", "-45"}},
                {new String[]{"--integerArg", "-1"}},
                {new String[]{"--integerArg", "31"}},
                {new String[]{"--integerArg", "106"}},
                {new String[]{"--doubleArg", "-1"}},
                {new String[]{"--doubleArg", "0"}},
                {new String[]{"--doubleArg", "21"}}
        };
    }

    @Test(dataProvider = "outOfRangeArgs", expectedExceptions = CommandLineException.OutOfRangeArgumentValue.class)
    public void testOutOfRangesArguments(final String[] argv) throws Exception {
        final WithBoundariesArguments o = new WithBoundariesArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(o);
        clp.parseArguments(System.err, argv);
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testHiddenRequiredArgumentThrowException() throws Exception {
        @CommandLineProgramProperties(summary = "tool with required and hidden argument",
                oneLineSummary = "tool with required and hidden argument",
                programGroup = TestProgramGroup.class)
        final class ToolWithRequiredHiddenArgument {
            @Argument(fullName = "hiddenTestArgument", shortName = "hiddenTestArgument", doc = "Hidden argument", optional = false)
            @Hidden
            public Integer hidden;
        }
        new CommandLineArgumentParser(new ToolWithRequiredHiddenArgument());
    }

    @Test
    public void testHiddenArguments() throws Exception {
        @CommandLineProgramProperties(summary = "test",
                oneLineSummary = "test",
                programGroup = TestProgramGroup.class)
        final class ToolWithHiddenArgument {
            @Argument(fullName = "hiddenTestArgument", shortName = "hiddenTestArgument", doc = "Hidden argument", optional = true)
            @Hidden
            public Integer hidden = 0;
        }

        final ToolWithHiddenArgument tool = new ToolWithHiddenArgument();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(tool);
        // test that it is not printed in the usage
        String out = clp.usage(true, false); // with common args, without hidden
        Assert.assertEquals(out.indexOf("hiddenTestArgument"), -1, out);
        out = clp.usage(true, true); // with common and hidden args
        Assert.assertNotEquals(out.indexOf("hiddenTestArgument"), -1, out);
        // test that it is parsed from the command line if specified
        clp.parseArguments(System.err, new String[]{"--hiddenTestArgument", "10"});
        Assert.assertEquals(tool.hidden.intValue(), 10);
    }

    @Test
    public void testAdvancedArguments() throws Exception {
        @CommandLineProgramProperties(summary = "test",
                oneLineSummary = "test",
                programGroup = TestProgramGroup.class)
        final class ToolWithAdvancedArgument {
            @Argument(fullName = "advancedTestArgument", shortName = "advancedTestArgument", doc = "Advanced argument", optional = true)
            @Advanced
            public Integer advanced = 0;
        }

        final CommandLineParser clp = new CommandLineArgumentParser(new ToolWithAdvancedArgument());
        // test that it is printed in the usage
        final String out = clp.usage(true, false); // with common args, without hidden
        Assert.assertTrue(out.contains("advancedTestArgument"), out);
    }

    @Test(expectedExceptions = CommandLineException.CommandLineParserInternalException.class)
    public void testRequiredAdvancedNotAllowed() throws Exception {
        @CommandLineProgramProperties(summary = "test",
                oneLineSummary = "test",
                programGroup = TestProgramGroup.class)
        final class ToolWithRequiredAdvancedArgument {
            @Advanced
            @Argument(optional = false)
            public String invalid;
        }
        new CommandLineArgumentParser(new ToolWithRequiredAdvancedArgument());
    }

    /***************************************************************************************
     * Start of tests and helper classes for CommandLineParser.gatherArgumentValuesOfType()
     ***************************************************************************************/

    /**
     * Classes and argument collections for use with CommandLineParser.gatherArgumentValuesOfType() tests below.
     *
     * Structured to ensure that we test support for:
     *
     * -distinguishing between arguments of the target type, and arguments not of the target type
     * -distinguishing between annotated and unannotated fields of the target type
     * -gathering arguments that are a subtype of the target type
     * -gathering multi-valued arguments of the target type within Collection types
     * -gathering arguments of the target type that are not specified on the command line
     * -gathering arguments of the target type from superclasses of our tool
     * -gathering arguments of the target type from argument collections
     * -gathering arguments when the target type is itself a parameterized type (eg., FeatureInput<VariantContext>)
     */

    private static class GatherArgumentValuesTestSourceParent {
        @Argument(fullName = "parentSuperTypeTarget", shortName = "parentSuperTypeTarget", doc = "")
        private GatherArgumentValuesTargetSuperType parentSuperTypeTarget;

        @Argument(fullName = "parentSubTypeTarget", shortName = "parentSubTypeTarget", doc = "")
        private GatherArgumentValuesTargetSubType parentSubTypeTarget;

        @Argument(fullName = "parentListSuperTypeTarget", shortName = "parentListSuperTypeTarget", doc = "")
        private List<GatherArgumentValuesTargetSuperType> parentListSuperTypeTarget;

        @Argument(fullName = "parentListSubTypeTarget", shortName = "parentListSubTypeTarget", doc = "")
        private List<GatherArgumentValuesTargetSubType> parentListSubTypeTarget;

        @Argument(fullName = "uninitializedParentTarget", shortName = "uninitializedParentTarget", optional = true, doc = "")
        private GatherArgumentValuesTargetSuperType uninitializedParentTarget;

        @Argument(fullName = "parentNonTargetArgument", shortName = "parentNonTargetArgument", doc = "")
        private int parentNonTargetArgument;

        private GatherArgumentValuesTargetSuperType parentUnannotatedTarget;

        @ArgumentCollection
        private GatherArgumentValuesTestSourceParentCollection parentCollection = new GatherArgumentValuesTestSourceParentCollection();
    }

    private static class GatherArgumentValuesTestSourceChild extends GatherArgumentValuesTestSourceParent {
        @Argument(fullName = "childSuperTypeTarget", shortName = "childSuperTypeTarget", doc = "")
        private GatherArgumentValuesTargetSuperType childSuperTypeTarget;

        @Argument(fullName = "childSubTypeTarget", shortName = "childSubTypeTarget", doc = "")
        private GatherArgumentValuesTargetSubType childSubTypeTarget;

        @Argument(fullName = "childListSuperTypeTarget", shortName = "childListSuperTypeTarget", doc = "")
        private List<GatherArgumentValuesTargetSuperType> childListSuperTypeTarget;

        @Argument(fullName = "childListSubTypeTarget", shortName = "childListSubTypeTarget", doc = "")
        private List<GatherArgumentValuesTargetSubType> childListSubTypeTarget;

        @Argument(fullName = "uninitializedChildTarget", shortName = "uninitializedChildTarget", optional = true, doc = "")
        private GatherArgumentValuesTargetSuperType uninitializedChildTarget;

        @Argument(fullName = "uninitializedChildListTarget", shortName = "uninitializedChildListTarget", optional = true, doc = "")
        private List<GatherArgumentValuesTargetSuperType> uninitializedChildListTarget;

        @Argument(fullName = "childNonTargetArgument", shortName = "childNonTargetArgument", doc = "")
        private int childNonTargetArgument;

        @Argument(fullName = "childNonTargetListArgument", shortName = "childNonTargetListArgument", doc = "")
        private List<Integer> childNonTargetListArgument;

        private GatherArgumentValuesTargetSuperType childUnannotatedTarget;

        @ArgumentCollection
        private GatherArgumentValuesTestSourceChildCollection childCollection = new GatherArgumentValuesTestSourceChildCollection();
    }

    private static class GatherArgumentValuesTestSourceParentCollection {
        private static final long serialVersionUID = 1L;

        @Argument(fullName = "parentCollectionSuperTypeTarget", shortName = "parentCollectionSuperTypeTarget", doc = "")
        private GatherArgumentValuesTargetSuperType parentCollectionSuperTypeTarget;

        @Argument(fullName = "parentCollectionSubTypeTarget", shortName = "parentCollectionSubTypeTarget", doc = "")
        private GatherArgumentValuesTargetSubType parentCollectionSubTypeTarget;

        @Argument(fullName = "uninitializedParentCollectionTarget", shortName = "uninitializedParentCollectionTarget", optional = true, doc = "")
        private GatherArgumentValuesTargetSuperType uninitializedParentCollectionTarget;

        @Argument(fullName = "parentCollectionNonTargetArgument", shortName = "parentCollectionNonTargetArgument", doc = "")
        private int parentCollectionNonTargetArgument;

        private GatherArgumentValuesTargetSuperType parentCollectionUnannotatedTarget;
    }

    private static class GatherArgumentValuesTestSourceChildCollection {
        private static final long serialVersionUID = 1L;

        @Argument(fullName = "childCollectionSuperTypeTarget", shortName = "childCollectionSuperTypeTarget", doc = "")
        private GatherArgumentValuesTargetSuperType childCollectionSuperTypeTarget;

        @Argument(fullName = "childCollectionSubTypeTarget", shortName = "childCollectionSubTypeTarget", doc = "")
        private GatherArgumentValuesTargetSubType childCollectionSubTypeTarget;

        @Argument(fullName = "childCollectionListSuperTypeTarget", shortName = "childCollectionListSuperTypeTarget", doc = "")
        private List<GatherArgumentValuesTargetSuperType> childCollectionListSuperTypeTarget;

        @Argument(fullName = "uninitializedChildCollectionTarget", shortName = "uninitializedChildCollectionTarget", optional = true, doc = "")
        private GatherArgumentValuesTargetSuperType uninitializedChildCollectionTarget;

        @Argument(fullName = "childCollectionNonTargetArgument", shortName = "childCollectionNonTargetArgument", doc = "")
        private int childCollectionNonTargetArgument;

        private GatherArgumentValuesTargetSuperType childCollectionUnannotatedTarget;
    }

    /**
     * Our tests will search for argument values of this type, subtypes of this type, and Collections of
     * this type or its subtypes. Has a String constructor so that the argument parsing system can correctly
     * initialize it.
     */
    private static class GatherArgumentValuesTargetSuperType {
        private String value;

        public GatherArgumentValuesTargetSuperType( String s ) {
            value = s;
        }

        public String getValue() {
            return value;
        }
    }

    private static class GatherArgumentValuesTargetSubType extends GatherArgumentValuesTargetSuperType {
        public GatherArgumentValuesTargetSubType( String s ) {
            super(s);
        }
    }

    @DataProvider(name = "gatherArgumentValuesOfTypeDataProvider")
    public Object[][] gatherArgumentValuesOfTypeDataProvider() {
        // Non-Collection arguments of the target type
        final List<String> targetScalarArguments = Arrays.asList("childSuperTypeTarget", "childSubTypeTarget",
                                                                 "parentSuperTypeTarget", "parentSubTypeTarget",
                                                                 "childCollectionSuperTypeTarget", "childCollectionSubTypeTarget",
                                                                 "parentCollectionSuperTypeTarget", "parentCollectionSubTypeTarget");
        // Collection arguments of the target type
        final List<String> targetListArguments = Arrays.asList("childListSuperTypeTarget", "childListSubTypeTarget",
                                                               "parentListSuperTypeTarget", "parentListSubTypeTarget",
                                                               "childCollectionListSuperTypeTarget");
        // Arguments of the target type that we won't specify on our command line
        final List<String> uninitializedTargetArguments = Arrays.asList("uninitializedChildTarget", "uninitializedChildListTarget",
                                                                        "uninitializedParentTarget", "uninitializedChildCollectionTarget",
                                                                        "uninitializedParentCollectionTarget");
        // Arguments not of the target type
        final List<String> nonTargetArguments = Arrays.asList("childNonTargetArgument", "parentNonTargetArgument",
                                                              "childCollectionNonTargetArgument", "parentCollectionNonTargetArgument",
                                                              "childNonTargetListArgument");

        List<String> commandLineArguments = new ArrayList<>();
        List<Pair<String, String>> sortedExpectedGatheredValues = new ArrayList<>();

        for ( String targetScalarArgument : targetScalarArguments ) {
            final String argumentValue = targetScalarArgument + "Value";

            commandLineArguments.add("--" + targetScalarArgument);
            commandLineArguments.add(argumentValue);
            sortedExpectedGatheredValues.add(Pair.of(targetScalarArgument, argumentValue));
        }

        // Give each list argument multiple values
        for ( String targetListArgument : targetListArguments ) {
            for ( int argumentNum = 1; argumentNum <= 3; ++argumentNum ) {
                final String argumentValue = targetListArgument + "Value" + argumentNum;

                commandLineArguments.add("--" + targetListArgument);
                commandLineArguments.add(argumentValue);
                sortedExpectedGatheredValues.add(Pair.of(targetListArgument, argumentValue));
            }
        }

        // Make sure the uninitialized args of the target type not included on the command line are
        // represented in the expected output
        for ( String uninitializedTargetArgument : uninitializedTargetArguments ) {
            sortedExpectedGatheredValues.add(Pair.of(uninitializedTargetArgument, null));
        }

        // The non-target args are all of type int, so give them an arbitrary int value on the command line.
        // These should not be gathered at all, so are not added to the expected output.
        for ( String nonTargetArgument : nonTargetArguments ) {
            commandLineArguments.add("--" + nonTargetArgument);
            commandLineArguments.add("1");
        }

        Collections.sort(sortedExpectedGatheredValues);

        return new Object[][] {{
            commandLineArguments, sortedExpectedGatheredValues
        }};
    }

    @Test(dataProvider = "gatherArgumentValuesOfTypeDataProvider")
    public void testGatherArgumentValuesOfType( final List<String> commandLineArguments, final List<Pair<String, String>> sortedExpectedGatheredValues ) {
        GatherArgumentValuesTestSourceChild argumentSource = new GatherArgumentValuesTestSourceChild();

        // Parse the command line, and inject values into our test instance
        CommandLineArgumentParser clp = new CommandLineArgumentParser(argumentSource);
        clp.parseArguments(System.err, commandLineArguments.toArray(new String[commandLineArguments.size()]));

        // Gather all argument values of type GatherArgumentValuesTargetSuperType (or Collection<GatherArgumentValuesTargetSuperType>),
        // including subtypes.
        List<Pair<ArgumentDefinition, GatherArgumentValuesTargetSuperType>> gatheredArguments =
                clp.gatherArgumentValuesOfType(GatherArgumentValuesTargetSuperType.class);

        // Make sure we gathered the expected number of argument values
        Assert.assertEquals(gatheredArguments.size(), sortedExpectedGatheredValues.size(), "Gathered the wrong number of arguments");

        // Make sure actual gathered argument values match expected values
        List<Pair<String, String>> sortedActualGatheredArgumentValues = new ArrayList<>();
        for ( Pair<ArgumentDefinition, GatherArgumentValuesTargetSuperType> gatheredArgument : gatheredArguments ) {
            Assert.assertNotNull(gatheredArgument.getKey().getUnderlyingField().getAnnotation(Argument.class), "Gathered argument is not annotated with an @Argument annotation");

            String argumentName = gatheredArgument.getKey().getUnderlyingField().getAnnotation(Argument.class).fullName();
            GatherArgumentValuesTargetSuperType argumentValue = gatheredArgument.getValue();

            sortedActualGatheredArgumentValues.add(Pair.of(argumentName, argumentValue != null ? argumentValue.getValue() : null));
        }
        Collections.sort(sortedActualGatheredArgumentValues);

        Assert.assertEquals(sortedActualGatheredArgumentValues, sortedExpectedGatheredValues,
                            "One or more gathered argument values not correct");
    }

    /**
     * Nonsensical parameterized class, just to ensure that CommandLineParser.gatherArgumentValuesOfType()
     * can gather argument values of a generic type
     *
     * @param <T> meaningless type parameter
     */
    private static class GatherArgumentValuesParameterizedTargetType<T> {
        private String value;
        private T foo;

        public GatherArgumentValuesParameterizedTargetType( String s ) {
            value = s;
            foo = null;
        }

        public String getValue() {
            return value;
        }
    }

    private static class GatherArgumentValuesParameterizedTypeSource {
        @Argument(fullName = "parameterizedTypeArgument", shortName = "parameterizedTypeArgument", doc = "")
        private GatherArgumentValuesParameterizedTargetType<Integer> parameterizedTypeArgument;

        @Argument(fullName = "parameterizedTypeListArgument", shortName = "parameterizedTypeListArgument", doc = "")
        private List<GatherArgumentValuesParameterizedTargetType<Integer>> parameterizedTypeListArgument;
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testGatherArgumentValuesOfTypeWithParameterizedType() {
        GatherArgumentValuesParameterizedTypeSource argumentSource = new GatherArgumentValuesParameterizedTypeSource();

        // Parse the command line, and inject values into our test instance
        CommandLineArgumentParser clp = new CommandLineArgumentParser(argumentSource);
        clp.parseArguments(System.err, new String[]{"--parameterizedTypeArgument", "parameterizedTypeArgumentValue",
                                                    "--parameterizedTypeListArgument", "parameterizedTypeListArgumentValue"});

        // Gather argument values of the raw type GatherArgumentValuesParameterizedTargetType, and make
        // sure that we match fully-parameterized declarations
        List<Pair<ArgumentDefinition, GatherArgumentValuesParameterizedTargetType>> gatheredArguments =
                clp.gatherArgumentValuesOfType(GatherArgumentValuesParameterizedTargetType.class);

        Assert.assertEquals(gatheredArguments.size(), 2, "Wrong number of arguments gathered");

        Assert.assertNotNull(gatheredArguments.get(0).getKey().getUnderlyingField().getAnnotation(Argument.class), "Gathered argument is not annotated with an @Argument annotation");
        Assert.assertEquals(gatheredArguments.get(0).getKey().getUnderlyingField().getAnnotation(Argument.class).fullName(), "parameterizedTypeArgument", "Wrong argument gathered");
        Assert.assertEquals(gatheredArguments.get(0).getValue().getValue(), "parameterizedTypeArgumentValue", "Wrong value for gathered argument");
        Assert.assertNotNull(gatheredArguments.get(1).getKey().getUnderlyingField().getAnnotation(Argument.class), "Gathered argument is not annotated with an @Argument annotation");
        Assert.assertEquals(gatheredArguments.get(1).getKey().getUnderlyingField().getAnnotation(Argument.class).fullName(), "parameterizedTypeListArgument", "Wrong argument gathered");
        Assert.assertEquals(gatheredArguments.get(1).getValue().getValue(), "parameterizedTypeListArgumentValue", "Wrong value for gathered argument");
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

    @Test
    public void testLongPreamble() {
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(new CommandLineArgumentParserTest.TestParserLongPreamble());

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream stream = new PrintStream(byteArrayOutputStream);
        clp.parseArguments(stream, new String[]{});
        stream.append(clp.usage(true, true));

        final String expected = "USAGE: TestParserLongPreamble [arguments]\n" +
                "\n" +
                "X &lt; Y This is the first row but it's really long and it has lots of words...really big words. Because it knows the\n" +
                "best people and really has lots of interesting things it needs to get across. Definitely more than can fit in 140\n" +
                "characters...";

        final String result = byteArrayOutputStream.toString();
        Assert.assertEquals(result.substring(0, expected.length()), expected);
    }


    /***************************************************************************************
     * End of tests and helper classes for CommandLineParser.gatherArgumentValuesOfType()
     ***************************************************************************************/
}
