package org.broadinstitute.barclay.help;

import com.sun.javadoc.ClassDoc;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.argparser.WorkflowProperties;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Custom Barclay-based Javadoc Doclet used for generating tool WDL.
 */
public class WDLDoclet extends HelpDoclet {

    /**
     * Create a WDL doclet and generate the FreeMarker templates properties.
     * @param rootDoc
     * @throws IOException
     */
    public static boolean start(final com.sun.javadoc.RootDoc rootDoc) throws IOException {
        return new WDLDoclet().startProcessDocs(rootDoc);
    }

    @Override
    public boolean includeInDocs(final DocumentedFeature documentedFeature, final ClassDoc classDoc, final Class<?> clazz) {
        if (super.includeInDocs(documentedFeature, classDoc, clazz)) {
            boolean hasWorkflowProperties = clazz.getAnnotation(WorkflowProperties.class) != null;
            boolean isCommandLineProgram = clazz.getAnnotation(CommandLineProgramProperties.class) != null;
            if (hasWorkflowProperties) {
                if (!isCommandLineProgram) {
                    throw new DocException(String.format(
                            "WorkflowProperties can only be applied to classes that are annotated with CommandLineProgramProperties (%s)",
                            clazz));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * @return Create and return a DocWorkUnit-derived object to handle WDLGen
     * for the target feature(s) represented by clazz.
     *
     * @param documentedFeature DocumentedFeature annotation for the target feature
     * @param classDoc javadoc classDoc for the target feature
     * @param clazz class of the target feature
     * @return DocWorkUnit to be used for this feature
     */
    @Override
    protected DocWorkUnit createWorkUnit(
            final DocumentedFeature documentedFeature,
            final com.sun.javadoc.ClassDoc classDoc,
            final Class<?> clazz)
    {
        return includeInDocs(documentedFeature, classDoc, clazz) ?
                // for WDL we don't need a custom DocWorkUnit, only a custom handler, so just use the
                // Barclay default DocWorkUnit class
                new DocWorkUnit(
                    new WDLWorkUnitHandler(this),
                    documentedFeature,
                    classDoc,
                    clazz) :
                null;
    }

    @Override
    protected void processWorkUnitTemplate(
            final Configuration cfg,
            final DocWorkUnit workUnit,
            final List<Map<String, String>> indexByGroupMaps,
            final List<Map<String, String>> featureMaps)
    {
        try {
            // Merge data-model with wdl template
            final Template wdlTemplate = cfg.getTemplate(workUnit.getTemplateName());
            final File wdlOutputPath = new File(getDestinationDir(), workUnit.getTargetFileName());
            try (final Writer out = new OutputStreamWriter(new FileOutputStream(wdlOutputPath))) {
                wdlTemplate.process(workUnit.getRootMap(), out);
            }

            // Rather than rely on the default Barclay JSON file that is created by the doc system, use a
            // separate template to allow more control over the initial values. Barclay would provide the
            // initial values everywhere, but for required args we want to use a String containing the
            // expected type, like womtool does.
            final Template jsonTemplate = cfg.getTemplate("wdlJSONTemplate.json.ftl");
            final File jsonOutputPath = new File(getDestinationDir(), workUnit.getJSONFileName());
            try (final Writer out = new OutputStreamWriter(new FileOutputStream(jsonOutputPath))) {
                jsonTemplate.process(workUnit.getRootMap(), out);
            }
        } catch (IOException e) {
            throw new DocException("IOException during documentation creation", e);
        } catch (TemplateException e) {
            throw new DocException("TemplateException during documentation creation", e);
        }
    }

}
