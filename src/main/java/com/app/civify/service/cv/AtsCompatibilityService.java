package com.app.civify.service.cv;

import com.app.civify.dto.cv.AtsFormatIssue;
import com.app.civify.dto.cv.AtsFormatIssue.Severity;
import com.app.civify.dto.cv.AtsFormatReport;
import com.app.civify.dto.cv.CvTailorResponse.AtsScore;
import com.app.civify.dto.resume.ResumeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic ATS format and parseability analysis based on common enterprise
 * parser behavior (Workday, Greenhouse, Taleo, iCIMS, Lever).
 */
@Service
@Slf4j
public class AtsCompatibilityService {

    private static final Set<String> SAFE_FONTS = Set.of(
            "inter", "arial", "calibri", "georgia", "times new roman", "helvetica", "roboto", "lato", "merriweather");

    private static final Set<String> VISUAL_SKILL_STYLES = Set.of("dots", "bar");
    private static final Set<String> HIGH_RISK_TEMPLATES = Set.of("creative");
    private static final Set<String> MEDIUM_RISK_TEMPLATES = Set.of("modern", "executive");

    private static final Map<String, Integer> TEMPLATE_BASE_FORMAT = Map.of(
            "ats", 96,
            "minimal", 90,
            "classic", 86,
            "modern", 76,
            "executive", 74,
            "creative", 52);

    public AtsFormatReport analyze(ResumeData resumeData) {
        List<AtsFormatIssue> issues = new ArrayList<>();
        List<String> exportHints = new ArrayList<>();

        String templateId = normalizeTemplateId(resumeData);
        int formatScore = TEMPLATE_BASE_FORMAT.getOrDefault(templateId, 72);
        int parseScore = formatScore;

        if (HIGH_RISK_TEMPLATES.contains(templateId)) {
            issues.add(issue("SIDEBAR_LAYOUT", Severity.CRITICAL,
                    "Sidebar or multi-column layouts often scramble reading order in ATS parsers",
                    "Switch to the ATS Optimized or Classic template"));
            formatScore -= 18;
            parseScore -= 22;
        } else if (MEDIUM_RISK_TEMPLATES.contains(templateId)) {
            issues.add(issue("TEMPLATE_MEDIUM_RISK", Severity.WARNING,
                    "This template may include icons or columns that some ATS systems skip",
                    "Use ATS Optimized template for corporate applications"));
            formatScore -= 8;
            parseScore -= 10;
        }

        ResumeData.ResumeMetadata meta = resumeData != null ? resumeData.getMetadata() : null;
        if (meta != null) {
            String skillsStyle = meta.getSkillsDisplayStyle();
            if (skillsStyle != null && VISUAL_SKILL_STYLES.contains(skillsStyle.toLowerCase(Locale.ROOT))) {
                issues.add(issue("VISUAL_SKILLS", Severity.WARNING,
                        "Visual skill bars and dots are not extracted as searchable text",
                        "Set skills display to Tags or Level (text)"));
                formatScore -= 10;
                parseScore -= 12;
                exportHints.add("Export with text-based skill tags for ATS uploads");
            }

            String langStyle = meta.getLanguagesDisplayStyle();
            if (langStyle != null && VISUAL_SKILL_STYLES.contains(langStyle.toLowerCase(Locale.ROOT))) {
                issues.add(issue("VISUAL_LANGUAGES", Severity.WARNING,
                        "Visual language proficiency indicators may not parse correctly",
                        "Use text level labels for languages"));
                formatScore -= 6;
                parseScore -= 8;
            }

            String font = meta.getFontFamily();
            if (font != null && !isSafeFont(font)) {
                issues.add(issue("DECORATIVE_FONT", Severity.WARNING,
                        "Decorative or monospace fonts can reduce OCR and parser accuracy",
                        "Use Arial, Calibri, Georgia, or Times New Roman"));
                formatScore -= 5;
                parseScore -= 6;
            }

            if (meta.getCustomStyles() != null && !meta.getCustomStyles().isEmpty()) {
                issues.add(issue("CUSTOM_STYLES", Severity.INFO,
                        "Custom inline styles may not survive PDF text extraction",
                        "Prefer the ATS Optimized template for uploads"));
                formatScore -= 3;
            }
        }

        if (resumeData != null && resumeData.getPersonalInfo() != null) {
            var pi = resumeData.getPersonalInfo();
            if (isBlank(pi.getEmail())) {
                issues.add(issue("MISSING_EMAIL", Severity.CRITICAL,
                        "Email is required for most ATS contact parsing", "Add a professional email"));
                formatScore -= 12;
                parseScore -= 10;
            }
            if (isBlank(pi.getFullName())) {
                issues.add(issue("MISSING_NAME", Severity.CRITICAL,
                        "Full name should appear at the top of the resume", "Add your full name"));
                formatScore -= 15;
                parseScore -= 15;
            }
        }

        if (hasHtmlInDescriptions(resumeData)) {
            issues.add(issue("HTML_IN_BODY", Severity.WARNING,
                    "HTML or rich formatting in descriptions may parse as garbled text",
                    "Use plain bullets and simple line breaks"));
            formatScore -= 6;
            parseScore -= 8;
        }

        if (!hasExperienceSection(resumeData)) {
            issues.add(issue("MISSING_EXPERIENCE", Severity.WARNING,
                    "A clearly labeled Experience section helps ATS categorize work history",
                    "Add an Experience section with standard heading"));
            formatScore -= 8;
        }

        if (!hasSkillsSection(resumeData)) {
            issues.add(issue("MISSING_SKILLS", Severity.INFO,
                    "A dedicated Skills section improves keyword matching in recruiter searches",
                    "Add a Skills section with plain-text skill names"));
            formatScore -= 4;
        }

        formatScore = clamp(formatScore);
        parseScore = clamp(parseScore);

        String readiness = toReadiness(formatScore);
        String recommended = recommendTemplate(templateId, formatScore);

        if (!"ats".equals(recommended) && formatScore < 80) {
            exportHints.add("Recommended: switch to ATS Optimized template before PDF export");
        }
        exportHints.add("Compatible with common parsers: Workday, Greenhouse, Lever, Taleo-style systems");

        return AtsFormatReport.builder()
                .formatScore(formatScore)
                .parseabilityScore(parseScore)
                .atsReadiness(readiness)
                .recommendedTemplateId(recommended)
                .issues(issues)
                .exportHints(exportHints)
                .build();
    }

