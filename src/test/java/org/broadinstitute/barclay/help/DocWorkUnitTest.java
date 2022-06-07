
    @CommandLineProgramProperties(
            summary = "test CLP deprecation",
            oneLineSummary = "",
            programGroup = TestProgramGroup.class)
    @DeprecatedFeature
    @DocumentedFeature
    public class DeprecatedCLPTest {
    }

    @Test
    public void testDeprecatedCommandLineProgram() {
        final DocWorkUnit deprecatedCLPWorkUnit = createDocWorkUnitForDefaultHandler(DeprecatedCLPTest.class,
                        DocGenMocks.mockClassDoc("", Collections.emptyMap()));
        Assert.assertTrue(deprecatedCLPWorkUnit.isDeprecatedFeature());
        Assert.assertEquals(deprecatedCLPWorkUnit.getDeprecationDetail(), "This feature is deprecated and will be removed in a future release.");
    }

    @Test
    public void testCompareTo() {
        final DocWorkUnit first = createDocWorkUnitForDefaultHandler(TestArgumentContainer.class, DocGenMocks.mockClassDoc("", Collections.emptyMap()));
        final DocWorkUnit second = createDocWorkUnitForDefaultHandler(TestExtraDocs.class, DocGenMocks.mockClassDoc("", Collections.emptyMap()));
        Assert.assertEquals(first.compareTo(first), 0);
        Assert.assertTrue(first.compareTo(second) < 0);
        Assert.assertTrue(second.compareTo(first) > 0);
    }

    private DocWorkUnit createDocWorkUnitForDefaultHandler(
            final Class<?> clazz, final ClassDoc classDoc) {
        return new DocWorkUnit(
                new DefaultDocWorkUnitHandler(new HelpDoclet()),
                clazz.getAnnotation(DocumentedFeature.class),
                classDoc,
                clazz
        );
    }

}