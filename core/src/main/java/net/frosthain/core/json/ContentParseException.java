package net.frosthain.core.json;

import java.io.Serial;

/** Thrown when a content file cannot be read. The message always contains the JSON path of the problem. */
public final class ContentParseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String jsonPath;

    public ContentParseException(String jsonPath, String message) {
        super(jsonPath + ": " + message);
        this.jsonPath = jsonPath;
    }

    public ContentParseException(String jsonPath, String message, Throwable cause) {
        super(jsonPath + ": " + message, cause);
        this.jsonPath = jsonPath;
    }

    public String jsonPath() {
        return jsonPath;
    }
}
