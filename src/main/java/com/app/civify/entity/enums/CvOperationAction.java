package com.app.civify.entity.enums;

/**
 * Actions that can be performed on CV operations.
 * Used for dedicated CV operation logging separate from general audit logs.
 */
public enum CvOperationAction {
    /**
     * CV file upload and initial parse
     */
    UPLOAD,

    /**
     * External API parse operation
     */
    PARSE,

    /**
     * CV tailoring for job description
     */
    TAILOR,

    /**
     * PII masking operation
     */
    MASK
}
