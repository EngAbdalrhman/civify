package com.app.civify.entity.enums;

/**
 * Type of CV generation
 */
public enum CvGenerationType {
    /**
     * CV generated via API call (with API key)
     */
    API,
    
    /**
     * CV generated via UI stepper (4-step process)
     */
    UI,
    
    /**
     * CV uploaded directly by user
     */
    UPLOADED
}
