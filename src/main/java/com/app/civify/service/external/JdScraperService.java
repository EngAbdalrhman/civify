package com.app.civify.service.external;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service to scrape Job Descriptions from URLs.
 * Supports generic scraping with heuristics for common job boards (LinkedIn, Indeed, etc.).
 */
@Service
@Slf4j
public class JdScraperService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    /**
     * Scrapes the content from the given URL.
     * Tries to find the main content container to avoid navbars/footers.
     */
    public String scrape(String url) {
        log.info("Scraping Job Description from URL: {}", url);
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();

            // Heuristics to find the main job description body
            // LinkedIn: .description__text, .job-details
            // Typically generic: main, article, or div with class containing "job" or "description"
            Element mainContent = findMainContent(doc);

            if (mainContent != null) {
                // Clean HTML to text, preserving some structure with newlines
                return cleanHtmlToText(mainContent);
            } else {
                // Fallback: Body text
                return cleanHtmlToText(doc.body());
            }

        } catch (IOException e) {
            log.error("Failed to scrape URL {}: {}", url, e.getMessage());
            throw new RuntimeException("Failed to scrape Job Description. Please copy/paste the text manually.", e);
        }
    }

    private Element findMainContent(Document doc) {
        // Try known selectors for common sites
        // This is a simplified list.
        String[] selectors = {
            ".description__text", // LinkedIn
            "#jobDescriptionText", // Indeed
            ".job-description", 
            "article",
            "main"
        };
        
        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && el.text().length() > 100) {
                return el;
            }
        }
        
        return null; // Fallback
    }

    private String cleanHtmlToText(Element element) {
        if (element == null) return "";
        
        // Preserve line breaks for standard block elements
        // Clone to avoid modifying the original document used elsewhere if applicable
        Element clone = element.clone();
        
        clone.select("br").append("\\n");
        clone.select("p").prepend("\\n\\n");
        clone.select("li").prepend("\\n- ");
        clone.select("div").prepend("\\n");
        
        // Get the text from the modified HTML
        // Using clone.text() would just get text of nodes, missing the explicitly added "\n" text nodes correctly if mixed.
        // Actually, we inserted text nodes containing literal "\n".
        
        String safeHtml = Jsoup.clean(clone.html(), Safelist.none());
        
        // Unescape entities (like &amp;)
        String text = Jsoup.parse(safeHtml).text();
        
        // Replace literal \n markers we added (which are now just text) with actual newlines
        text = text.replace("\\n", "\n");
        
        // Remove common noise
        text = text.replaceAll("(?i)Show more\\s*Show less", "");
        
        // Normalize whitespace:
        // 1. Replace multiple spaces with single space
        // 2. Reduce multiple newlines to max 2
        
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                sb.append(trimmed).append("\n");
            } else {
                 // Keep empty lines but limit consecutive ones?
                 // Simple approach: just rebuild
                 sb.append("\n");
            }
        }
        
        String result = sb.toString();
        // Collapse 3+ newlines to 2
        return result.replaceAll("\n{3,}", "\n\n").trim();
    }
}
