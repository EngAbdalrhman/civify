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
     * Cover letter generation
     */
    COVER_LETTER,
    
    /**
     * Interview preparation questions
     */
    INTERVIEW_PREP,
    
    /**
     * Career roadmap generation
     */
    ROADMAP,
    
    /**
     * ATS score calculation
     */
    ATS_SCORE,
    
    /**
     * AI content generation (general)
     */
    CONTENT_GENERATE,
    
    /**
     * Document masking (PII redaction)
     */
    DOC_MASK,
    
    /**
     * Document text extraction
     */
    DOC_EXTRACT,
    
    /**
     * OCR processing
     */
    DOC_OCR,
    
    /**
     * External API token usage
     */
    EXTERNAL_API,
    
    /**
     * Generic/other token usage
     */
    OTHER
}

