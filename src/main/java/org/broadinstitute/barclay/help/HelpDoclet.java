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
import org.broadinstitute.barclay.argparser.Hidden;
import org.broadinstitute.barclay.utils.JVMUtils;

import java.io.*;
import java.util.*;

/**
 * Javadoc Doclet that combines javadoc, Barclay annotations, and FreeMarker
 * templates to produce formatted docs for classes.
 * <p/>
 * The doclet has the following workflow:
 * <p/>
 * 1 -- walk the javadoc hierarchy, looking for classes that have the DocumentedFeature annotation
 * 2 -- for each annotated class, construct a WorkUnit/Handler to determine if that feature
 * should be included in the ouput
 * 3 -- for each included feature, construct a DocWorkUnit consisting of all documentation
 * evidence (DocumentedFeature/CommandLineProgramProperties annotations, javadoc ClassDoc,
 * java Class, and DocWorkUnitHandler
 * 4 -- After all DocWorkUnits are accumulated, delegate the processing of each work unit to
 * the work unit's handler, allowing it to populate the work unit's Freemarker property map, after
 * which each work unit is written to it's template-based output file and GSON file
 * 5 -- write out an index of all units, organized by group
 * <p/>
 * Note: although this class can be used to generate documentation directly, most consumers will
 * want to subclass it to override the following methods in order to create application-specific
 * templates and template property maps:
 *
 * {@link #getIndexTemplateName}
 * {@link #createWorkUnit}
 * {@link #createGSONWorkUnit}
 * {@link #start(RootDoc)} A static method that instantiates the subclass and delegates to the
 * instance method {@link #startProcessDocs(RootDoc)}.
 */
public class HelpDoclet {
    final protected static Logger logger = LogManager.getLogger(HelpDoclet.class);

    // Builtin javadoc command line arguments
    final private static String DESTINATION_DIR_OPTION = "-d";
    final private static String WINDOW_TITLE_OPTION = "-windowtitle";
    final private static String DOC_TITLE_OPTION = "-doctitle";
    final private static String QUIET_OPTION = "-quiet";

    // Barclay HelpDoclet custom command line options
    final private static String SETTINGS_DIR_OPTION = "-settings-dir";
    final private static String BUILD_TIMESTAMP_OPTION = "-build-timestamp";
    final private static String ABSOLUTE_VERSION_OPTION = "-absolute-version";
    final private static String INCLUDE_HIDDEN_OPTION = "-hidden-version";
    final private static String OUTPUT_FILE_EXTENSION_OPTION = "-output-file-extension";
    final private static String INDEX_FILE_EXTENSION_OPTION = "-index-file-extension";

    // Where we find the help FreeMarker templates
    final private static File DEFAULT_SETTINGS_DIR = new File("settings/helpTemplates");
    // Where we write the output
    final private static File DEFAULT_DESTINATION_DIR = new File("barclaydocs");
    // Default output file extension
    final private static String DEFAULT_OUTPUT_FILE_EXTENSION = "html";

    // ----------------------------------------------------------------------
    //
    // Variables that are set on the command line when running javadoc
    //
    // ----------------------------------------------------------------------
    protected static File settingsDir = DEFAULT_SETTINGS_DIR;
    protected static File destinationDir = DEFAULT_DESTINATION_DIR;
    protected static String outputFileExtension = DEFAULT_OUTPUT_FILE_EXTENSION;
    protected static String indexFileExtension = DEFAULT_OUTPUT_FILE_EXTENSION;
    protected static String buildTimestamp = "[no timestamp available]";
    protected static String absoluteVersion = "[no version available]";
    protected static boolean showHiddenFeatures = false;

    private RootDoc rootDoc;                // The javadoc root doc
    private Set<DocWorkUnit> workUnits;     // Set of all things we are going to document

