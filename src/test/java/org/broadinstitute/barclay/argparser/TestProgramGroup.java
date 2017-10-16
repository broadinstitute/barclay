package org.broadinstitute.barclay.argparser;

/**
 * only for testing
 */
public final class TestProgramGroup implements CommandLineProgramGroup {

    public static final String NAME = "TestProgramGroup";
    public static final String DESCRIPTION = "Test program group used for testing";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}