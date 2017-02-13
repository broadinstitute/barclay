package org.broadinstitute.barclay.help;


import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Integration test for documentation generation.
 */
public class DocumentationGenerationIntegrationTest {

    @Test
    public void testDocGenRoundTrip() throws IOException {
        File outputDir = Files.createTempDirectory("BarclayDocGen").toAbsolutePath().toFile();
        File expectedDir = new File("src/test/resources/org/broadinstitute/barclay/help/expected/");

        outputDir.deleteOnExit();

        String[] argArray = new String[]{
                "-build-timestamp", "2016/01/01 01:01:01",      // dummy, constant timestamp
                "-absolute-version", "11.1",                    // dummy version
                "-settings-dir", "src/main/resources/helpTemplates",
                "-d", outputDir.getAbsolutePath(),
                "-output-file-extension", "html",
                "-doclet", TestDoclet.class.getName(),
                "-docletpath", "build/libs",
                "-sourcepath", "src/test/java",
                "org.broadinstitute.barclay.help",
                "org.broadinstitute.barclay.argparser",
                "-verbose",
                "-cp", System.getProperty("java.class.path")
        };

        List<String> docArgList = new ArrayList<>();
        docArgList.addAll(Arrays.asList(argArray));

        // run javadoc with the the custom doclet
        com.sun.tools.javadoc.Main.execute(docArgList.toArray(new String[]{}));

        // Compare output files
        Assert.assertTrue(filesContentsIdentical(outputDir, expectedDir, "index.html"));
        Assert.assertTrue(filesContentsIdentical(outputDir, expectedDir,
                "org_broadinstitute_barclay_help_TestArgumentContainer.html"));
        Assert.assertTrue(filesContentsIdentical(outputDir, expectedDir,
                "org_broadinstitute_barclay_help_TestArgumentContainer.html.json"));
        Assert.assertTrue(filesContentsIdentical(outputDir, expectedDir,
                "org_broadinstitute_barclay_help_TestExtraDocs.html"));
        Assert.assertTrue(filesContentsIdentical(outputDir, expectedDir,
                "org_broadinstitute_barclay_help_TestExtraDocs.html.json"));
    }

    public boolean filesContentsIdentical(
            final File actualDir,
            final File expectedDir,
            final String fileName) throws IOException {
        byte[] actualBytes = Files.readAllBytes(new File(actualDir, fileName).toPath());
        byte[] expectedBytes = Files.readAllBytes(new File(expectedDir, fileName).toPath());
        return Arrays.equals(actualBytes, expectedBytes);
    }

}
