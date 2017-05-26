package org.broadinstitute.barclay.argparser;

/**
 * only for testing
 */
public final class TestProgramGroup implements CommandLineProgramGroup {
    @Override
    public String getName() {
        return "TestProgramGroup";
    }

    @Override
    public String getDescription() {
        return "Test program group used for testing";
    }
}