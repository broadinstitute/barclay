package org.broadinstitute.barclay.help.testinputs;


import org.broadinstitute.barclay.argparser.*;
import org.broadinstitute.barclay.argparser.CommandLinePluginProvider;
import org.broadinstitute.barclay.argparser.CommandLinePluginUnitTest;
import org.broadinstitute.barclay.help.DocumentedFeature;

import java.io.File;
import java.util.*;

/**
 * Argument container class for testing documentation generation. Contains an argument
 * for each @Argument, @ArgumentCollection, and @DocumentedFeature property that should
 * be tested.
 *
 * Test custom tag:
 * {@MyTag.Type testType}
 *
 * <p>
 * The purpose of this paragraph is to test embedded html formatting.
 * <ol>
 *     <li>This is point number 1</li>
 *     <li>This is point number 2</li>
 * </ol>
 * </p>
 */
@CommandLineProgramProperties(
        summary = TestArgumentContainer.SUMMARY,
        oneLineSummary = TestArgumentContainer.ONE_LINE_SUMMARY,
        programGroup = TestProgramGroup.class)
@BetaFeature
@WorkflowProperties(memory ="3G")
@DocumentedFeature(groupName = TestArgumentContainer.GROUP_NAME, extraDocs = TestExtraDocs.class)
public class TestArgumentContainer implements CommandLinePluginProvider {

    public static final String SUMMARY = "Test tool summary";
    public static final String ONE_LINE_SUMMARY = "Argument container class for testing documentation generation.";
    public static final String GROUP_NAME = "Test feature group name";

    public TestArgumentContainer() {
        argBaseClass = new TestBaseArgumentType() {

            /**
             * Note: Javadoc ignores comments for anonymous/inner classes, so this comment does not
             * get propagated to the fulltext for this argument.
             */
            @Argument(shortName="anonymousClassArg", fullName="fullAnonymousArgName", optional=true, doc="Test anonymous class arg")
            public List<File> anonymousClassArg;
        };
    }

    @ArgumentCollection
    public TestBaseArgumentType argBaseClass;

    /**
     * Positional arguments
     */
    @PositionalArguments(
            minElements = 2,
            maxElements = 2,
            doc = "Positional arguments, min = 2, max = 2")
    public List<File> positionalArguments;

    /**
     * Optional file list.
     */
    @Argument(fullName = "optionalFileList",
            shortName = "optFilList",
            doc = "Optional file list",
            optional = true)
    public List<File> optionalFileList;

    /**
     * Required file list.
     */
    @WorkflowOutput(requiredCompanions ={"companionDictionary", "companionIndex"})
    @Argument(fullName = "requiredFileList",
            shortName = "reqFilList",
            doc = "Required file list",
            optional = false)
    public List<File> requiredFileList;

    /**
     * Optional string list.
     */
    @Argument(fullName = "optionalStringList",
            shortName = "optStrList",
            doc = "An optional list of strings",
            optional = true)
    public List<String> optionalStringList = Collections.emptyList();

    /**
     * Required string list.
     */
    @Argument(fullName = "requiredStringList",
            shortName = "reqStrList",
            doc = "A required list of strings",
            optional = false)
    public List<String> requiredStringList = Collections.emptyList();

    /**
     * Required double
     */
    @Argument(fullName = "optionalDouble",
            shortName = "optDouble",
            doc = "Optionals double with initial value 2.15",
            optional = true)
     protected double optionalDouble = 2.15;

    /**
     * Optional double list.
     */
    @Argument(fullName = "optionalDoubleList",
            shortName = "optDoubleList",
            doc = "optionalDoubleList with initial values: 100.0, 99.9, 99.0, 90.0",
            optional = true)
    private List<Double> optionalDoubleList = new ArrayList<Double>(Arrays.asList(100.0, 99.9, 99.0, 90.0));

    /**
     * Optional flag.
     */
    @Argument(fullName = "optionalFlag",
            shortName = "optFlag",
            doc = "Optional flag, defaults to false.",
            optional = true)
    private boolean optionalFlag = false;

    /**
     * Hidden, optional list. This should not be displayed in normal help output.
     */
    @Hidden
    @Argument(fullName = "hiddenOptionalList",
            shortName = "hiddenOptList",
            doc = "*******ERROR*******: this is supposed to be hidden so you shouldn't be seeing it in the doc output",
            optional = true)
    private ArrayList<Double> hiddenOptionalList = new ArrayList<>();

    /**
     * Advanced, Optional int
     */
    @Advanced
    @Argument(fullName = "advancedOptionalInt",
            shortName = "advancedOptInt",
            doc = "advancedOptionalInt with initial value 1", optional = true)
    protected int optionalInt = 1;

    /**
     * Deprecated string
     */
    @Deprecated
    @Argument(fullName = "deprecatedString",
            shortName = "depStr",
            doc = "deprecated", optional = true)
    protected int deprecatedString;

    @Argument(doc="Use field name if no name in annotation.")
    protected String usesFieldNameForArgName;

    //////////////////////////////////////////////////////////////////////
    // Embedded argument collection
    @ArgumentCollection
    private TestArgumentCollection testArugmentCollection = new TestArgumentCollection();

    //////////////////////////////////////////////////////////////////////
    // clpEnum
    public enum TestEnum implements CommandLineParser.ClpEnum {

        ENUM_VALUE_1("This is enum value 1."),
        ENUM_VALUE_2("This is enum value 2.");

        private String helpdoc;

        TestEnum(final String helpdoc) {
            this.helpdoc = helpdoc;
        }

        @Override
        public String getHelpDoc() {
            return helpdoc;
        }
    };

    @Argument(fullName = "optionalClpEnum",
            shortName = "optionalClpEnum",
            doc = "Optional Clp enum",
            optional = true)
    TestEnum optionalClpEnum = TestEnum.ENUM_VALUE_1;

    @Argument(fullName = "requiredClpEnum",
            shortName = "requiredClpEnum",
            doc = "Required Clp enum",
            optional = false)
    TestEnum requiredClpEnum;

    /**
     * Mutually exclusive args
     */
    @Argument(shortName = "mutexSourceField", fullName = "mutexSourceField",
            mutex = {"mutexTargetField1", "mutexTargetField2"},
            optional = true)
    public List<File> mutexSourceField;

    @Argument(shortName = "mutexTargetField1", fullName= "mutexTargetField1",
            doc = "SAM/BAM/CRAM file(s) with alignment data from the first read of a pair.",
            mutex = {"mutexSourceField"},
            optional = true)
    public List<File> mutexTargetField1;

    @Argument(shortName = "mutexTargetField2", fullName= "mutexTargetField2",
            doc = "SAM/BAM file(s) with alignment data from the second read of a pair.",
            mutex = {"mutexSourceField"},
            optional = true)
    public List<File> mutexTargetField2;

    @Argument(shortName = "ES", fullName = "enumSetLong",
            doc = "Some set thing.",
            optional = true)
    public EnumSet<TestEnum> anEnumSet = EnumSet.of(TestEnum.ENUM_VALUE_1);

    // Command line plugin descriptor with optional arguments
    @Override
    public List<? extends CommandLinePluginDescriptor<?>> getPluginDescriptors() {
        return Collections.singletonList(
                new CommandLinePluginUnitTest.TestPluginDescriptor(
                        Collections.singletonList(new CommandLinePluginUnitTest.TestDefaultPlugin())));
    }
}