    /**
     * The entry point for javadoc generation. Default implementation creates an instance of
     * {@link HelpDoclet} and calls {@link #startProcessDocs(RootDoc)} on that instance.
     *
     * <p>Note: Custom Barclay doclets should subclass this class, and implement a similar static
     * method that creates an instance of the doclet subclass and delegates to that instance's
     * {@link #startProcessDocs(RootDoc)}.
     */
     public static boolean start(final RootDoc rootDoc) throws IOException {
         return new HelpDoclet().startProcessDocs(rootDoc);
     }

    /**
     * Extracts the contents of certain types of javadoc and adds them to an output file.
     *
     * @param rootDoc The documentation root.
     * @return Whether the JavaDoc run succeeded.
     * @throws java.io.IOException if output can't be written.
     */
    protected boolean startProcessDocs(final RootDoc rootDoc) throws IOException {
        for (String[] options : rootDoc.options()) {
            parseOption(options);
        }

        if (!settingsDir.exists())
            throw new RuntimeException(SETTINGS_DIR_OPTION + " :" + settingsDir.getPath() + " does not exist");
        else if (!settingsDir.isDirectory())
            throw new RuntimeException(SETTINGS_DIR_OPTION + " :" + settingsDir.getPath() + " is not a directory");

        processDocs(rootDoc);
        return true;
    }

    /**
     * Parse the options for the javadoc command line. The first item is the option name, and the
     * following the number of items returned by {@link #optionLength(String)} for that option.
     *
     * Implementations should override this method and call it to parse the required options.
     * In addition, to support custom options {@link #optionLength(String)} may be implemented.
     *
     * @param options
     */
    protected void parseOption(final String[] options) {
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
        if (options[0].equals(INDEX_FILE_EXTENSION_OPTION)) {
            indexFileExtension = options[1];
        }
    }

    /**
     * Validate the given options against options supported by this doclet.
     *
     * <p>Note: for custom options, doclets should implement a similar static method and call the
     * one from {@link HelpDoclet} to maintain required options. In addition, custom options should
     * be handled by {@link #parseOption(String[])}.
     *
     * @param option Option to validate.
     * @return Number of potential parameters; 0 if not supported.
     */
    public static int optionLength(final String option) {
        // Any arguments used for the doclet need to be recognized here. Many javadoc plugins (ie. gradle)
        // automatically add some such as "-doctitle", "-windowtitle", which we ignore.
        if (option.equals(DOC_TITLE_OPTION) ||
            option.equals(WINDOW_TITLE_OPTION) ||
            option.equals(SETTINGS_DIR_OPTION) ||
            option.equals(DESTINATION_DIR_OPTION) ||
            option.equals(BUILD_TIMESTAMP_OPTION) ||
            option.equals(ABSOLUTE_VERSION_OPTION) ||
            option.equals(OUTPUT_FILE_EXTENSION_OPTION) ||
            option.equals(INDEX_FILE_EXTENSION_OPTION)) {
            return 2;
        } else if (option.equals(QUIET_OPTION)) {
            return 1;
        } else {
            logger.error("The Javadoc command line option is not recognized by the Barclay doclet: " + option);
            return 0;
        }
    }

    /**
     * Process the classes that have been included by the javadoc process in the rootDoc object.
     *
     * @param rootDoc root structure containing the the set of objects accumulated by the javadoc process
     */
    private void processDocs(final RootDoc rootDoc) {
        this.rootDoc = rootDoc;

        // Get a list of all the features and groups that we'll actually retain
        workUnits = computeWorkUnits();

        final Set<String> uniqueGroups = new HashSet<>();
        final List<Map<String, String>> featureMaps = new ArrayList<>();
        final List<Map<String, String>> groupMaps = new ArrayList<>();

        // First pass over work units: create the top level map of features and groups
        workUnits.stream().forEach(
                workUnit -> {
                    featureMaps.add(indexDataMap(workUnit));
                    if (!uniqueGroups.contains(workUnit.getGroupName())) {
                        uniqueGroups.add(workUnit.getGroupName());
                        groupMaps.add(getGroupMap(workUnit));
                    }
                }
        );

        // Second pass:  populate the property map for each work unit
        workUnits.stream().forEach(workUnit -> { workUnit.processDoc(featureMaps, groupMaps); });

        // Third pass: Generate the individual outputs for each work unit, and the top-level index file
        emitOutputFromTemplates(groupMaps, featureMaps);
    }


