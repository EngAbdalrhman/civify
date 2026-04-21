package com.app.civify.exception;

/**
 * Exception thrown when an uploaded file is flagged as potentially malicious.
 * Kept separate from {@link BadRequestException} for clearer monitoring and logging.
 */
public class MaliciousFileException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String threatType;

    /**
     * @param message    Human-readable error message (or i18n key)
     * @param threatType Machine-readable threat category (e.g. "JAVASCRIPT_IN_PDF")
     */
    public MaliciousFileException(String message, String threatType) {
        super(message);
        this.threatType = threatType;
    }

    public String getThreatType() {
        return threatType;
    }
}
