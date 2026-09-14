package com.app.civify.entity.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Granular permissions / scopes for Personal Access Tokens (PATs) and API Keys.
 */
public enum ApiKeyScope {
    // Resume AI & Documents
    CV_READ("cv:read", "View and list stored resumes"),
    CV_PARSE("cv:parse", "Parse resume documents into JSON"),
    CV_TAILOR("cv:tailor", "AI tailor resumes to job descriptions"),
    CV_EXPORT("cv:export", "Render and export styled PDFs/DOCX"),
    ATS_SCORE("ats:score", "Calculate ATS compatibility score"),
    PII_MASK("pii:mask", "Redact sensitive personal contact information"),

    // Job Intelligence
    JOBS_SCRAPE("jobs:scrape", "Scrape structured job descriptions from URLs"),

    // Job Applications (Kanban)
    APPS_READ("apps:read", "List and view tracked job applications"),
    APPS_WRITE("apps:write", "Create, update, and manage job applications"),

    // Monetization & Billing
    BILLING_READ("billing:read", "View Pay-Per-CV pricing and entitlements"),
    BILLING_PURCHASE("billing:purchase", "Initiate Pay-Per-CV pass checkouts"),

    // Account Profile
    ACCOUNT_READ("account:read", "View user profile, plan, and token balance");

    private final String value;
    private final String description;

    ApiKeyScope(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static Set<String> allValues() {
        return Arrays.stream(values())
                .map(ApiKeyScope::getValue)
                .collect(Collectors.toSet());
    }

    public static Set<String> defaultExtensionScopes() {
        return Set.of(
                CV_READ.getValue(),
                CV_PARSE.getValue(),
                CV_TAILOR.getValue(),
                CV_EXPORT.getValue(),
                ATS_SCORE.getValue(),
                PII_MASK.getValue(),
                JOBS_SCRAPE.getValue(),
                APPS_READ.getValue(),
                APPS_WRITE.getValue(),
                BILLING_READ.getValue(),
                ACCOUNT_READ.getValue()
        );
    }
}