    /**
     * For each class in the rootDoc class list, delegate to the appropriate DocWorkUnitHandler to
     * determine if it should be included in this run, and for each included feature, construct a DocWorkUnit.
     *
     * @return the set of all DocWorkUnits for which we are actually generating docs
     */
    private Set<DocWorkUnit> computeWorkUnits() {
        final TreeSet<DocWorkUnit> workUnits = new TreeSet<>();

        for (final ClassDoc classDoc : rootDoc.classes()) {
            final Class<?> clazz = getClassForClassDoc(classDoc);
            final DocumentedFeature documentedFeature = getDocumentedFeatureForClass(clazz);

            if (documentedFeature != null) {
                if (documentedFeature.enable()) {
                    DocWorkUnit workUnit = createWorkUnit(
                            documentedFeature,
                            classDoc,
                            clazz);
                    if (workUnit != null) {
                        workUnits.add(workUnit);
                    }
                } else {
                    logger.info("Skipping disabled documentation for feature: " + classDoc);
                }
            }
        }

        return workUnits;
    }

    public RootDoc getRootDoc() { return rootDoc; }

    public String getBuildTimeStamp() { return buildTimestamp; }

    public String getBuildVersion() { return absoluteVersion; }

    /**
     * @return Boolean indicating whether to include @Hidden annotations in our documented output
     */
    public boolean showHiddenFeatures() { return showHiddenFeatures; }

    /**
     * @return the output extension to use, i.e., ".html" or ".php"
     */
    public String getOutputFileExtension() { return outputFileExtension; }

    /**
     * @return the output extension to use for the index, i.e., ".html" or ".php"
     */
    public String getIndexFileExtension() { return indexFileExtension; }

    /**
     * @return the name of the index template to be used for this doclet
     */
    public String getIndexTemplateName() { return "generic.index.template.html"; }

    /**
     * @return the file where the files will be output
     */
    public File getDestinationDir() { return  destinationDir; }

    /**
     * Determine if a particular class should be included in the output. This is called by the doclet
     * to determine if a DocWorkUnit should be created for this feature.
     *
     * @param documentedFeature feature that is being considered for inclusion in the docs
     * @param classDoc for the class that is being considered for inclusion in the docs
     * @param clazz class that is being considered for inclusion in the docs
     * @return true if the doc should be included, otherwise false
     */
    public boolean includeInDocs(final DocumentedFeature documentedFeature, final ClassDoc classDoc, final Class<?> clazz) {
        boolean hidden = !showHiddenFeatures() && clazz.isAnnotationPresent(Hidden.class);
        return !hidden && JVMUtils.isConcrete(clazz);
    }

