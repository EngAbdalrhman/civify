package com.app.civify.entity.enums;

/**
 * Types of token consumption in the system
 */
public enum TokenUsageType {
    /**
     * Parsing uploaded CV/Resume
     */
    CV_PARSE,
    
    /**
     * AI tailoring CV for job description
     */
    CV_TAILOR,
    
    /**
     * AI content generation
     */
    CONTENT_GENERATE,
    
    /**
     * Generic/other token usage
     */
    OTHER
}
