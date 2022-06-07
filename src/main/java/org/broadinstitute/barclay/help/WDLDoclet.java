package org.broadinstitute.barclay.help;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jdk.javadoc.doclet.DocletEnvironment;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.argparser.WorkflowProperties;

import javax.lang.model.element.Element;
import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Custom Barclay-based Javadoc Doclet used for generating tool WDL.
 */
public class WDLDoclet extends HelpDoclet {

//    /**
//     * Create a WDL doclet and generate the FreeMarker templates properties.
//     * @param docEnv DocletEnvironment
//     */
//    public static boolean run(final DocletEnvironment docEnv) throws IOException {
//        return super.start;
//    }

    @Override
    public boolean includeInDocs(final DocumentedFeature documentedFeature, final Class<?> clazz) {
        if (super.includeInDocs(documentedFeature, clazz)) {
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
     * @param classElement Element for the target feature
     * @param clazz class of the target feature
     * @param documentedFeature DocumentedFeature annotation for the target feature
     * @return DocWorkUnit to be used for this feature
     */
    @Override
    public DocWorkUnit createWorkUnit(
            final Element classElement,
            final Class<?> clazz,
            final DocumentedFeature documentedFeature)
    {
        return includeInDocs(documentedFeature, clazz) ?
                // for WDL we don't need a custom DocWorkUnit, only a custom handler, so just use the
                // Barclay default DocWorkUnit class
                new DocWorkUnit(
                    new WDLWorkUnitHandler(this),
                        classElement,
                        clazz,
                        documentedFeature)
                : null;
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
