/*
* Copyright 2012-2016 Broad Institute, Inc.
* 
* Permission is hereby granted, free of charge, to any person
* obtaining a copy of this software and associated documentation
* files (the "Software"), to deal in the Software without
* restriction, including without limitation the rights to use,
* copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the
* Software is furnished to do so, subject to the following
* conditions:
* 
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
* OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
* THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package org.broadinstitute.barclay.help;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;

import java.io.*;
import java.util.*;

/**
 * Javadoc Doclet that combines javadoc, Barclay annotations, and FreeMarker
 * templates to produce formatted docs for classes.
 * <p/>
 * The doclet has the following workflow:
 * <p/>
 * 1 -- walk the javadoc hierarchy, looking for class that have the
 * DocumentedFeature documentedFeatureObject
 * 2 -- construct for each a DocWorkUnit, resulting in the complete
 * set of things to document
 * 3 -- for each unit, actually generate a PHP page documenting it
 * as well as links to related features via their units.  Writing
 * of a specific class PHP is accomplished by a generate DocumentationHandler
 * 4 -- write out an index of all units, organized by group
 * 5 -- emit JSON version of Docs using Google GSON (currently incomplete but workable)
 * <p/>
 */
public class HelpDoclet {
    final protected static Logger logger = LogManager.getLogger(HelpDoclet.class);

    // HelpDoclet command line options
    final private static String SETTINGS_DIR_OPTION = "-settings-dir";
    final private static String DESTINATION_DIR_OPTION = "-destination-dir";
    final private static String BUILD_TIMESTAMP_OPTION = "-build-timestamp";
    final private static String ABSOLUTE_VERSION_OPTION = "-absolute-version";
    final private static String INCLUDE_HIDDEN_OPTION = "-hidden-version";
    final private static String OUTPUT_FILE_EXTENSION_OPTION = "-output-file-extension";

    /**
     * Where we find the help FreeMarker templates
     */
    final private static File DEFAULT_SETTINGS_DIR = new File("settings/helpTemplates");

    /**
     * Where we write the PHP directory
     */
    final private static File DEFAULT_DESTINATION_DIR = new File("barclaydocs");

    final private static String DEFAULT_OUTPUT_FILE_EXTENSION = "html";

    // ----------------------------------------------------------------------
    //
    // Variables that are set on the command line when running javadoc
    //
    // ----------------------------------------------------------------------
    protected static File settingsDir = DEFAULT_SETTINGS_DIR;
    protected static File destinationDir = DEFAULT_DESTINATION_DIR;
    protected static String buildTimestamp = "[no timestamp available]";
    protected static String absoluteVersion = "[no version available]";
    protected static boolean showHiddenFeatures = false;
    protected static String outputFileExtension = DEFAULT_OUTPUT_FILE_EXTENSION;

    /**
     * The javadoc root doc
     */
    RootDoc rootDoc;

    /**
     * The set of all things we are going to document
     */
    private Set<DocWorkUnit> workUnits;

    /**
     * Extracts the contents of certain types of javadoc and adds them to an XML file.
     *
     * @param rootDoc The documentation root.
     * @return Whether the JavaDoc run succeeded.
     * @throws java.io.IOException if output can't be written.
     */
    protected boolean startProcessDocs(final RootDoc rootDoc) throws IOException {
        for (String[] options : rootDoc.options()) {
            if (options[0].equals(SETTINGS_DIR_OPTION))
                settingsDir = new File(options[1]);
            if (options[0].equals(DESTINATION_DIR_OPTION))
                destinationDir = new File(options[1]);
            if (options[0].equals(BUILD_TIMESTAMP_OPTION))
                buildTimestamp = options[1];
            if (options[0].equals(ABSOLUTE_VERSION_OPTION))
                absoluteVersion = options[1];
            if (options[0].equals(INCLUDE_HIDDEN_OPTION))
                showHiddenFeatures = true;
            if (options[0].equals(OUTPUT_FILE_EXTENSION_OPTION)) {
                outputFileExtension = options[1];
            }
        }

        if (!settingsDir.exists())
            throw new RuntimeException(SETTINGS_DIR_OPTION + " :" + settingsDir.getPath() + " does not exist");
        else if (!settingsDir.isDirectory())
            throw new RuntimeException(SETTINGS_DIR_OPTION + " :" + settingsDir.getPath() + " is not a directory");

        processDocs(rootDoc);
        return true;
    }

