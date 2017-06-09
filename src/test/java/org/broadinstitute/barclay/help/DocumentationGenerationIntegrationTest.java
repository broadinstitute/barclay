package org.broadinstitute.barclay.help;


import org.testng.Assert;
import org.testng.annotations.DataProvider;
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

    // common arguments not changed for tests
    private static final List<String> COMMON_DOC_ARG_LIST = Arrays.asList(
            "-build-timestamp", "2016/01/01 01:01:01",      // dummy, constant timestamp
            "-absolute-version", "11.1",                    // dummy version
            "-docletpath", "build/libs",
            "-sourcepath", "src/test/java",
            "org.broadinstitute.barclay.help",
            "org.broadinstitute.barclay.argparser",
            "-verbose",
            "-cp", System.getProperty("java.class.path")
    );

    private static final List<String> DOCUMENTED_PHP_NAMES_WITHOUT_EXTENSION = Arrays.asList(
            "org_broadinstitute_barclay_help_TestArgumentContainer.",
            "org_broadinstitute_barclay_help_TestArgumentContainer.",
            "org_broadinstitute_barclay_help_TestExtraDocs.",
            "org_broadinstitute_barclay_help_TestExtraDocs."
    );

    private static String[] docArgList(final Class<?> docletClass, final File templatesFolder, final File outputDir,
            final String indexFileExtension, final String outputFileExtension) {
        // set the common arguments
        final List<String> docArgList = new ArrayList<>(COMMON_DOC_ARG_LIST);
        // set the templates
        docArgList.add("-settings-dir");
        docArgList.add(templatesFolder.getAbsolutePath());
        // set the output directory
        docArgList.add("-d");
        docArgList.add(outputDir.getAbsolutePath());
        // set the doclet class
        docArgList.add("-doclet");
        docArgList.add(docletClass.getName());
        // set the index and output extension
        docArgList.add("-index-file-extension");
        docArgList.add(indexFileExtension);
        docArgList.add("-output-file-extension");
        docArgList.add(outputFileExtension);
        return docArgList.toArray(new String[]{});
    }

    @DataProvider
    public Object[][] getDocGenTestParams() {
        return new Object[][] {
                // default doclet and templates
                {HelpDoclet.class, new File("src/main/resources/org/broadinstitute/barclay/helpTemplates/"), new File("src/test/resources/org/broadinstitute/barclay/help/expected/HelpDoclet"), "html", "html"},
                // custom doclet and templates
                {TestDoclet.class, new File("src/test/resources/org/broadinstitute/barclay/help/templates/TestDoclet"), new File("src/test/resources/org/broadinstitute/barclay/help/expected/TestDoclet"), "html", "html"}
        };
    }

    @Test(dataProvider = "getDocGenTestParams")
    public void testDocGenRoundTrip(final Class<?> docletClass, final File templatesFolder, final File expectedDir,
            final String indexFileExtension, final String outputFileExtension) throws IOException {
        // creates a temp output directory
        final File outputDir = Files.createTempDirectory(docletClass.getName()).toAbsolutePath().toFile();
        outputDir.deleteOnExit();

        // run javadoc with the the custom doclet
        com.sun.tools.javadoc.Main.execute(docArgList(docletClass, templatesFolder, outputDir, indexFileExtension, outputFileExtension));

        // Compare index files
        Assert.assertTrue(filesContentsIdentical(new File(expectedDir, "index." + indexFileExtension), new File(outputDir, "index.html")));

        // Compare output files (json and html)
        for (final String prefix: DOCUMENTED_PHP_NAMES_WITHOUT_EXTENSION) {
            Assert.assertTrue(filesContentsIdentical(new File(outputDir, prefix + "html.json"), new File(expectedDir, prefix + "html.json")));
            Assert.assertTrue(filesContentsIdentical(new File(outputDir, prefix + outputFileExtension), new File(expectedDir, prefix + "html")));
        }
    }

    private boolean filesContentsIdentical(
            final File actualFile,
            final File expectedFile) throws IOException {
        byte[] actualBytes = Files.readAllBytes(actualFile.toPath());
        byte[] expectedBytes = Files.readAllBytes(expectedFile.toPath());
        return Arrays.equals(actualBytes, expectedBytes);
    }
}
