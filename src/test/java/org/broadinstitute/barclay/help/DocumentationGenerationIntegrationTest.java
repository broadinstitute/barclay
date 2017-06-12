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

    private static String inputResourcesDir = "src/main/resources/org/broadinstitute/barclay/";
    private static String testResourcesDir = "src/test/resources/org/broadinstitute/barclay/";

    private static final String indexFileName = "index";
    private static final String jsonFileExtension = ".json";

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

    private static final List<String> EXPECTED_OUTPUT_FILE_NAME_PREFIXES = Arrays.asList(
            "org_broadinstitute_barclay_help_TestArgumentContainer",
            "org_broadinstitute_barclay_help_TestExtraDocs"
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
                {HelpDoclet.class,
                        new File(inputResourcesDir + "helpTemplates/"),
                        new File(testResourcesDir + "help/expected/HelpDoclet"),
                        "html", // testIndexFileExtension
                        "html", // testOutputFileExtension
                        "html", // requestedIndexFileExtension
                        "html"  // requestedOutputFileExtension
                },
                // defaut doclet and templates using alternate index extension
                {HelpDoclet.class,
                        new File(inputResourcesDir + "helpTemplates/"),
                        new File(testResourcesDir + "help/expected/HelpDoclet"),
                        "html",  // testIndexFileExtension
                        "html",  // testOutputFileExtension
                        "xhtml", // requestedIndexFileExtension
                        "html"   // requestedOutputFileExtension
                },
                // custom doclet and templates
                {TestDoclet.class,
                        new File(testResourcesDir + "help/templates/TestDoclet/"),
                        new File(testResourcesDir + "help/expected/TestDoclet"),
                        "html", // testIndexFileExtension
                        "html", // testOutputFileExtension
                        "html", // requestedIndexFileExtension
                        "html"  // requestedOutputFileExtension
                }
        };
    }

    @Test(dataProvider = "getDocGenTestParams")
    public void testDocGenRoundTrip(
            final Class<?> docletClass,
            final File inputTemplatesFolder,
            final File expectedDir,
            final String testIndexFileExtension,
            final String testOutputFileExtension,
            final String requestedIndexFileExtension,
            final String requestedOutputFileExtension
    ) throws IOException
    {
        // creates a temp output directory
        final File outputDir = Files.createTempDirectory(docletClass.getName()).toAbsolutePath().toFile();
        outputDir.deleteOnExit();

        // run javadoc with the custom doclet
        com.sun.tools.javadoc.Main.execute(
                docArgList(docletClass, inputTemplatesFolder, outputDir, requestedIndexFileExtension, requestedOutputFileExtension)
        );

        // Compare index files
        assertFileContentsIdentical(
                new File(outputDir, indexFileName + "." + requestedIndexFileExtension),
                new File(expectedDir, indexFileName + "." + testIndexFileExtension));

        // Compare output files (json and workunit)
        for (final String workUnitFileNamePrefix: EXPECTED_OUTPUT_FILE_NAME_PREFIXES) {
            //check json file
            assertFileContentsIdentical(
                    //TODO: its a bug that we include the requestedOutputFileExtension in the json file extension; the file
                    // name should really just be workUnitName.json instead of workUnitName.extension.json
                    new File(outputDir, workUnitFileNamePrefix + "." + requestedOutputFileExtension + jsonFileExtension),
                    new File(expectedDir, workUnitFileNamePrefix + "." + testOutputFileExtension + jsonFileExtension));
            // check workunit output file
            assertFileContentsIdentical(
                    new File(outputDir, workUnitFileNamePrefix + "." + requestedOutputFileExtension),
                    new File(expectedDir, workUnitFileNamePrefix + "." + testOutputFileExtension));
        }
    }

    private void assertFileContentsIdentical(
            final File actualFile,
            final File expectedFile) throws IOException {
        byte[] actualBytes = Files.readAllBytes(actualFile.toPath());
        byte[] expectedBytes = Files.readAllBytes(expectedFile.toPath());
        Assert.assertEquals(actualBytes, expectedBytes);
    }
}