    /**
     * Validate the given options against options supported by this doclet.
     *
     * @param option Option to validate.
     * @return Number of potential parameters; 0 if not supported.
     */
    public static int optionLength(final String option) {
        // Any arguments used for the doclet need to be recognized here. Many javadoc plugins (ie. gradle)
        // automatically add some such as "-d", "-doctitle", "-windowtitle", which we ignore.
        if (option.equals("-d") ||
                option.equals("-doctitle") ||
                option.equals("-windowtitle") ||
                option.equals(SETTINGS_DIR_OPTION) ||
            option.equals(DESTINATION_DIR_OPTION) ||
            option.equals(BUILD_TIMESTAMP_OPTION) ||
            option.equals(ABSOLUTE_VERSION_OPTION) ||
            option.equals(OUTPUT_FILE_EXTENSION_OPTION)) {
            return 2;
        } else if (option.equals("-quiet")) {
            return 1;
        } else {
            logger.error("The Javadoc command line option is not recognized by the Barclay doclet: " + option);
            return 0;
        }
    }

    /**
     * @return Boolean indicating whether to include @Hidden annotations in our documented output
     */
    public boolean showHiddenFeatures() {
        return showHiddenFeatures;
    }

    /**
     * @return the output extension to use, i.e., ".html" or ".php"
     */
    protected String getOutputFileExtension() { return outputFileExtension; }

    protected String getIndexTemplateName() { return "generic.index.template.html"; }

    /**
     * @return a DocumentedFeatureHandler-derived object. Subclasses may override this to provide a
     * custom handler.
     */
    protected DocumentedFeatureHandler createDocumentedFeatureHandler(
            final ClassDoc classDoc,
            final DocumentedFeatureObject documentedFeature) {
        return new DefaultDocumentedFeatureHandler();
    }

