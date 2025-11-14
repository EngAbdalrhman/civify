package com.app.civify.entity.enums;

/**
 * Types of features available in subscription plans
 */
public enum FeatureType {
    /**
     * CV generation feature (with limit)
     */
    CV_GENERATION("CV Generation", "Generate tailored CVs", true),
    
    /**
     * API access feature (with limit)
     */
    API_ACCESS("API Access", "Access to REST API", true),
    
    /**
     * Watermark removal feature (boolean)
     */
    WATERMARK_REMOVAL("Watermark Removal", "Remove watermark from generated CVs", false),
    
    /**
     * Dashboard access feature (boolean)
     */
    DASHBOARD_ACCESS("Dashboard Access", "Access to analytics dashboard", false),
    
    /**
     * Premium templates feature (boolean)
     */
    PREMIUM_TEMPLATES("Premium Templates", "Access to premium CV templates", false),
    
    /**
     * AI model access (e.g., GPT-4, Claude)
     */
    AI_MODEL_ACCESS("AI Model Access", "Access to advanced AI models", false),
    
    /**
     * Priority support feature (boolean)
     */
    PRIORITY_SUPPORT("Priority Support", "24/7 priority customer support", false),
    
    /**
     * Custom branding feature (boolean)
     */
    CUSTOM_BRANDING("Custom Branding", "Add your own branding to CVs", false),
    
    /**
     * Export formats (PDF, DOCX, etc.)
     */
    EXPORT_FORMATS("Export Formats", "Export CVs in multiple formats", false),
    
    /**
     * Team collaboration feature (with user limit)
     */
    TEAM_COLLABORATION("Team Collaboration", "Collaborate with team members", true),
    
    /**
     * Storage space (with limit in GB)
     */
    STORAGE_SPACE("Storage Space", "Cloud storage for CVs and documents", true),
    
    /**
     * Integration access (third-party integrations)
     */
    INTEGRATION_ACCESS("Integration Access", "Access to third-party integrations", false);

    private final String displayName;
    private final String description;
    private final boolean hasLimit;

    FeatureType(String displayName, String description, boolean hasLimit) {
        this.displayName = displayName;
        this.description = description;
        this.hasLimit = hasLimit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasLimit() {
        return hasLimit;
    }
}