    /**
     * Actually write out the output files (html and gson file for each feature) and the index file.
     */
    private void emitOutputFromTemplates (
            final List<Map<String, String>> groupMaps,
            final List<Map<String, String>> featureMaps)
    {
        try {
            /* ------------------------------------------------------------------- */
            /* You should do this ONLY ONCE in the whole application life-cycle:   */
            final Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(settingsDir); // where the template files come from
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            // Generate one template file for each work unit
            workUnits.stream().forEach(workUnit -> processWorkUnitTemplate(cfg, workUnit, groupMaps, featureMaps));
            processIndexTemplate(cfg, new ArrayList<>(workUnits), groupMaps);

        } catch (FileNotFoundException e) {
            throw new RuntimeException("FileNotFoundException processing javadoc template", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException processing javadoc template", e);
        }
    }

    /**
     * Create a work unit and handler capable of documenting the feature specified by the input arguments.
     * Returns null if no appropriate handler is found or doc shouldn't be documented at all.
     */
    protected DocWorkUnit createWorkUnit(
            final DocumentedFeature documentedFeature,
            final ClassDoc classDoc,
            final Class<?> clazz)
    {
        return new DocWorkUnit(
                new DefaultDocWorkUnitHandler(this),
                documentedFeature,
                classDoc,
                clazz);
    }

    /**
     * Returns the instantiated DocumentedFeature that describes the doc for this class.
     *
     * @param clazz
     * @return DocumentedFeature, or null if this classDoc shouldn't be included/documented
     */
    private DocumentedFeature getDocumentedFeatureForClass(final Class<?> clazz) {
        if (clazz != null && clazz.isAnnotationPresent(DocumentedFeature.class)) {
            return clazz.getAnnotation(DocumentedFeature.class);
        }
        else {
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
        final File indexFile = new File(getDestinationDir() + "/index." + getIndexFileExtension());
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
     * @return The map used to populate the index template used by this doclet.
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
        workUnitList.stream().forEach(workUnit -> data.add(indexDataMap(workUnit)));

        root.put("data", data);
        root.put("groups", groupMaps);
        root.put("timestamp", getBuildTimeStamp());
        root.put("version", getBuildVersion());

        return root;
    }

    /**
     * Helper routine that returns the map of group name and summary given the workUnit. Subclasses that
     * override this should call this method before doing further processing.
     *
     * @param workUnit
     * @return The property map for the work unit's entry in the index map for this doclet.
     */
    protected Map<String, String> getGroupMap(final DocWorkUnit workUnit) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("id", getGroupIdFromName(workUnit.getGroupName()));
        propertyMap.put("name", workUnit.getGroupName());
        propertyMap.put("summary", workUnit.getGroupSummary());
        return propertyMap;
    };

    private String getGroupIdFromName(final String groupName) { return groupName.replaceAll("\\W", ""); }

    /**
     * Return a String -> String map suitable for FreeMarker to create an index to this WorkUnit
     *
     * @return
     */
    public Map<String, String> indexDataMap(final DocWorkUnit workUnit) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("name", workUnit.getName());
        propertyMap.put("summary", workUnit.getSummary());
        propertyMap.put("filename", workUnit.getTargetFileName());
        propertyMap.put("group", workUnit.getGroupName());
        propertyMap.put("beta", Boolean.toString(workUnit.getBetaFeature()));
        return propertyMap;
    }

    /**
     * Helper function that finding the DocWorkUnit associated with class from among all of the work units
     *
     * @param c the class we are looking for
     * @return the DocWorkUnit whose .clazz.equals(c), or null if none could be found
     */
    public final DocWorkUnit findWorkUnitForClass(final Class<?> c) {
        for (final DocWorkUnit workUnit : this.workUnits)
            if (workUnit.getClazz().equals(c))
                return workUnit;
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
            final List<Map<String, String>> indexByGroupMaps,
            final List<Map<String, String>> featureMaps)
    {
        try {
            // Merge data-model with template
            Template template = cfg.getTemplate(workUnit.getTemplateName());
            File outputPath = new File(getDestinationDir() + "/" + workUnit.getTargetFileName());
            try (final Writer out = new OutputStreamWriter(new FileOutputStream(outputPath))) {
                template.process(workUnit.getRootMap(), out);
            }
        } catch (IOException e) {
            throw new DocException("IOException during documentation creation", e);
        } catch (TemplateException e) {
            throw new DocException("TemplateException during documentation creation", e);
        }

        // Create GSON-friendly container object
        GSONWorkUnit gsonworkunit = createGSONWorkUnit(workUnit, indexByGroupMaps, featureMaps);

        gsonworkunit.populate(
                workUnit.getProperty("summary").toString(),
                workUnit.getProperty("gson-arguments"),
                workUnit.getProperty("description").toString(),
                workUnit.getProperty("name").toString(),
                workUnit.getProperty("group").toString()
        );

        // Convert object to JSON and write JSON entry to file
        File outputPathForJSON = new File(getDestinationDir() + "/" + workUnit.getTargetFileName() + ".json");

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
            final List<Map<String, String>> indexByGroupMaps,
            final List<Map<String, String>> featureMaps)
    {
        return new GSONWorkUnit();
    }

}
