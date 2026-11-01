package net.frosthain.core.validation;

import java.util.Comparator;

/**
 * A single finding of loading or validation. {@code location} names the file or content id,
 * {@code message} is German and meant to be shown to content authors directly.
 */
public record Diagnostic(Severity severity, String location, String message) {

    public enum Severity {
        /** Content is broken or not permitted; must not ship. */
        ERROR,
        /** Works, but probably unintended (e.g. missing English translation). */
        WARNING,
        /** Informational (e.g. a pack overrides another pack's definition). */
        INFO
    }

    public static final Comparator<Diagnostic> ORDER = Comparator
            .comparing(Diagnostic::severity)
            .thenComparing(Diagnostic::location)
            .thenComparing(Diagnostic::message);

    public static Diagnostic error(String location, String message) {
        return new Diagnostic(Severity.ERROR, location, message);
    }

    public static Diagnostic warning(String location, String message) {
        return new Diagnostic(Severity.WARNING, location, message);
    }

    public static Diagnostic info(String location, String message) {
        return new Diagnostic(Severity.INFO, location, message);
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + location + ": " + message;
    }
}