    /**
     * Merges format analysis into an existing content/keyword ATS score.
     */
    public AtsScore enrichScore(AtsScore score, ResumeData resumeData) {
        if (score == null) {
            return null;
        }
        AtsFormatReport report = analyze(resumeData);

        int contentOverall = score.getOverall() >= 0 ? score.getOverall() : 0;
        int keyword = score.getKeywordMatch() >= 0 ? score.getKeywordMatch() : 0;
        int skills = score.getSkillsMatch() >= 0 ? score.getSkillsMatch() : 0;
        int secondary = (keyword + skills) / 2;
        int combinedFormat = (report.getFormatScore() + report.getParseabilityScore()) / 2;

        int newOverall = (int) Math.round(
                0.55 * contentOverall + 0.25 * secondary + 0.20 * combinedFormat);
        newOverall = clamp(newOverall);

        List<String> suggestions = new ArrayList<>();
        if (score.getSuggestions() != null) {
            suggestions.addAll(score.getSuggestions());
        }
        for (AtsFormatIssue issue : report.getIssues()) {
            if (issue.getSeverity() == Severity.CRITICAL || issue.getSeverity() == Severity.WARNING) {
                String hint = issue.getFixHint() != null ? issue.getFixHint() : issue.getMessage();
                if (!suggestions.contains(hint)) {
                    suggestions.add(hint);
                }
            }
        }
        if (suggestions.size() > 12) {
            suggestions = new ArrayList<>(suggestions.subList(0, 12));
        }

        List<AtsFormatIssue> criticalIssues = report.getIssues().stream()
                .filter(i -> i.getSeverity() != Severity.INFO)
                .limit(8)
                .toList();

        return AtsScore.builder()
                .overall(newOverall)
                .keywordMatch(score.getKeywordMatch())
                .skillsMatch(score.getSkillsMatch())
                .contentScore(contentOverall)
                .formatScore(report.getFormatScore())
                .parseabilityScore(report.getParseabilityScore())
                .missingKeywords(score.getMissingKeywords())
                .suggestions(suggestions)
                .formatIssues(criticalIssues)
                .atsReadiness(report.getAtsReadiness())
                .recommendedTemplateId(report.getRecommendedTemplateId())
                .tokensUsed(score.getTokensUsed())
                .build();
    }

    private static AtsFormatIssue issue(String code, Severity severity, String message, String fixHint) {
        return AtsFormatIssue.builder()
                .code(code)
                .severity(severity)
                .message(message)
                .fixHint(fixHint)
                .build();
    }

    private static String normalizeTemplateId(ResumeData data) {
        if (data == null || data.getMetadata() == null || data.getMetadata().getTemplateId() == null) {
            return "modern";
        }
        return data.getMetadata().getTemplateId().toLowerCase(Locale.ROOT);
    }

    private static String recommendTemplate(String current, int formatScore) {
        if (formatScore >= 85 && ("ats".equals(current) || "minimal".equals(current) || "classic".equals(current))) {
            return current;
        }
        return "ats";
    }

    private static String toReadiness(int formatScore) {
        if (formatScore >= 85) {
            return "excellent";
        }
        if (formatScore >= 70) {
            return "good";
        }
        if (formatScore >= 50) {
            return "caution";
        }
        return "poor";
    }

    private static boolean isSafeFont(String font) {
        String lower = font.toLowerCase(Locale.ROOT);
        return SAFE_FONTS.stream().anyMatch(lower::contains);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static boolean hasExperienceSection(ResumeData data) {
        if (data == null || data.getSections() == null) {
            return false;
        }
        return data.getSections().stream()
                .anyMatch(s -> "experience".equalsIgnoreCase(s.getType())
                        && s.getItems() != null
                        && !s.getItems().isEmpty());
    }

    private static boolean hasSkillsSection(ResumeData data) {
        if (data == null || data.getSections() == null) {
            return false;
        }
        return data.getSections().stream()
                .anyMatch(s -> "skills".equalsIgnoreCase(s.getType())
                        && s.getItems() != null
                        && !s.getItems().isEmpty());
    }

    private static boolean hasHtmlInDescriptions(ResumeData data) {
        if (data == null || data.getSections() == null) {
            return false;
        }
        for (ResumeData.ResumeSection section : data.getSections()) {
            if (section.getItems() == null) {
                continue;
            }
            for (ResumeData.SectionItem item : section.getItems()) {
                String desc = item.getDescription();
                if (desc != null && (desc.contains("<") || desc.contains("**"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