    /**
     * @param rootDoc
     */
    private void processDocs(final RootDoc rootDoc) {
        // setup the global access to the root
        this.rootDoc = rootDoc;

        try {
            /* ------------------------------------------------------------------- */
            /* You should do this ONLY ONCE in the whole application life-cycle:   */
            final Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(settingsDir); // where the template files come from
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            workUnits = computeWorkUnits();

            final Set<String> seenFeatureGroups = new HashSet<>();
            final List<Map<String, String>> featureMaps = new ArrayList<>();
            final List<Map<String, String>> groupMaps = new ArrayList<>();

            workUnits.stream().forEach(
                    workUnit -> {
                        featureMaps.add(workUnit.indexDataMap());
                        if (!seenFeatureGroups.contains(workUnit.documentedFeatureObject.groupName())) {
                            groupMaps.add(getGroupMap(workUnit.documentedFeatureObject));
                            seenFeatureGroups.add(workUnit.documentedFeatureObject.groupName());
                        }
                    }
            );

            emitOutputFromTemplates(cfg, groupMaps, featureMaps);

        } catch (FileNotFoundException e) {
            throw new RuntimeException("FileNotFoundException processing javadoc template", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException processing javadoc template", e);
        }
    }

    /**
     * @return the set of all DocWorkUnits for which we are generating docs.
     */
    private Set<DocWorkUnit> computeWorkUnits() {
        final TreeSet<DocWorkUnit> workUnits = new TreeSet<>();

        for (final ClassDoc doc : rootDoc.classes()) {
            final Class<?> clazz = getClassForClassDoc(doc);

            final DocumentedFeatureObject feature = getFeatureForClass(clazz);
            final DocumentedFeatureHandler handler = createFeatureHandler(doc, feature);
            if (handler != null && handler.includeInDocs(clazz, doc)) {
                final DocWorkUnit workUnit = new DocWorkUnit(
                        doc.name(),
                        feature.groupName(),
                        feature,
                        handler,
                        doc,
                        clazz,
                        buildTimestamp,
                        absoluteVersion);
                workUnits.add(workUnit);
            }
        }

        return workUnits;
    }

    /**
     * Actually write out the output files (html and gson file for each feature) and the index file.
     */
    private void emitOutputFromTemplates (
            final Configuration cfg,
            final List<Map<String, String>> groupMaps,
            final List<Map<String, String>> featureMaps) throws IOException
    {
        // Generate one template file for each work unit
        workUnits.stream().forEach(
                workUnit -> {
                    processWorkUnitTemplate(cfg, workUnit, groupMaps, featureMaps);
                });

        // Generate the index
        processIndexTemplate(cfg, new ArrayList<>(workUnits), groupMaps);
    }

    /**
     * Create a handler capable of documenting the class doc according to feature.  Returns
     * null if no appropriate handler is found or doc shouldn't be documented at all.
     */
    private DocumentedFeatureHandler createFeatureHandler(final ClassDoc classDoc, final DocumentedFeatureObject documentedFeature) {
        if (documentedFeature != null) {
            if (documentedFeature.enable()) {
                // delegate to the subclass to create a DocumentedFeatureHandler derived object
                final DocumentedFeatureHandler handler = createDocumentedFeatureHandler( classDoc, documentedFeature);
                handler.setDoclet(this);
                return handler;
            } else {
                logger.info("Skipping disabled documentation for feature: " + classDoc);
            }
        }

        return null;
    }

    /**
     * Returns the instantiated DocumentedFeatureObject that describes the doc for this class.
     *
     * This method prefers the summary and group names from the DocumentedFeature annotation if they are
     * present, but will fall back to the ones from the CommandLineProgramProperties annotation if that is present.
     * This reduces the need to specify redundant value for CommandLineProgramProperties classes that require
     * both annotations.
     *
     * @param clazz
     * @return DocumentedFeatureObject, or null if this classDoc shouldn't be included/documented
     */
    private DocumentedFeatureObject getFeatureForClass(final Class<?> clazz) {

        if (clazz != null && clazz.isAnnotationPresent(DocumentedFeature.class)) {
            DocumentedFeature f = clazz.getAnnotation(DocumentedFeature.class);
            String summary = f.summary();
            String groupName = f.groupName();
            String groupSummary = f.groupSummary();
            CommandLineProgramProperties clProps =
                    clazz.isAnnotationPresent(CommandLineProgramProperties.class) ?
                        clazz.getAnnotation(CommandLineProgramProperties.class) :
                        null;
            if ((summary == null || summary.length() == 0) && clProps != null) {
                summary = clProps.oneLineSummary();
                if (summary == null || summary.length() == 0) {
                    summary = "No summary available";
                }
            }
            if ((groupName == null || groupName.length() == 0) && clProps != null) {
                groupName = clProps.programGroup().getName();
                if (groupName == null || groupName.length() == 0) {
                    groupName = "No group name available";
                }
            }
            if ((groupSummary == null || groupSummary.length() == 0) && clProps != null) {
                try {
                    groupSummary = clProps.programGroup().newInstance().getDescription();
                    if (groupSummary == null || groupSummary.length() == 0) {
                        groupSummary = "No group summary available";
                    }
                }
                catch (IllegalAccessException | InstantiationException e){
                    logger.warn(
                            String.format("Cant instantiate program group class to retrieve summary for group %s for class %s",
                                    clProps.programGroup().getName(),
                                    clazz.getName()));
                    groupSummary = "No group summary available";
                }
            }
            return new DocumentedFeatureObject(clazz, f.enable(), summary, groupName, groupSummary, f.extraDocs());
        } else {
            return null;
        }
    }

    /**
     * Return the Java class described by the ClassDoc doc
     *
     * @param doc
     * @return
     */
    private Class<? extends Object> getClassForClassDoc(final ClassDoc doc) {
        try {
            return DocletUtils.getClassForDoc(doc);
        } catch (ClassNotFoundException e) {
            // we got a classdoc for a class we can't find.  Maybe in a library or something
            return null;
        } catch (NoClassDefFoundError e) {
            return null;
        } catch (UnsatisfiedLinkError e) {
            return null; // naughty BWA bindings
        }
    }

    /**
     * Create the php index listing all of the Docs features
     *
     * @param cfg
     * @param workUnitList
     * @param groupMaps
     * @throws IOException
     */
    private void processIndexTemplate(
            final Configuration cfg,
            final List<DocWorkUnit> workUnitList,
            final List<Map<String, String>> groupMaps
   ) throws IOException {
        // Get or create a template and merge in the data
        final Template template = cfg.getTemplate(getIndexTemplateName());
        final File indexFile = new File(destinationDir + "/index." + outputFileExtension);
        try (final FileOutputStream fileOutStream = new FileOutputStream(indexFile);
             final OutputStreamWriter outWriter = new OutputStreamWriter(fileOutStream)) {
            template.process(groupIndexMap(workUnitList, groupMaps), outWriter);
        } catch (TemplateException e) {
            throw new DocException("Freemarker Template Exception during documentation index creation", e);
        }
    }

    /**
     * Helpful function to create the php index.  Given all of the already run DocWorkUnits,
     * create the high-level grouping data listing individual features by group.
     *
     * @param workUnitList
     * @return
     */
    protected Map<String, Object> groupIndexMap(
            final List<DocWorkUnit> workUnitList,
            final List<Map<String, String>> groupMaps
    ) {
        //
        // root -> data -> { summary -> y, filename -> z }, etc
        //      -> groups -> group1, group2, etc.
        Map<String, Object> root = new HashMap<>();

        Collections.sort(workUnitList);

        List<Map<String, String>> data = new ArrayList<>();
        workUnitList.stream().forEach(workUnit -> data.add(workUnit.indexDataMap()));

        root.put("data", data);
        root.put("groups", groupMaps);
        root.put("timestamp", buildTimestamp);
        root.put("version", absoluteVersion);

        return root;
    }

    /**
     * Helper routine that returns the map of name and summary given the documentedFeatureObject
     *
     * @param documentedFeatureObject
     * @return
     */
    protected Map<String, String> getGroupMap(final DocumentedFeatureObject documentedFeatureObject) {
        Map<String, String> root = new HashMap<>();
        root.put("id", getGroupIdFromName(documentedFeatureObject.groupName()));
        root.put("name", documentedFeatureObject.groupName());
        root.put("summary", documentedFeatureObject.groupSummary());
        return root;
    };

    private String getGroupIdFromName(final String groupName) { return groupName.replaceAll("\\W", ""); }

    /**
     * Helper function that finding the DocWorkUnit associated with class from among all of the work units
     *
     * @param c the class we are looking for
     * @return the DocWorkUnit whose .clazz.equals(c), or null if none could be found
     */
    public final DocWorkUnit findWorkUnitForClass(final Class<?> c) {
        for (final DocWorkUnit unit : this.workUnits)
            if (unit.clazz.equals(c))
                return unit;
        return null;
    }

    /**
     * Return the ClassDoc associated with clazz
     *
     * @param clazz
     * @return
     */
    public ClassDoc getClassDocForClass(final Class<?> clazz) {
        return rootDoc.classNamed(clazz.getName());
    }

    /**
     * High-level function that processes a single DocWorkUnit unit using its handler
     *
     * @param cfg
     * @param workUnit
     * @param featureMaps
     * @throws IOException
     */
    protected void processWorkUnitTemplate(
            final Configuration cfg,
            final DocWorkUnit workUnit,
            final List<Map<String, String>> groupMaps,
            final List<Map<String, String>> featureMaps)
    {
        workUnit.handler.processWorkUnit(workUnit);
        workUnit.rootMap.put("groups", groupMaps);
        workUnit.rootMap.put("data", featureMaps);

        try {
            // Merge data-model with template
            Template template = cfg.getTemplate(workUnit.handler.getTemplateName(workUnit.classDoc));
            File outputPath = new File(destinationDir + "/" + workUnit.getTargetFileName());
            try (final Writer out = new OutputStreamWriter(new FileOutputStream(outputPath))) {
                template.process(workUnit.rootMap, out);
            }
        } catch (IOException e) {
            throw new DocException("IOException during documentation creation", e);
        } catch (TemplateException e) {
            throw new DocException("TemplateException during documentation creation", e);
        }

        // Create GSON-friendly container object
        GSONWorkUnit gsonworkunit = createGSONWorkUnit(workUnit, groupMaps, featureMaps);

        gsonworkunit.populate(
                workUnit.rootMap.get("summary").toString(),
                workUnit.rootMap.get("gson-arguments"),
                workUnit.rootMap.get("description").toString(),
                workUnit.rootMap.get("name").toString(),
                workUnit.rootMap.get("group").toString()
        );

        // Convert object to JSON and write JSON entry to file
        File outputPathForJSON = new File(destinationDir + "/" + workUnit.getTargetFileName() + ".json");

        try (final BufferedWriter jsonWriter = new BufferedWriter(new FileWriter(outputPathForJSON))) {
            Gson gson = new GsonBuilder()
                .serializeSpecialFloatingPointValues()
                .setPrettyPrinting()
                .create();
            String json = gson.toJson(gsonworkunit);
            jsonWriter.write(json);
        } catch (IOException e) {
            throw new DocException("Failed to create JSON entry", e);
        }
    }

    /**
     * Doclet implementations (subclasses) should return a GSONWorkUnit-derived object if the GSON objects
     * for the DocumentedFeature needs to contain custom values.
     * @return a GSONWorkUnit-derived object
     */
    protected GSONWorkUnit createGSONWorkUnit(
            final DocWorkUnit workUnit,
            final List<Map<String, String>> groupMaps,
            final List<Map<String, String>> featureMaps)
    {
        return new GSONWorkUnit();
    }


}
