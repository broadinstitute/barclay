package org.broadinstitute.barclay.argparser;

/**
 * Exceptions thrown by CommandLineParser implementations.
 */
public class CommandLineException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CommandLineException( String msg ) {
        super(msg);
    }

    public CommandLineException( String message, Throwable throwable ) {
        super(message, throwable);
    }

    // todo -- fix up exception cause passing
    public static class MissingArgument extends CommandLineException {
        private static final long serialVersionUID = 0L;

        public MissingArgument(String arg, String message) {
            super(String.format("Argument %s was missing: %s", arg, message));
        }
    }

    public static class BadArgumentValue extends CommandLineException {
        private static final long serialVersionUID = 0L;

        public BadArgumentValue(String arg, String value) {
            super(String.format("Argument %s has a bad value: %s", arg, value));
        }

        public BadArgumentValue(String arg, String value, String message){
            super(String.format("Argument %s has a bad value: %s. %s", arg, value,message));
        }

        public BadArgumentValue(String message) {
            super(String.format("Illegal argument value: %s", message));
        }
    }

    /**
     * <p/>
     * Class CommandLineParserInternalException
     * <p/>
     * For internal errors in the command line parser not related to syntax errors in the command line itself.
     */
    public static class CommandLineParserInternalException extends CommandLineException {
        private static final long serialVersionUID = 0L;
        public CommandLineParserInternalException( final String s ) {
            super(s);
        }

        public CommandLineParserInternalException( final String s, final Throwable throwable ) {
            super(s, throwable);
        }
    }

    /**
     * For wrapping errors that are believed to never be reachable
     */
    public static class ShouldNeverReachHereException extends CommandLineException {
        private static final long serialVersionUID = 0L;
        public ShouldNeverReachHereException( final String s ) {
            super(s);
        }
        public ShouldNeverReachHereException( final String s, final Throwable throwable ) {
            super(s, throwable);
        }
        public ShouldNeverReachHereException( final Throwable throwable) {this("Should never reach here.", throwable);}
    }

}
