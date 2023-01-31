package org.broadinstitute.barclay.help;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import freemarker.cache.TemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.ClassTemplateLoader;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.Reporter;

import org.broadinstitute.barclay.argparser.Hidden;
import org.broadinstitute.barclay.help.scanners.JavaLanguageModelScanners;
import org.broadinstitute.barclay.utils.JVMUtils;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
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
 * should be included in the output
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
 * {@link #run(DocletEnvironment)}
 * {@link #getIndexTemplateName()}
 * {@link #createWorkUnit(Element, Class, DocumentedFeature)}
 * {@link #createGSONWorkUnit(DocWorkUnit, List, List)}
 */
public class HelpDoclet implements Doclet {
    // Where we find the help FreeMarker templates
    final static File DEFAULT_SETTINGS_DIR = new File("settings/helpTemplates");
    final static String DEFAULT_SETTINGS_CLASSPATH = "/org/broadinstitute/barclay/helpTemplates";
    // Where we write the output
    final static File DEFAULT_DESTINATION_DIR = new File("barclaydocs");
    // Default output file extension
    final static String DEFAULT_OUTPUT_FILE_EXTENSION = "html";

    // ----------------------------------------------------------------------
    //
    // Variables that are set on the command line when running javadoc
    //
    // ----------------------------------------------------------------------
    protected File settingsDir = DEFAULT_SETTINGS_DIR;
    protected boolean isSettingsDirSet = false;
    protected File destinationDir = DEFAULT_DESTINATION_DIR;
    protected String outputFileExtension = DEFAULT_OUTPUT_FILE_EXTENSION;
    protected String indexFileExtension = DEFAULT_OUTPUT_FILE_EXTENSION;
    protected String buildTimestamp = "[no timestamp available]";
    protected String absoluteVersion = "[no version available]";
    protected boolean showHiddenFeatures = false;
    protected boolean useDefaultTemplates = false;

    // Variables to store data for Freemarker:
    private DocletEnvironment docletEnv;      // The javadoc doclet env
    protected Set<DocWorkUnit> workUnits = new LinkedHashSet<>();     // Set of all things we are going to document
    private  Reporter reporter;

    @Override
    public void init(final Locale locale, final Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public boolean run(final DocletEnvironment docEnv) {
        // Make sure the user specified a settings directory OR that we should use the defaults.
        // Both are not allowed.
        // Neither are not allowed.
        if ( (useDefaultTemplates && isSettingsDirSet) ||
                (!useDefaultTemplates && !isSettingsDirSet)) {
            throw new RuntimeException("ERROR: must specify only ONE of: " + BarclayDocletOptions.USE_DEFAULT_TEMPLATES_OPTION + " , " + BarclayDocletOptions.SETTINGS_DIR_OPTION);
        }

        this.docletEnv = docEnv;

        // Make sure we can use the directory for settings we have set:
        if (!useDefaultTemplates) {
            validateSettingsDir();
        }

        // Make sure we're in a good state to run:
        validateDocletStartingState();

        processDocs(docEnv);
        return true;
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return new LinkedHashSet<>() {{

            // standard javadoc options

            add(new BarclayDocletOption.SimpleStandardOption(BarclayDocletOptions.DESTINATION_DIR_OPTION) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    destinationDir = new File(arguments.get(0));
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(BarclayDocletOptions.WINDOW_TITLE_OPTION) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    return true;
                }
            });

            add(new BarclayDocletOption.SimpleStandardOption(BarclayDocletOptions.DOC_TITLE_OPTION) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    return true;
                }
            });

            add(new BarclayDocletOption(
                    Arrays.asList(BarclayDocletOptions.QUIET_OPTION),
                    "quiet",
                    0,
                    Option.Kind.STANDARD,
                    null) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    // the gradle javadoc task includes this since its a standard doclet arg
                    // so we need to tolerate it, but ignore it
                    return true;
                }
            });

            add(new BarclayDocletOption(
                    Arrays.asList(BarclayDocletOptions.SETTINGS_DIR_OPTION),
                    "settings dir",
                    1,
                    Option.Kind.STANDARD,
                    "<string>") {
                @Override
                public boolean process(String option, List<String> arguments) {
                    settingsDir = new File(arguments.get(0));
                    isSettingsDirSet = true;
                    return true;
                }
            });

            // custom Barclay options

            add(new BarclayDocletOption(
                    Arrays.asList(BarclayDocletOptions.BUILD_TIMESTAMP_OPTION),
                    "build timestamp",
                    1,
                    Option.Kind.OTHER,
                    "<string>") {
                @Override
                public boolean process(String option, List<String> arguments) {
                    buildTimestamp = arguments.get(0);
                    return true;
                }
            });
            add(new BarclayDocletOption(
                    Arrays.asList(BarclayDocletOptions.ABSOLUTE_VERSION_OPTION),
                    "absolute version",
                    1,
                    Option.Kind.OTHER,
                    "<string>") {
                @Override
                public boolean process(String option, List<String> arguments) {
                    absoluteVersion = arguments.get(0);
                    return true;
                }
            });
            add(new BarclayDocletOption(
                    Arrays.asList(BarclayDocletOptions.OUTPUT_FILE_EXTENSION_OPTION),
                    "output file extension",
                    1,
                    Option.Kind.OTHER,
                    "<string>") {
                @Override
                public boolean process(String option, List<String> arguments) {
                    outputFileExtension = arguments.get(0);
                    return true;
                }
            });
            add(new BarclayDocletOption(
                    Arrays.asList(BarclayDocletOptions.INDEX_FILE_EXTENSION_OPTION),
                    "index file extension",
                    1,
                    Option.Kind.OTHER,
                    "<string>") {
                @Override
                public boolean process(String option, List<String> arguments) {
                    indexFileExtension = arguments.get(0);
                    return true;
                }
            });
            add(new BarclayDocletOption(
                    Arrays.asList(BarclayDocletOptions.INCLUDE_HIDDEN_OPTION),
                    "hidden features",
                    0,
                    Option.Kind.OTHER,
                    null) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    showHiddenFeatures = true;
                    return true;
                }
            });
            add(new BarclayDocletOption(
                    Arrays.asList(BarclayDocletOptions.USE_DEFAULT_TEMPLATES_OPTION),
                    "use default templates",
                    0,
                    Option.Kind.OTHER,
                    null) {
                @Override
                public boolean process(String option, List<String> arguments) {
                    useDefaultTemplates = true;
                    return true;
                }
            });
        }};
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_17;
    }

    /**
     * Ensure that {@link #settingsDir} exists and is a directory.
     * Throws a {@link RuntimeException} if {@link #settingsDir} is invalid.
     */
    private void validateSettingsDir() {
         if (!settingsDir.exists()) {
             throw new RuntimeException(BarclayDocletOptions.SETTINGS_DIR_OPTION + " : " + settingsDir.getPath() + " does not exist!");
         }
         else if (!settingsDir.isDirectory()) {
             throw new RuntimeException(BarclayDocletOptions.SETTINGS_DIR_OPTION + " : " + settingsDir.getPath() + " is not a directory!");
         }
     }

    /**
     * This method exists to allow child classes to do input argument / state checking.
     * Designed to be overridden in child classes.
     *
     * Child classes should do internal validation and throw if there are issues.
     */
    protected void validateDocletStartingState() {

    }

    /**
     * Process the classes that have been included by the javadoc process.
     *
     * @param docletEnv the Doclet's DocletEnvironment
     */
    private void processDocs(final DocletEnvironment docletEnv) {
        this.docletEnv = docletEnv;

        // scan all included elements for DocumentedFeatures
        workUnits = JavaLanguageModelScanners.getWorkUnits(this, docletEnv, reporter, docletEnv.getIncludedElements());

        final Set<String> uniqueGroups = new LinkedHashSet<>();
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

        // Second pass: populate the property map for each work unit
        workUnits.stream().forEach(workUnit -> { workUnit.processDoc(featureMaps, groupMaps); });

        // Third pass: Generate the individual outputs for each work unit, and the top-level index file
        emitOutputFromTemplates(groupMaps, featureMaps);
    }

    public DocletEnvironment getDocletEnv() { return docletEnv; }

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
    public String getIndexTemplateName() { return "generic.index.html.ftl"; }

    /**
     * @return The base filename for the index file associated with this doclet.
     */
    public String getIndexBaseFileName() { return "index"; }

    /**
     * @return the file where the files will be output
     */
    public File getDestinationDir() { return  destinationDir; }

    /**
     * Determine if a particular class should be included in the output. This is called by the doclet
     * to determine if a DocWorkUnit should be created for this feature.
     *
     * @param documentedFeature feature that is being considered for inclusion in the docs
     * @param clazz class that is being considered for inclusion in the docs
     * @return true if the doc should be included, otherwise false
     */
    public boolean includeInDocs(final DocumentedFeature documentedFeature, final Class<?> clazz) {
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
            final Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
            cfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_23));

            // We need to set up a scheme to load our settings from wherever they may live.
            // This means we need to set up a multi-loader including the classpath and any specified options:

            TemplateLoader templateLoader;

            // Only add the settings directory if we're supposed to:
            if ( useDefaultTemplates ) {
                templateLoader = new ClassTemplateLoader(getClass(), DEFAULT_SETTINGS_CLASSPATH);
            }
            else {
                templateLoader = new FileTemplateLoader(new File(settingsDir.getPath()));
            }

            // Tell freemarker to load our templates as we specified above:
            cfg.setTemplateLoader(templateLoader);

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
    public DocWorkUnit createWorkUnit(
            final Element classElement,
            final Class<?> clazz,
            final DocumentedFeature documentedFeature)
    {
        return new DocWorkUnit(
                new DefaultDocWorkUnitHandler(this),
                classElement,
                clazz,
                documentedFeature
        );
    }

    /**
     * Create the php index listing all of the Docs features
     *
     * @param cfg
     * @param workUnitList
     * @param groupMaps
     * @throws IOException
     */
    protected void processIndexTemplate(
            final Configuration cfg,
            final List<DocWorkUnit> workUnitList,
            final List<Map<String, String>> groupMaps
   ) throws IOException {
        // Get or create a template and merge in the data
        final Template template = cfg.getTemplate(getIndexTemplateName());

        final File indexFile = new File(getDestinationDir(),
                            getIndexBaseFileName() + '.' + getIndexFileExtension()
        );

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

        // Note that these properties are inserted into the toplevel FreeMarker map for the WorkUnit typed
        // as Strings with values "true" or "false", but here the same entries are typed as Booleans in the
        // index.
        propertyMap.put("beta", Boolean.toString(workUnit.isBetaFeature()));
        propertyMap.put("experimental", Boolean.toString(workUnit.isExperimentalFeature()));
        propertyMap.put(TemplateProperties.FEATURE_DEPRECATED, Boolean.toString(workUnit.isDeprecatedFeature()));

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
            File outputPath = new File(getDestinationDir(), workUnit.getTargetFileName());
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
        
        // Convert object to JSON and write JSON entry to file
        File outputPathForJSON = new File(getDestinationDir(), workUnit.getJSONFileName());

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
        return new GSONWorkUnit(workUnit);
    }

}
