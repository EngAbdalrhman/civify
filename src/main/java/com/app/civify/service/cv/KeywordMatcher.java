package com.app.civify.service.cv;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for deterministic keyword matching between CV and Job Description.
 * Used to calculate a base ATS score and identify missing keywords.
 */
@Component
public class KeywordMatcher {

    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "when",
            "at", "by", "for", "from", "in", "into", "of", "off", "on", "onto",
            "to", "with", "within", "about", "above", "across", "after", "against",
            "along", "amid", "among", "anti", "around", "as", "before", "behind",
            "below", "beneath", "beside", "besides", "between", "beyond", "concerning",
            "considering", "despite", "down", "during", "except", "excepting",
            "excluding", "following", "inside", "like", "minus", "near", "next",
            "opposite", "outside", "over", "past", "per", "plus", "regarding",
            "round", "save", "since", "than", "through", "till", "toward",
            "under", "underneath", "unlike", "until", "up", "upon", "versus", "via",
            "is", "am", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "will", "would", "shall", "should", "can", "could",
            "may", "might", "must", "i", "you", "he", "she", "it", "we", "they",
            "me", "him", "her", "us", "them", "my", "your", "his", "its", "our", "their",
            "mine", "yours", "hers", "ours", "theirs", "this", "that", "these", "those",
            "job", "description", "requirements", "responsibilities", "qualifications",
            "experience", "skills", "role", "work", "team", "company", "candidate"
    );

    /**
     * Match keywords between resume and job description.
     * Uses deterministic word-boundary matching for multi-word phrases.
     *
     * @param resumeText The full text of the resume.
     * @param jobDescription The full text of the job description.
     * @return A MatchResult containing the score and missing keywords.
     */
    public MatchResult match(String resumeText, String jobDescription) {
        // 1. Extract potential keywords from JD (n-grams up to 3 words)
        // For simplicity and speed in this deterministic matcher, we still rely on
        // significant tokens but now we check for their presence more accurately.
        // IMPROVEMENT: Ideally we would have a phrase dictionary.
        // For now, we extract single tokens from JD as "keywords" but we match them
        // using word boundaries in CV to avoid substring false positives (e.g. "arc" in "architect").
        Set<String> jdKeywords = extractKeywords(jobDescription);
        
        if (jdKeywords.isEmpty()) {
            return new MatchResult(0, Collections.emptyList());
        }

        // 2. Normalize resume text once
        String normalizedResume = resumeText.toLowerCase();

        // 3. Check for specific keywords using word boundaries
        Set<String> matchedKeywords = new HashSet<>();
        for (String keyword : jdKeywords) {
            if (containsKeyword(normalizedResume, keyword)) {
                matchedKeywords.add(keyword);
            }
        }

        // Calculate score
        double score = (double) matchedKeywords.size() / jdKeywords.size() * 100.0;
        
        // Find missing keywords
        List<String> missingKeywords = jdKeywords.stream()
                .filter(k -> !matchedKeywords.contains(k))
                .sorted()
                .limit(20)
                .collect(Collectors.toList());

        return new MatchResult((int) Math.round(score), missingKeywords);
    }
    
    public boolean containsKeyword(String text, String keyword) {
        try {
            Pattern pattern = PATTERN_CACHE.computeIfAbsent(keyword,
                k -> Pattern.compile("\\b" + Pattern.quote(k) + "\\b"));
            return pattern.matcher(text).find();
        } catch (Exception e) {
            return text.contains(keyword);
        }
    }

    /**
     * Extract significant keywords from text.
     * Tokenizes, lowercases, removes punctuation and stop words.
     */
    public Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> keywords = new HashSet<>();
        // Split by non-word characters
        String[] tokens = text.toLowerCase().split("[^a-zA-Z0-9]+");

        for (String token : tokens) {
            if (token.length() > 2 && !STOP_WORDS.contains(token)) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    public record MatchResult(int score, List<String> missingKeywords) {}
}
