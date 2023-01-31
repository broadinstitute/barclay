package org.broadinstitute.barclay.argparser;

import java.util.List;

/**
 * Interface implemented by command line programs that supply plugins to the command line parser.
 */
public interface CommandLinePluginProvider {
    List<? extends CommandLinePluginDescriptor<?>> getPluginDescriptors();
}
