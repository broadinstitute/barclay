package org.broadinstitute.barclay.argparser;

import java.util.function.DoubleFunction;

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

    public static class OutOfRangeArgumentValue extends BadArgumentValue {
        private static final long serialVersionUID = 0L;

        public OutOfRangeArgumentValue(final String argName, final double minValue, final double maxValue, final Object value) {
            super(argName, getValueString(value), getMessage(minValue, maxValue, value instanceof Integer));
        }

        // to handle null values
        private static String getValueString(final Object value) {
            return (value == null) ? "null" : value.toString();
        }

        // get the message for the values, correctly formatted
        private static String getMessage(final double minValue, final double maxValue, final boolean asInt) {
            final boolean outMinValue = minValue != Double.NEGATIVE_INFINITY;
            final boolean outMaxValue = maxValue != Double.POSITIVE_INFINITY;
            final DoubleFunction<String> toString = (asInt) ? Double::toString : v -> Integer.toString((int) Math.rint(v));
            if (outMinValue && outMaxValue) {
                return String.format("allowed range [%s, %s].", toString.apply(minValue), toString.apply(maxValue));
            } else if (outMinValue) {
                return String.format("minimum allowed value %s", toString.apply(minValue));
            } else if (outMaxValue) {
                return String.format("maximum allowed value %s", toString.apply(maxValue));
            }
            // this should never be reached
            throw new IllegalArgumentException("Unbounded range should not thrown this exception");
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
