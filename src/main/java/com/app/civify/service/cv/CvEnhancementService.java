package com.app.civify.service.cv;

import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.cv.CvTailorResponse.*;
import com.app.civify.dto.cv.CvTranslateResponse;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.service.ai.AiService;
import com.app.civify.service.ai.JsonSanitizer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import com.app.civify.dto.ai.AiModelResponse;

/**
 * Service for CV enhancement operations including tailoring, ATS scoring,
 * interview prep, roadmap generation, and cover letter creation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvEnhancementService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final KeywordMatcher keywordMatcher;
    private final ContentCleaner contentCleaner;
    private final ResumeDiffService resumeDiffService;
    private final JsonSanitizer jsonSanitizer;
    private final AtsCompatibilityService atsCompatibilityService;

    @Value("${ai.default.provider:gemini}")
    private String defaultProvider;

    @Value("${ai.default.model:gemini-pro}")
    private String defaultModel;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();
    static {
        YAML_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    // ==================== YAML Prompts for Token Optimization ====================

    private static final String TAILOR_CV_PROMPT = """
            You are an expert CV writer. Your goal is to optimize this CV for the provided Job Description (JD) while maintaining strict truthfulness.

            STRATEGY:
            1. ANALYZE relevance: Does the candidate's experience actually align with the JD?
            2. IF RELEVANT: Incorporate JD specific keywords (skills, methodologies) into existing experience descriptions.
            3. IF NOT RELEVANT (e.g., mismatched industry): DO NOT force keywords. Focus on professional formatting and clarity. Improve valid verbs and metrics.

            TRUTHFULNESS RULES (CRITICAL):
            1. DO NOT INVENT experience, skills, or credentials.
            2. DO NOT use hyperbole like "superhero", "rockstar", "ninja", "unparalleled".
            3. KEEP ORIGINAL dates, locations, company names, job titles.
            4. YOU MAY rephrase descriptions to align with JD terminology ONLY IF the underlying work supports it.
            5. DO NOT add "implied" skills unless you are 100%% certain the candidate has them based on the text.

            ATS-FRIENDLY WRITING:
            - Use standard section titles where possible (Experience, Education, Skills)
            - Prefer bullet lines: action + scope + measurable result
            - Plain text only in descriptions; no HTML or markdown

            CRITICAL OUTPUT RULES:
            1. Output ONLY valid YAML. No conversational text before or after.
            2. Do NOT include markdown formatting, backticks, or preamble in any language.
            3. Do NOT translate YAML key names. Keep all keys in English exactly as shown.
            4. Start your response directly with "personalInfo:" — nothing before it.

            Job Description:
            {JOB_DESCRIPTION}

            Current CV (YAML):
            {CV_YAML}

            Output YAML only (strict valid YAML):
            personalInfo:
              fullName: [keep original]
              email: [keep original]
              phone: [keep original]
              city: [keep original]
              country: [keep original]
              linkedin: [keep original]
              github: [keep original]
              website: [keep original]
              summary: [Rewrite to be professional and concise. Incorporate JD keywords ONLY if relevant.]
              jobTitle: [match JD job title if experience supports it, otherwise keep original]
            sections:
              - id: [keep original]
                title: [keep original]
                type: [keep original]
                items:
                  - id: [keep original]
                    title: [keep original]
                    subtitle: [keep original]
                    date: [keep original]
                    description: [Expand descriptions meaningfully, integrating relevant JD keywords NATURALLY. Remove weak language.]
                    location: [keep original]
                    tags: [Extract key skills into list]
            metadata:
              language: [keep original]
            """;

    private static final String ATS_SCORE_PROMPT = """
            You are a senior recruiter evaluating this CV against a job description for ATS screening.
            Recruiters use systems like Workday, Greenhouse, Lever, and Taleo that extract plain text from PDFs.

            Score honestly (0-100):
            - keywordMatch: how many JD keywords/skills appear in the CV (searchable text only)
            - skillsMatch: relevance and clarity of skills and experience to the role
            - overall: combined fit for passing automated screening AND human review

            FORMAT AWARENESS (if metadata.templateId is creative, or skills use dots/bar, note in suggestions):
            - Prefer standard headings: Experience, Education, Skills, Summary
            - Bullets: action verb + scope + measurable outcome
            - Do not reward keyword stuffing or invented experience

            Job Description:
            {JOB_DESCRIPTION}

            CV Summary:
            {CV_YAML}

            Output YAML only:
            overall: 0-100
            keywordMatch: 0-100
            skillsMatch: 0-100
            missingKeywords:
              - keyword1
            suggestions:
              - specific professional improvement
            """;

    private static final String GENERAL_ATS_SCORE_PROMPT = """
            You are an expert recruiter and ATS analyst. Evaluate this CV as it would be read after PDF text extraction
            (Workday, Greenhouse, Lever, Taleo-style parsers). Be realistic and professional, not generous.

            CV Content:
            {CV_YAML}

            CONTENT QUALITY (reflect in overall and keywordMatch):
            - Strong action verbs (Led, Built, Delivered) with quantified results where present
            - Professional tone; no hype (rockstar, ninja, unparalleled)
            - Complete contact block: name, email, phone

            STRUCTURE (reflect in skillsMatch):
            - Standard section headings recruiters expect: Experience, Education, Skills, Summary
            - Reverse-chronological experience; bullet points over dense paragraphs
            - Skills as plain text lists, not graphics

            Output YAML only:
            overall: 0-100 (content quality for ATS + recruiters)
            keywordMatch: 0-100 (action verbs, metrics, clarity)
            skillsMatch: 0-100 (structure and skills presentation)
            missingKeywords:
              - specific gaps (e.g., "quantifiable metrics in experience bullets")
            suggestions:
              - concrete, professional improvements (max 6)

            CRITICAL: Plain text only in YAML values. No markdown bold (**).
            """;

    private static final String INTERVIEW_QUESTIONS_PROMPT = """
            Generate interview questions for this job role based on the CV.

            Job: {JOB_TITLE}
            Company: {COMPANY_NAME}
            CV Skills: {CV_SKILLS}

            Output YAML only (max 5 questions):
            questions:
              - question: text
                hint: brief answer guidance
                category: behavioral|technical|situational

            CRITICAL: DO NOT use any markdown formatting, bold (**), or asterisks in your YAML output. Return plain text only for all values.
            """;

    private static final String ROADMAP_PROMPT = """
            Create a career roadmap to bridge gaps between CV and job requirements.

            Job Description:
            {JOB_DESCRIPTION}

            Current Skills: {CV_SKILLS}

            Output YAML only:
            skillGaps:
              - skill1
              - skill2
            steps:
              - step: action description
                timeframe: 1-2 weeks|1 month|2-3 months

            CRITICAL: DO NOT use any markdown formatting, bold (**), or asterisks in your YAML output. Return plain text only for all values.
            """;

    private static final String COVER_LETTER_PROMPT = """
            You are an expert career coach. Write a compelling cover letter BODY for the following candidate.

            Target Job Title: {JOB_TITLE}
            Target Company: {COMPANY_NAME}

            Job Description:
            {JOB_DESCRIPTION}

            Candidate:
            Name: {CV_NAME}
            Email: {CV_EMAIL}
            Phone: {CV_PHONE}

            Candidate Profile:
            Summary: {CV_SUMMARY}
            Top Skills: {CV_SKILLS}
            Experience Highlights: {CV_EXPERIENCE}

            Job Description Analysis (Pain Points):
            {JOB_PAIN_POINTS}

            Instructions:
            1. Use the "Hook-Story-Offer" framework in 3-4 short paragraphs.
            2. Open with "Dear Hiring Manager," (or "Dear {COMPANY_NAME} Hiring Team," when company is known).
            3. Reference {COMPANY_NAME} and {JOB_TITLE} naturally when provided.
            4. Tone: professional, confident, authentic. Use metrics from the profile where possible.
            5. Do NOT include a letterhead, sender address block, date line, or signature block — those are added separately.
            6. NEVER use bracket placeholders such as [Your Name], [Date], [Company Name], or similar.

            Output ONLY the letter body (salutation through closing paragraph before signature). Plain text, no markdown.
            """;

    private static final Pattern BRACKET_PLACEHOLDER = Pattern.compile("\\[[^\\]]+\\]");

    private static final String TRANSLATE_CV_PROMPT = """
            You are an expert professional translator. Translate this CV into the target language.

            Target Language: {TARGET_LANGUAGE}

            TRANSLATE_CV_PROMPT RULES (CRITICAL):
            1. Translate ALL descriptive text, summaries, item descriptions, tags, and section titles into the target language.
            2. KEEP original PII fields EXACTLY as they are: fullName, email, phone, linkedin, github, website.
            3. KEEP dates and locations in their original format unless translation is strictly necessary (e.g. "Present" -> "الحالي").
            4. Ensure the translation sounds professional and uses appropriate industry terminology for the target language.
            5. Set metadata.language to the ISO 639-1 code of the target language.
            6. Set metadata.layoutDirection to "rtl" if translating to Arabic, Farsi, Urdu, or Hebrew. Otherwise "ltr".

            CRITICAL OUTPUT RULES:
            1. Output ONLY valid YAML. No conversational text before or after.
            2. Do NOT include markdown formatting, backticks, or preamble in any language.
            3. Do NOT translate YAML key names. Keep all keys in English exactly as shown.
            4. Start your response directly with "personalInfo:" — nothing before it.

            Current CV (YAML):
            {CV_YAML}

            Output YAML only (strict valid YAML):
            """;

    // ==================== Main Tailor Method ====================

    public CvTailorResponse tailorCv(CvTailorRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting CV tailoring for job: {}",
                request.getJobTitle() != null ? request.getJobTitle() : "unspecified");

        // Thread-safe token accumulator for parallel tasks
        AtomicInteger tokenTracker = new AtomicInteger(0);

        // Capture MDC context so async threads inherit the requestId for logging
        Map<String, String> mdcContext = MDC.getCopyOfContextMap() != null
                ? MDC.getCopyOfContextMap() : Collections.emptyMap();

        try {
            // Phase 1: Start parallel tasks that don't depend on tailored CV
            // - Tailor the CV content
            // - Calculate ORIGINAL ATS score (before tailoring)
            // - Other optional tasks
            CompletableFuture<ResumeData> tailoredCvFuture = CompletableFuture.supplyAsync(
                    withMdc(mdcContext, () -> tailorCvContent(request.getResumeData(), request.getJobDescription(), tokenTracker)),
                    executor);

            // Calculate ORIGINAL CV's ATS score for "before" comparison
            CompletableFuture<AtsScore> originalAtsScoreFuture = CompletableFuture.supplyAsync(
                    withMdc(mdcContext, () -> calculateAtsScore(request.getResumeData(), request.getJobDescription(), tokenTracker)),
                    executor);

            CompletableFuture<InterviewPrep> interviewFuture = request.isIncludeInterviewQuestions()
                    ? CompletableFuture.supplyAsync(
                            withMdc(mdcContext, () -> generateInterviewQuestions(request, tokenTracker)), executor)
                    : CompletableFuture.completedFuture(null);

            CompletableFuture<Roadmap> roadmapFuture = request.isIncludeRoadmap()
                    ? CompletableFuture.supplyAsync(
                            withMdc(mdcContext, () -> generateRoadmap(request, tokenTracker)), executor)
                    : CompletableFuture.completedFuture(null);

            CompletableFuture<String> coverLetterFuture = request.isGenerateCoverLetter()
                    ? CompletableFuture.supplyAsync(
                            withMdc(mdcContext, () -> generateCoverLetter(request, tokenTracker)), executor)
                    : CompletableFuture.completedFuture(null);

            // Wait for tailored CV to be ready first
            ResumeData tailoringResult = tailoredCvFuture.get();

            // Phase 1.5: Post-Tailoring Refinement (Keyword Gap Injection)
            // Ensure we don't lose keywords that were present in the original CV
            // This guarantees tailored_score >= original_score for keyword component
            final ResumeData refinedCv = refineAfterTailoring(request.getResumeData(), tailoringResult,
                    request.getJobDescription());

            // Phase 2: Calculate TAILORED CV's ATS score (this is the "after" score)
            CompletableFuture<AtsScore> tailoredAtsScoreFuture = CompletableFuture.supplyAsync(
                    withMdc(mdcContext, () -> calculateAtsScore(refinedCv, request.getJobDescription(), tokenTracker)), executor);

            // Wait for all remaining futures
            CompletableFuture.allOf(originalAtsScoreFuture, tailoredAtsScoreFuture,
                    interviewFuture, roadmapFuture, coverLetterFuture).join();

            AtsScore tailoredAtsScore = tailoredAtsScoreFuture.get();
            AtsScore originalAtsScore = originalAtsScoreFuture.get();

            // Report the measured result, including regressions and scoring fallbacks.
            // Raising it to the original score hides failures from users and agents.

            // Truthfulness Validation
            List<String> validationWarnings = validateAlignment(request.getResumeData(), refinedCv);

            // Generate Change Diffs using Robust Service
            List<String> changes = resumeDiffService.calculateDiff(request.getResumeData(), refinedCv);

            int totalTokens = tokenTracker.get();
            log.info("CV Tailoring complete. Total tokens used: {}", totalTokens);

            return CvTailorResponse.builder()
                    .tailoredCv(refinedCv)
                    .atsScore(tailoredAtsScore)
                    .originalAtsScore(originalAtsScore)
                    .interviewPrep(interviewFuture.get())
                    .roadmap(roadmapFuture.get())
                    .coverLetter(coverLetterFuture.get())
                    .changes(changes)
                    .validationWarnings(validationWarnings)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .totalTokensUsed(totalTokens)
                    .build();

        } catch (Exception e) {
            log.error("Error tailoring CV: {}", e.getMessage(), e);
            // Return partial response with ATS score if available
            return CvTailorResponse.builder()
                    .tailoredCv(request.getResumeData()) // Return original if tailoring failed
                    .atsScore(AtsScore.builder()
                            .overall(-1)
                            .suggestions(List.of("Error processing: " + e.getMessage()))
                            .build())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .totalTokensUsed(tokenTracker.get()) // Return what we counted so far
                    .build();
        }
    }

    // ==================== Individual AI Methods ====================

    public CvTranslateResponse translateCv(ResumeData resumeData, String targetLanguage) {
        long startTime = System.currentTimeMillis();
        log.info("Starting CV translation to language: {}", targetLanguage);

        try {
            String cvYaml = convertToYaml(resumeData);
            String promptBody = TRANSLATE_CV_PROMPT
                    .replace("{TARGET_LANGUAGE}", targetLanguage != null ? targetLanguage : "English")
                    .replace("{CV_YAML}", cvYaml != null ? cvYaml : "");

            AiModelResponse response = aiService.callModel(promptBody, defaultProvider, defaultModel);

            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());
            ResumeData rawData = parseYamlToResumeData(cleanedResponse);

            if (rawData == null || jsonSanitizer.isTruncated(rawData)) {
                log.warn("Tailored CV parsing failed or resulted in truncated data. Attempting sanitization and retry.");
                String sanitized = sanitizeYamlValues(cleanedResponse);
                rawData = parseYamlToResumeData(sanitized);
                if (rawData == null || jsonSanitizer.isTruncated(rawData)) {
                    throw new RuntimeException("Tailored CV parsing failed or resulted in truncated data even after sanitization");
                }
            }

            // Optional: clean clichés? Typically we shouldn't modify translator's choices too much,
            // but we can if desired. For now, let's keep the raw translated data.

            return CvTranslateResponse.builder()
                    .translatedCv(rawData)
                    .sourceLanguage(resumeData.getMetadata() != null ? resumeData.getMetadata().getLanguage() : "unknown")
                    .targetLanguage(targetLanguage)
                    .tokensUsed(response.getTotalTokens())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Error translating CV content: {}", e.getMessage());
            throw new RuntimeException("Failed to translate CV", e);
        }
    }

    private ResumeData tailorCvContent(ResumeData resumeData, String jobDescription, AtomicInteger tokenTracker) {
        try {
            // 0. Check Relevance First
            String cvTextForMatching = extractFullText(resumeData);
            KeywordMatcher.MatchResult matchResult = keywordMatcher.match(cvTextForMatching, jobDescription);
            boolean isLowRelevance = matchResult.score() < 10;

            String cvYaml = convertToYaml(resumeData);
            String promptBody = TAILOR_CV_PROMPT
                    .replace("{JOB_DESCRIPTION}", jobDescription != null ? jobDescription : "")
                    .replace("{CV_YAML}", cvYaml != null ? cvYaml : "");

            // Append strict warning if relevance is low
            if (isLowRelevance) {
                promptBody += "\n\nCRITICAL WARNING: The Job Description appears unrelated to this CV (Low Keyword Match). "
                        +
                        "DO NOT force-fit keywords. DO NOT invent skills. Focus ONLY on professional formatting and clarity. "
                        +
                        "If you cannot truthfully incorporate keywords, KEEP ORIGINAL CONTENT.";
                log.warn("Low keyword match ({}%) detected. Enabling strict no-hallucination mode.",
                        matchResult.score());
            }

            AiModelResponse response = aiService.callModel(promptBody, defaultProvider, defaultModel);
            if (tokenTracker != null) {
                tokenTracker.addAndGet(response.getTotalTokens());
            }

            // 1. Clean AI response (remove markdown blocks, extract content)
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());

            // 2. Parse Valid YAML to Object
            ResumeData rawData = parseYamlToResumeData(cleanedResponse);

            // 3. Guard: if parsing failed, fall back to original
            if (rawData == null || jsonSanitizer.isTruncated(rawData)) {
                log.warn("Tailored CV parsing failed or resulted in truncated data, falling back to original CV");
                return resumeData;
            }

            // 4. Clean "AI Clichés" (Polish) from values
            return contentCleaner.clean(rawData);
        } catch (Exception e) {
            log.error("Error tailoring CV content: {}", e.getMessage());
            return resumeData; // Return original on failure
        }
    }

    private AtsScore calculateAtsScore(ResumeData resumeData, String jobDescription, AtomicInteger tokenTracker) {
        // 1. Calculate deterministic keyword match score
        String cvText = extractFullText(resumeData);
        KeywordMatcher.MatchResult keywordResult = keywordMatcher.match(cvText, jobDescription);
        int consumedTokens = 0;

        try {
            String cvYaml = convertToYaml(resumeData);
            String promptBody = ATS_SCORE_PROMPT
                    .replace("{JOB_DESCRIPTION}", jobDescription != null ? jobDescription : "")
                    .replace("{CV_YAML}", cvYaml != null ? cvYaml : "");

            AiModelResponse response = aiService.callModel(promptBody, defaultProvider, defaultModel);
            consumedTokens = response.getTotalTokens();
            if (tokenTracker != null) {
                tokenTracker.addAndGet(response.getTotalTokens());
            }

            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());

            AtsScore aiScore = parseYamlToAtsScore(cleanedResponse);
            if (aiScore.getOverall() < 0) {
                throw new IllegalStateException("AI score response was invalid");
            }

            // 2. Hybrid Score Calculation
            // Weigh AI score 60% and Keyword score 40%
            int hybridOverall = (int) Math.round((aiScore.getOverall() * 0.6) + (keywordResult.score() * 0.4));

            // Deterministic floor: hybrid score must not fall below keyword score
            hybridOverall = Math.max(hybridOverall, keywordResult.score());

            // Merge missing keywords (AI + Deterministic)
            List<String> mergedMissingKeywords = new ArrayList<>();
            if (aiScore.getMissingKeywords() != null) {
                mergedMissingKeywords.addAll(aiScore.getMissingKeywords());
            }
            for (String kw : keywordResult.missingKeywords()) {
                if (mergedMissingKeywords.size() >= 15)
                    break;
                if (!mergedMissingKeywords.contains(kw)) {
                    mergedMissingKeywords.add(kw);
                }
            }

            AtsScore hybrid = AtsScore.builder()
                    .overall(hybridOverall)
                    .keywordMatch(keywordResult.score())
                    .skillsMatch(Math.max(aiScore.getSkillsMatch(), 0))
                    .missingKeywords(mergedMissingKeywords)
                    .suggestions(aiScore.getSuggestions())
                    .analysisSource("AI_HYBRID")
                    .tokensUsed(response.getTotalTokens())
                    .build();
            return atsCompatibilityService.enrichScore(hybrid, resumeData);

        } catch (Exception e) {
            log.warn("ATS AI analysis unavailable; using keyword fallback ({})", e.getClass().getSimpleName());
            AtsScore fallback = AtsScore.builder()
                    .overall(keywordResult.score())
                    .keywordMatch(keywordResult.score())
                    .skillsMatch(0)
                    .missingKeywords(keywordResult.missingKeywords())
                    .suggestions(List.of("AI analysis failed. Score based on keyword matching only."))
                    .tokensUsed(consumedTokens)
                    .analysisSource("KEYWORD_FALLBACK")
                    .build();
            return atsCompatibilityService.enrichScore(fallback, resumeData);
        }
    }

    /**
     * JD-specific ATS score for job match and external callers.
     */
    public AtsScore scoreResumeForJob(ResumeData resumeData, String jobDescription) {
        return calculateAtsScore(resumeData, jobDescription, null);
    }

    /**
     * Calculate general ATS score for a CV without a specific job description.
     * Evaluates based on industry best practices: action verbs, metrics, structure,
     * etc.
     *
     * @param resumeData the resume data to analyze
     * @return AtsScore with overall score and suggestions
     */
    public AtsScore calculateGeneralAtsScore(ResumeData resumeData) {
        try {
            String cvYaml = convertToYaml(resumeData);
            String promptBody = GENERAL_ATS_SCORE_PROMPT
                    .replace("{CV_YAML}", cvYaml != null ? cvYaml : "");

            AiModelResponse response = aiService.callModel(promptBody, defaultProvider, defaultModel);
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());

            AtsScore score = parseYamlToAtsScore(cleanedResponse);
            score.setTokensUsed(response.getTotalTokens());
            return score;
        } catch (Exception e) {
            log.error("Error calculating general ATS score: {}", e.getMessage());
            return AtsScore.builder()
                    .overall(-1)
                    .keywordMatch(-1)
                    .skillsMatch(-1)
                    .missingKeywords(new ArrayList<>())
                    .suggestions(List.of("Could not fully analyze. Please review manually."))
                    .tokensUsed(0)
                    .build();
        }
    }

    /**
     * Refines the tailored CV by injecting keywords that were present in the
     * original CV
     * but lost during the AI tailoring process.
     * This ensures fairness and prevents score regression.
     */
    private ResumeData refineAfterTailoring(ResumeData original, ResumeData tailored, String jd) {
        // Step 1: Restore any section items (courses, skills) dropped by the AI
        restoreLostSectionItems(original, tailored);

        String originalText = extractFullText(original).toLowerCase();
        String tailoredText = extractFullText(tailored).toLowerCase();

        Set<String> jdKeywords = keywordMatcher.extractKeywords(jd);

        // Step 2: Find JD keywords missing from Tailored CV
        List<String> missingFromTailored = jdKeywords.stream()
                .filter(kw -> !keywordMatcher.containsKeyword(tailoredText, kw))
                .collect(Collectors.toList());

        if (missingFromTailored.isEmpty())
            return tailored;

        // Of those missing, which ones EXIST in the Original CV? (Truthful injection)
        List<String> injectableKeywords = missingFromTailored.stream()
                .filter(kw -> keywordMatcher.containsKeyword(originalText, kw))
                .collect(Collectors.toList());

        if (!injectableKeywords.isEmpty()) {
            log.info("Refining tailored CV: Injecting {} lost keywords: {}", injectableKeywords.size(),
                    injectableKeywords);
            return injectMissingKeywords(tailored, injectableKeywords);
        }

        return tailored;
    }

    /**
     * Restores section items (e.g. skill titles, course titles) that existed in the
     * original CV
     * but were dropped by the AI during tailoring.
     */
    private void restoreLostSectionItems(ResumeData original, ResumeData tailored) {
        if (original.getSections() == null || tailored.getSections() == null)
            return;

        for (ResumeData.ResumeSection origSection : original.getSections()) {
            if (origSection.getItems() == null || origSection.getItems().isEmpty())
                continue;

            // An item can be renamed or moved to another section of the same type.
            // Stable IDs identify it; titles alone caused duplicate projects/degrees.
            List<ResumeData.SectionItem> missing = origSection.getItems().stream()
                    .filter(originalItem -> tailored.getSections().stream()
                            .filter(section -> java.util.Objects.equals(origSection.getType(), section.getType()))
                            .filter(section -> section.getItems() != null)
                            .flatMap(section -> section.getItems().stream())
                            .noneMatch(item -> matchesItem(originalItem, item)))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (missing.isEmpty()) continue;

            // Find matching section in tailored CV by id or type+title.
            ResumeData.ResumeSection tailoredSection = tailored.getSections().stream()
                    .filter(s -> matchesSection(origSection, s))
                    .findFirst()
                    .orElse(null);

            if (tailoredSection == null) {
                ResumeData.ResumeSection restored = objectMapper.convertValue(origSection, ResumeData.ResumeSection.class);
                restored.setItems(missing);
                tailored.getSections().add(restored);
                log.info("Restored {} missing CV items", missing.size());
                continue;
            }

            if (tailoredSection.getItems() == null) {
                tailoredSection.setItems(new ArrayList<>());
            }

            tailoredSection.getItems().addAll(missing);
            log.info("Restored {} missing CV items", missing.size());
        }
    }

    private boolean matchesItem(ResumeData.SectionItem original, ResumeData.SectionItem tailored) {
        if (original.getId() != null && !original.getId().isBlank()
                && original.getId().equals(tailored.getId())) return true;
        return !normalizeValue(original.getTitle()).isEmpty()
                && normalizeValue(original.getTitle()).equals(normalizeValue(tailored.getTitle()))
                && normalizeValue(original.getSubtitle()).equals(normalizeValue(tailored.getSubtitle()))
                && normalizeValue(original.getDate()).equals(normalizeValue(tailored.getDate()));
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesSection(ResumeData.ResumeSection a, ResumeData.ResumeSection b) {
        if (a.getId() != null && a.getId().equals(b.getId()))
            return true;
        if (a.getType() != null && a.getType().equalsIgnoreCase(b.getType())
                && a.getTitle() != null && a.getTitle().equalsIgnoreCase(b.getTitle()))
            return true;
        return false;
    }

    private ResumeData injectMissingKeywords(ResumeData tailored, List<String> keywordsToInject) {
        if (keywordsToInject.isEmpty())
            return tailored;

        // 1. Find "Skills" section
        ResumeData.ResumeSection skillsSection = tailored.getSections().stream()
                .filter(s -> (s.getType() != null && "skills".equalsIgnoreCase(s.getType())) ||
                        (s.getTitle() != null && "skills".equalsIgnoreCase(s.getTitle())))
                .findFirst()
                .orElse(null);

        // 2. If not found, create it
        if (skillsSection == null) {
            skillsSection = new ResumeData.ResumeSection();
            skillsSection.setType("skills");
            skillsSection.setTitle("Skills");
            skillsSection.setId("skills_" + System.currentTimeMillis());
            skillsSection.setItems(new ArrayList<>());
            // Add to sections
            tailored.getSections().add(skillsSection);
        }

        // 3. Find or create item to hold tags
        ResumeData.SectionItem skillsItem;
        if (skillsSection.getItems() == null) {
            skillsSection.setItems(new ArrayList<>());
        }

        if (skillsSection.getItems().isEmpty()) {
            skillsItem = new ResumeData.SectionItem();
            skillsItem.setId("item_skills_" + System.currentTimeMillis());
            skillsItem.setTitle("Core Competencies");
            skillsItem.setTags(new ArrayList<>());
            skillsSection.getItems().add(skillsItem);
        } else {
            skillsItem = skillsSection.getItems().get(0);
            if (skillsItem.getTags() == null) {
                skillsItem.setTags(new ArrayList<>());
            }
        }

        // 4. Inject unique keywords
        for (String kw : keywordsToInject) {
            final String keyword = kw; // lambda constraint
            boolean exists = skillsItem.getTags().stream()
                    .anyMatch(t -> t.equalsIgnoreCase(keyword));
            if (!exists) {
                skillsItem.getTags().add(kw);
            }
        }

        return tailored;
    }

    private InterviewPrep generateInterviewQuestions(CvTailorRequest request, AtomicInteger tokenTracker) {
        try {
            String skills = extractSkills(request.getResumeData());
            String promptBody = INTERVIEW_QUESTIONS_PROMPT
                    .replace("{JOB_TITLE}", request.getJobTitle() != null ? request.getJobTitle() : "the position")
                    .replace("{COMPANY_NAME}",
                            request.getCompanyName() != null ? request.getCompanyName() : "the company")
                    .replace("{CV_SKILLS}", skills);

            log.debug("Interview questions prompt length: {}", promptBody.length());
            AiModelResponse response = aiService.callModel(promptBody, defaultProvider, defaultModel);
            if (tokenTracker != null) {
                tokenTracker.addAndGet(response.getTotalTokens());
            }

            log.debug("Interview questions AI response (first 300 chars): {}",
                    response != null && response.getContent() != null
                            ? response.getContent().substring(0, Math.min(300, response.getContent().length()))
                            : "null");

            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());
            InterviewPrep result = parseYamlToInterviewPrep(cleanedResponse);

            if (result == null || result.getQuestions() == null || result.getQuestions().isEmpty()) {
                log.warn("Interview questions parsing returned empty result. Raw response: {}",
                        cleanedResponse.substring(0, Math.min(500, cleanedResponse.length())));
                return InterviewPrep.builder().questions(new ArrayList<>()).tokensUsed(response.getTotalTokens())
                        .build();
            }
            result.setTokensUsed(response.getTotalTokens());
            return result;
        } catch (Exception e) {
            log.error("Error generating interview questions: {}", e.getMessage(), e);
            return InterviewPrep.builder().questions(new ArrayList<>()).tokensUsed(0).build();
        }
    }

    private Roadmap generateRoadmap(CvTailorRequest request, AtomicInteger tokenTracker) {
        try {
            String skills = extractSkills(request.getResumeData());
            String promptBody = ROADMAP_PROMPT
                    .replace("{JOB_DESCRIPTION}",
                            request.getJobDescription() != null ? request.getJobDescription() : "")
                    .replace("{CV_SKILLS}", skills);

            log.debug("Roadmap prompt length: {}", promptBody.length());
            AiModelResponse response = aiService.callModel(promptBody, defaultProvider, defaultModel);
            if (tokenTracker != null) {
                tokenTracker.addAndGet(response.getTotalTokens());
            }

            log.debug("Roadmap AI response (first 300 chars): {}",
                    response != null && response.getContent() != null
                            ? response.getContent().substring(0, Math.min(300, response.getContent().length()))
                            : "null");

            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());
            Roadmap result = parseYamlToRoadmap(cleanedResponse);

            if (result == null || result.getSteps() == null || result.getSteps().isEmpty()) {
                log.warn("Roadmap parsing returned empty result. Raw response: {}",
                        cleanedResponse.substring(0, Math.min(500, cleanedResponse.length())));
                return Roadmap.builder().skillGaps(new ArrayList<>()).steps(new ArrayList<>())
                        .tokensUsed(response.getTotalTokens()).build();
            }
            result.setTokensUsed(response.getTotalTokens());
            return result;
        } catch (Exception e) {
            log.error("Error generating roadmap: {}", e.getMessage(), e);
            return Roadmap.builder().skillGaps(new ArrayList<>()).steps(new ArrayList<>()).tokensUsed(0).build();
        }
    }

    private String generateCoverLetter(CvTailorRequest request, AtomicInteger tokenTracker) {
        try {
            ResumeData.PersonalInfo info = request.getResumeData().getPersonalInfo();
            String skills = extractSkills(request.getResumeData());
            String experience = buildExperienceSummary(request.getResumeData());

            // The Cover Letter prompt needs to format correctly with .replace
            String companyName = request.getCompanyName() != null && !request.getCompanyName().isBlank()
                    ? request.getCompanyName()
                    : "the company";
            String jobTitle = request.getJobTitle() != null && !request.getJobTitle().isBlank()
                    ? request.getJobTitle()
                    : "the position";

            String promptBody = COVER_LETTER_PROMPT
                    .replace("{JOB_DESCRIPTION}",
                            request.getJobDescription() != null ? request.getJobDescription() : "unspecified")
                    .replace("{JOB_TITLE}", jobTitle)
                    .replace("{COMPANY_NAME}", companyName)
                    .replace("{CV_NAME}", info.getFullName() != null ? info.getFullName() : "Candidate")
                    .replace("{CV_EMAIL}", info.getEmail() != null ? info.getEmail() : "")
                    .replace("{CV_PHONE}", info.getPhone() != null ? info.getPhone() : "")
                    .replace("{CV_SUMMARY}", info.getSummary() != null ? info.getSummary() : "")
                    .replace("{CV_SKILLS}", skills)
                    .replace("{CV_EXPERIENCE}", experience)
                    .replace("{JOB_PAIN_POINTS}", request.getJobDescription() != null ? request.getJobDescription()
                            : "Standard role requirements");

            log.debug("Cover letter prompt length: {}", promptBody.length());
            AiModelResponse response = aiService.callModel(promptBody, defaultProvider, defaultModel);
            if (tokenTracker != null) {
                tokenTracker.addAndGet(response.getTotalTokens());
            }

            log.debug("Cover letter AI response (first 300 chars): {}",
                    response != null && response.getContent() != null
                            ? response.getContent().substring(0, Math.min(300, response.getContent().length()))
                            : "null");

            String body = jsonSanitizer.cleanAiResponse(response.getContent());
            if (body == null || body.isBlank()) {
                log.warn("Cover letter generation returned empty result");
                return "";
            }
            body = stripBracketPlaceholders(body);
            return buildCoverLetterWithHeader(info, body);
        } catch (Exception e) {
            log.error("Error generating cover letter: {}", e.getMessage(), e);
            return "";
        }
    }

    private String buildCoverLetterWithHeader(ResumeData.PersonalInfo info, String body) {
        String header = formatCoverLetterHeader(info);
        String trimmedBody = body.strip();
        if (trimmedBody.isEmpty()) {
            return header;
        }
        return header + "\n\n" + trimmedBody;
    }

    private String formatCoverLetterHeader(ResumeData.PersonalInfo info) {
        String name = info.getFullName() != null && !info.getFullName().isBlank()
                ? info.getFullName()
                : "Candidate";

        List<String> contactParts = new ArrayList<>();
        if (info.getEmail() != null && !info.getEmail().isBlank()) {
            contactParts.add(info.getEmail());
        }
        if (info.getPhone() != null && !info.getPhone().isBlank()) {
            contactParts.add(info.getPhone());
        }

        String location = Stream.of(info.getAddress(), info.getCity(), info.getCountry())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
        if (!location.isBlank()) {
            contactParts.add(location);
        }

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));

        StringBuilder header = new StringBuilder(name);
        if (!contactParts.isEmpty()) {
            header.append("\n").append(String.join(" | ", contactParts));
        }
        header.append("\n\n").append(date);
        return header.toString();
    }

    private String stripBracketPlaceholders(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String cleaned = BRACKET_PLACEHOLDER.matcher(text).replaceAll("").trim();
        return cleaned.replaceAll("(?m)^\\s*\\n", "").replaceAll("\n{3,}", "\n\n");
    }

    private String buildExperienceSummary(ResumeData resumeData) {
        if (resumeData.getSections() == null)
            return "";

        StringBuilder sb = new StringBuilder();
        resumeData.getSections().stream()
                .filter(s -> "experience".equalsIgnoreCase(s.getType()))
                .flatMap(s -> s.getItems().stream())
                .limit(3) // Top 3 experiences
                .forEach(item -> {
                    sb.append("- ").append(item.getSubtitle() != null ? item.getSubtitle() : "")
                            .append(" at ").append(item.getTitle() != null ? item.getTitle() : "")
                            .append(" (").append(item.getDate() != null ? item.getDate() : "").append(")\n");
                });
        return sb.toString();
    }

    // ==================== Helper Methods ====================

    private String convertToYaml(Object obj) {
        try {
            YAMLMapper yamlMapper = new YAMLMapper();
            return yamlMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error converting to YAML: {}", e.getMessage());
            return "";
        }
    }

    // Local cleaning methods removed - using JsonSanitizer

    private ResumeData parseYamlToResumeData(String yaml) {
        try {
            return YAML_MAPPER.readValue(yaml, ResumeData.class);
        } catch (Exception e) {
            log.warn("First YAML parse failed for ResumeData, attempting sanitization: {}", e.getMessage());
            try {
                String sanitized = sanitizeYamlValues(yaml);
                return YAML_MAPPER.readValue(sanitized, ResumeData.class);
            } catch (Exception e2) {
                log.error("Error parsing YAML to ResumeData after sanitization: {}", e2.getMessage());
                return null;
            }
        }
    }

    private AtsScore parseYamlToAtsScore(String yaml) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = YAML_MAPPER.readTree(yaml);
            validateScoreFields(root);

            return AtsScore.builder()
                    .overall(root.path("overall").asInt(0))
                    .keywordMatch(root.path("keywordMatch").asInt(0))
                    .skillsMatch(root.path("skillsMatch").asInt(0))
                    .missingKeywords(extractStringList(root.path("missingKeywords")))
                    .suggestions(extractStringList(root.path("suggestions")))
                    .build();
        } catch (Exception e) {
            log.warn("Invalid ATS score response; attempting local sanitization");
            try {
                String sanitized = sanitizeYamlValues(yaml);
                com.fasterxml.jackson.databind.JsonNode root = YAML_MAPPER.readTree(sanitized);
                validateScoreFields(root);
                return AtsScore.builder()
                        .overall(root.path("overall").asInt(0))
                        .keywordMatch(root.path("keywordMatch").asInt(0))
                        .skillsMatch(root.path("skillsMatch").asInt(0))
                        .missingKeywords(extractStringList(root.path("missingKeywords")))
                        .suggestions(extractStringList(root.path("suggestions")))
                        .build();
            } catch (Exception e2) {
                log.warn("ATS score response remains invalid after sanitization");
                return AtsScore.builder()
                        .overall(-1)
                        .suggestions(List.of("Error parsing AI response"))
                        .build();
            }
        }
    }

    private void validateScoreFields(com.fasterxml.jackson.databind.JsonNode root) {
        if (root == null || !root.isObject()) throw new IllegalArgumentException("Invalid ATS score object");
        for (String field : List.of("overall", "keywordMatch", "skillsMatch")) {
            var value = root.path(field);
            if (!value.isIntegralNumber() || value.asInt() < 0 || value.asInt() > 100) {
                throw new IllegalArgumentException("Invalid ATS score field");
            }
        }
    }

    private List<String> extractStringList(com.fasterxml.jackson.databind.JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode item : node) {
                if (item.isTextual()) {
                    list.add(item.asText());
                } else if (item.isObject()) {
                    // Flatten complex objects to string string representation or extract a "value"
                    // if reasonable
                    // For suggestions, just toString() is better than crashing
                    try {
                        // Attempt to find common text fields if it's an object (e.g. "suggestion":
                        // "text")
                        if (item.has("suggestion"))
                            list.add(item.get("suggestion").asText());
                        else if (item.has("text"))
                            list.add(item.get("text").asText());
                        else if (item.has("value"))
                            list.add(item.get("value").asText());
                        else
                            list.add(item.toString());
                    } catch (Exception e) {
                        list.add(item.toString());
                    }
                } else {
                    list.add(item.asText());
                }
            }
        } else if (node.isTextual()) {
            list.add(node.asText());
        }
        return list;
    }

    private InterviewPrep parseYamlToInterviewPrep(String yaml) {
        try {
            return YAML_MAPPER.readValue(yaml, InterviewPrep.class);
        } catch (Exception e) {
            log.warn("First YAML parse failed for InterviewPrep, attempting sanitization: {}", e.getMessage());
            try {
                String sanitized = sanitizeYamlValues(yaml);
                return YAML_MAPPER.readValue(sanitized, InterviewPrep.class);
            } catch (Exception e2) {
                log.error("Error parsing YAML to InterviewPrep after sanitization: {}", e2.getMessage());
                return null;
            }
        }
    }

    private Roadmap parseYamlToRoadmap(String yaml) {
        try {
            return YAML_MAPPER.readValue(yaml, Roadmap.class);
        } catch (Exception e) {
            log.warn("First YAML parse failed for Roadmap, attempting sanitization: {}", e.getMessage());
            try {
                String sanitized = sanitizeYamlValues(yaml);
                return YAML_MAPPER.readValue(sanitized, Roadmap.class);
            } catch (Exception e2) {
                log.error("Error parsing YAML to Roadmap after sanitization: {}", e2.getMessage());
                return null;
            }
        }
    }

    /**
     * Sanitizes YAML output from the AI model by quoting unquoted string values that contain colons.
     * This prevents errors like "expected a single document in the stream but found another document"
     * or "mapping values are not allowed here" when the AI outputs:
     * - step: Master OAuth 2.0: standard protocol
     */
    private String sanitizeYamlValues(String yaml) {
        if (yaml == null || yaml.isBlank()) return yaml;

        // Step 1: Strip markdown block ticks if present
        String sanitized = yaml.replaceAll("(?m)^```(yaml)?\\s*$", "");

        // Step 2: Strip markdown bold markers (** or *) which YAML interprets as alias syntax
        // The AI frequently outputs: - step: **Phase 1: Core Backend**
        sanitized = sanitized.replaceAll("\\*+", "");

        // Step 3: Quote plain list items that contain colons, escaping internal quotes
        // Example: "  - Address Experience Gap: The \"minimum\"..." -> "  - \"Address Experience Gap: The \\\"minimum\\\"...\""
        Matcher listMatcher = Pattern.compile("(?m)^(\\s*-\\s+)(?![\\p{L}\\p{N}_]+:\\s+)(?![\"'])(.*?:.*)(?<![\"'])$").matcher(sanitized);
        StringBuffer listSb = new StringBuffer();
        while (listMatcher.find()) {
            String prefix = listMatcher.group(1);
            String content = listMatcher.group(2).replace("\"", "\\\"");
            listMatcher.appendReplacement(listSb, Matcher.quoteReplacement(prefix + "\"" + content + "\""));
        }
        listMatcher.appendTail(listSb);
        sanitized = listSb.toString();

        // Step 4: Quote unquoted map values that contain colons, escaping internal quotes
        // Example: "- step: Master OAuth 2.0: protocol \"A\"" -> "- step: \"Master OAuth 2.0: protocol \\\"A\\\"\""
        Matcher mapMatcher = Pattern.compile("(?m)^(\\s*-?\\s*[\\p{L}\\p{N}_]+:\\s+)(?![\"'])(.*?:.*)(?<![\"'])$").matcher(sanitized);
        StringBuffer mapSb = new StringBuffer();
        while (mapMatcher.find()) {
            String prefix = mapMatcher.group(1);
            String content = mapMatcher.group(2).replace("\"", "\\\"");
            mapMatcher.appendReplacement(mapSb, Matcher.quoteReplacement(prefix + "\"" + content + "\""));
        }
        mapMatcher.appendTail(mapSb);
        sanitized = mapSb.toString();

        return sanitized;
    }

    private String extractFullText(ResumeData resumeData) {
        StringBuilder sb = new StringBuilder();
        // 1. Personal Info
        if (resumeData.getPersonalInfo() != null) {
            appendIfNotNull(sb, resumeData.getPersonalInfo().getJobTitle());
            appendIfNotNull(sb, resumeData.getPersonalInfo().getSummary());
        }

        // 2. All Sections (Experience, Education, Projects, Skills)
        if (resumeData.getSections() != null) {
            for (ResumeData.ResumeSection section : resumeData.getSections()) {
                if (section.getItems() != null) {
                    for (ResumeData.SectionItem item : section.getItems()) {
                        appendIfNotNull(sb, item.getTitle());
                        appendIfNotNull(sb, item.getSubtitle());
                        appendIfNotNull(sb, item.getDescription());

                        // Tags (critical for skills)
                        if (item.getTags() != null) {
                            for (String tag : item.getTags()) {
                                appendIfNotNull(sb, tag);
                            }
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    private void appendIfNotNull(StringBuilder sb, String text) {
        if (text != null && !text.isBlank()) {
            sb.append(text).append(" ");
        }
    }

    private String extractSkills(ResumeData resumeData) {
        if (resumeData.getSections() == null)
            return "";

        return resumeData.getSections().stream()
                .filter(s -> "skills".equalsIgnoreCase(s.getType()))
                .flatMap(s -> s.getItems().stream())
                .flatMap(item -> item.getTags() != null ? item.getTags().stream()
                        : (item.getTitle() != null ? List.of(item.getTitle()).stream() : List.<String>of().stream()))
                .distinct()
                .limit(15)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private List<String> validateAlignment(ResumeData original, ResumeData tailored) {
        List<String> warnings = new ArrayList<>();

        // 1. Validate Experience (Companies)
        Set<String> originalCompanies = extractOrganizationsFromSection(original, "experience");
        Set<String> tailoredCompanies = extractOrganizationsFromSection(tailored, "experience");

        for (String company : tailoredCompanies) {
            if (!originalCompanies.contains(company)) {
                warnings.add(
                        "Possible Hallucination: Company '" + company + "' found in tailored CV was not in original.");
            }
        }

        // 2. Validate Education (Schools/Degrees)
        Set<String> originalSchools = extractOrganizationsFromSection(original, "education");
        Set<String> tailoredSchools = extractOrganizationsFromSection(tailored, "education");

        for (String school : tailoredSchools) {
            if (!originalSchools.contains(school)) {
                warnings.add("Possible Hallucination: Education institution '" + school
                        + "' found in tailored CV was not in original.");
            }
        }

        return warnings;
    }

    private Set<String> extractOrganizationsFromSection(ResumeData data, String sectionType) {
        if (data == null || data.getSections() == null) {
            return new HashSet<>();
        }
        return data.getSections().stream()
                .filter(s -> sectionType.equalsIgnoreCase(s.getType())) // Filter by type (e.g., "experience")
                .filter(s -> s.getItems() != null)
                .flatMap(s -> s.getItems().stream())
                .map(item -> normalizeValue(item.getSubtitle()))
                .filter(title -> !title.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Wraps a Supplier to propagate MDC context into async threads.
     * Ensures requestId appears in log lines from CompletableFuture worker threads.
     */
    private <T> Supplier<T> withMdc(Map<String, String> mdcContext, Supplier<T> supplier) {
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (mdcContext != null) {
                MDC.setContextMap(mdcContext);
            }
            try {
                return supplier.get();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
