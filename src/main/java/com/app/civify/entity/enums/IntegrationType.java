package com.app.civify.entity.enums;

/**
 * Types of integrations available in subscription plans
 */
public enum IntegrationType {
    /**
     * AI model integrations (GPT-4, Claude, etc.)
     */
    AI_MODEL("AI Model", "Access to AI language models"),
    
    /**
     * Cloud storage integrations (Google Drive, Dropbox, etc.)
     */
    CLOUD_STORAGE("Cloud Storage", "Cloud storage integrations"),
    
    /**
     * Job board integrations (LinkedIn, Indeed, etc.)
     */
    JOB_BOARD("Job Board", "Job board integrations"),
    
    /**
     * ATS (Applicant Tracking System) integrations
     */
    ATS("ATS", "Applicant Tracking System integrations"),
    
    /**
     * Email service integrations (Gmail, Outlook, etc.)
     */
    EMAIL_SERVICE("Email Service", "Email service integrations"),
    
    /**
     * Calendar integrations (Google Calendar, Outlook Calendar, etc.)
     */
    CALENDAR("Calendar", "Calendar integrations"),
    
    /**
     * CRM integrations (Salesforce, HubSpot, etc.)
     */
    CRM("CRM", "Customer Relationship Management integrations"),
    
    /**
     * Analytics integrations (Google Analytics, Mixpanel, etc.)
     */
    ANALYTICS("Analytics", "Analytics and tracking integrations"),
    
    /**
     * Payment gateway integrations
     */
    PAYMENT("Payment", "Payment gateway integrations"),
    
    /**
     * Webhook integrations
     */
    WEBHOOK("Webhook", "Custom webhook integrations"),
    
    /**
     * API access
     */
    API("API", "REST API access"),
    
    /**
     * Other third-party integrations
     */
    OTHER("Other", "Other integrations");

    private final String displayName;
    private final String description;

    IntegrationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}

