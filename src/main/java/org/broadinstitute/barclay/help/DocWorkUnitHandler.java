package org.broadinstitute.barclay.help;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.barclay.utils.Utils;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.Renderer;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for work unit handlers for docs. The DocWorkUnitHandler defines the template
 * used for each documented feature, and populates the template property map for that template.
 */
public abstract class DocWorkUnitHandler {
    /**
     * Default Markdown to HTML renderer.
     */
    // TODO: add extensions to default renderer?
    protected static final Renderer DEFAULT_RENDERER = HtmlRenderer.builder().build();

    private final HelpDoclet doclet;
    private final Renderer markdownRenderer;

    // default parser for Markdown formatted documentation
    // TODO: allow customization of Markdown parser
    private Parser markdownParser = Parser.builder().build();

    /**
     * @param doclet the HelpDoclet driving this documentation run. Can not be null.
     */
    public DocWorkUnitHandler(final HelpDoclet doclet) {
        this(doclet, DEFAULT_RENDERER);
    }

    /**
     * @param doclet the HelpDoclet driving this documentation run. Can not be null.
     * @param markdownRenderer renderer for rendering markdown formatted strings. Can not be null.
     */
    public DocWorkUnitHandler(final HelpDoclet doclet, final Renderer markdownRenderer) {
       this.doclet = Utils.nonNull(doclet, "Doclet cannot be null");
       this.markdownRenderer = Utils.nonNull(markdownRenderer, "Renderer cannot be null");
    }

    /**
     * @return the HelpDoclet driving this documentation run
     */
    public HelpDoclet getDoclet() {
        return doclet;
    }

    /**
     * @return rendered markdown string for documentation; empty String if the {@code markdownString} is null or empty.
     */
    public final String renderMarkdown(final String markdownString) {
        if (markdownString == null || markdownString.isEmpty()) {
            return "";
        }
        // render the String and remove the end of line added by the parser/renderer
        final String renderedString = StringUtils.stripEnd(markdownRenderer.render(markdownParser.parse(markdownString)), "\n");
        // Markdown parser/renderer adds always a <p></p> around the parsed String even if it is a single line
        // but we do not want this in single line documentation
        // TODO: commonmark should have a way to remove this behaviour from the parser/renderer
        if (!renderedString.contains("\n")) {
            return StringUtils.removePattern(renderedString, "^<p>|</p>$");
        }
        return renderedString;
    }

    /**
     * Actually generate the documentation map by populating the associated workUnit's properties.
     *
     * @param workUnit work unit to generate documentation for
     */
    public abstract void processWorkUnit(DocWorkUnit workUnit, List<Map<String, String>>featureMaps, List<Map<String, String>> groupMaps);

    /**
     * Return the name of the FreeMarker template to be used to process the work unit.
     *
     * Note this is a flat filename relative to settings/helpTemplates in the source tree
     * @param workUnit template to use for this work unit
     * @return name of the template
     * @throws IOException
     */
    public abstract String getTemplateName(DocWorkUnit workUnit);

    /**
     * Return the flat filename (no paths) that the handler would like the Doclet to
     * write out the documentation for workUnit
     * @param workUnit
     * @return the name of the destination file to which documentation output will be written
     */
    public String getDestinationFilename(final DocWorkUnit workUnit) {
        return DocletUtils.phpFilenameForClass(workUnit.getClazz(), HelpDoclet.outputFileExtension);
    }

    /**
     * Apply any fallback rules to determine the summary line that should be used for the work unit.
     * Default implementation uses the value from the DocumentedFeature annotation, rendered with {@link #renderMarkdown(String)}.
     * @param workUnit
     * @return Summary for this work unit.
     */
    public String getSummaryForWorkUnit(final DocWorkUnit workUnit) {
        return renderMarkdown(workUnit.getDocumentedFeature().summary());
    }

    /**
     * Apply any fallback rules to determine the group name line that should be used for the work unit.
     * Default implementation uses the value from the DocumentedFeature annotation.
     * @param workUnit
     * @return Group name to be used for this work unit.
     */
    public String getGroupNameForWorkUnit(final DocWorkUnit workUnit) {
        return workUnit.getDocumentedFeature().groupName();
    }

    /**
     * Apply any fallback rules to determine the group summary line that should be used for the work unit.
     * Default implementation uses the value from the DocumentedFeature annotation, rendered with {@link #renderMarkdown(String)}.
     * @param workUnit
     * @return Group summary to be used for this work unit.
     */
    public String getGroupSummaryForWorkUnit(final DocWorkUnit workUnit) {
        return renderMarkdown(workUnit.getDocumentedFeature().groupSummary());
    }

}
