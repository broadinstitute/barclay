package joptsimple;

import joptsimple.util.KeyValuePair;

/**
 * Custom subclass of the jopt parser for Barclay. Overrides short argument handling
 * so that short arg clustering is disabled.
 */
public class BarclayOptionParser extends OptionParser {
    public BarclayOptionParser(final boolean allowAbbreviations) {
        super(allowAbbreviations);
    }

    /**
     * Only delegate short arg handling to the base class if we're guaranteed that the option will be
     * recognized using the full argument string. Otherwise the base class implementation will fall back
     * to clustered short name recognition, which we want to disable to avoid the confusing error message
     * generated.
     */
    @Override
    void handleShortOptionToken( String candidate, ArgumentList arguments, OptionSet detected ) {
        KeyValuePair optionAndArgument = KeyValuePair.valueOf( candidate.substring( 1 ) );

        if ( isRecognized( optionAndArgument.key ) ) {
            super.handleShortOptionToken(candidate, arguments, detected );
        }
        else {
            throw new UnrecognizedOptionException(candidate);
        }
    }

}
