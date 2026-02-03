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
        Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);
        
        // Use Jsoup clean but we want text. 
        // Better: replace <br> and <p> with newlines before getting text
        element.select("br").append("\\n");
        element.select("p").prepend("\\n\\n");
        element.select("li").prepend("\\n- ");
        
        return Jsoup.clean(element.html(), "", Safelist.none(), outputSettings).trim();
    }
}
