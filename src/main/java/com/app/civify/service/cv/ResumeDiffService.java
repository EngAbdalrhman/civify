package com.app.civify.service.cv;

import com.app.civify.dto.resume.ResumeData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to calculate structured differences between two ResumeData objects.
 * Ported concept from Resume-Matcher's refiner.py.
 */
@Service
public class ResumeDiffService {

    public List<String> calculateDiff(ResumeData original, ResumeData tailored) {
        List<String> changes = new ArrayList<>();

        if (original == null || tailored == null) {
            return changes;
        }

        // 1. Compare Personal Info / Summary
        comparePersonalInfo(original, tailored, changes);

        // 2. Compare Skills
        compareSkills(original, tailored, changes);

        // 3. Compare Experience Entries (High-level: Added/Removed/Modifications)
        compareSectionItems(original, tailored, "experience", "Experience", changes);
        
        // 4. Compare Projects
        compareSectionItems(original, tailored, "projects", "Project", changes);

        return changes;
    }

    private void comparePersonalInfo(ResumeData original, ResumeData tailored, List<String> changes) {
        if (original.getPersonalInfo() == null || tailored.getPersonalInfo() == null) return;

        String origSummary = original.getPersonalInfo().getSummary();
        String newSummary = tailored.getPersonalInfo().getSummary();

        if (origSummary != null && !origSummary.equals(newSummary)) {
            changes.add("Optimized Professional Summary");
        }
        
        if (tailored.getPersonalInfo().getJobTitle() != null && 
           !tailored.getPersonalInfo().getJobTitle().equals(original.getPersonalInfo().getJobTitle())) {
             changes.add("Updated Headline to match target role");
        }
    }

    private void compareSkills(ResumeData original, ResumeData tailored, List<String> changes) {
        Set<String> origSkills = extractTags(original, "skills");
        Set<String> newSkills = extractTags(tailored, "skills");
        
        if (origSkills.equals(newSkills)) return;

        Set<String> added = new HashSet<>(newSkills);
        added.removeAll(origSkills);

        Set<String> removed = new HashSet<>(origSkills);
        removed.removeAll(newSkills);

        if (!added.isEmpty()) {
            // Limit output if too many
            if (added.size() > 5) {
               changes.add("Added " + added.size() + " new skills aligned with JD");
            } else {
               changes.add("Added skills: " + String.join(", ", added));
            }
        }
        
        // We generally don't highlight removals unless asked, as users care more about additions for ATS.
    }

    private void compareSectionItems(ResumeData original, ResumeData tailored, String sectionType, String label, List<String> changes) {
         // Simplified logic: compare counts and check for modifications to descriptions
         // For a robust diff, we would match by ID or Company Name.
         // Here we assume order roughly maintained or matching by Title.
         
         // For now, let's just track high-level changes
         List<ResumeData.SectionItem> origItems = getItems(original, sectionType);
         List<ResumeData.SectionItem> newItems = getItems(tailored, sectionType);
         
         if (origItems.size() != newItems.size()) {
             changes.add("Restructured " + label + " section (items changed from " + origItems.size() + " to " + newItems.size() + ")");
             return;
         }
         
         // Check modifications
         for (int i = 0; i < origItems.size(); i++) {
             ResumeData.SectionItem o = origItems.get(i);
             // Find matching item in newItems by title/subtitle
             ResumeData.SectionItem n = findMatchingItem(newItems, o);
             
             if (n != null) {
                 if (isDescriptionChanged(o.getDescription(), n.getDescription())) {
                     changes.add("Refined description for " + label + ": " + (o.getTitle() != null ? o.getTitle() : "Item " + (i+1)));
                 }
             }
         }
    }
    
    private ResumeData.SectionItem findMatchingItem(List<ResumeData.SectionItem> items, ResumeData.SectionItem target) {
        return items.stream()
            .filter(i -> (i.getTitle() != null && i.getTitle().equalsIgnoreCase(target.getTitle())))
            .findFirst()
            .orElse(null);
    }
    
    private boolean isDescriptionChanged(String d1, String d2) {
        if (d1 == null && d2 == null) return false;
        if (d1 == null || d2 == null) return true;
        // Simple equality check. Could be enhanced with fuzzy match.
        return !d1.trim().equals(d2.trim());
    }

    private Set<String> extractTags(ResumeData data, String sectionType) {
        if (data.getSections() == null) return new HashSet<>();
        return data.getSections().stream()
                .filter(s -> sectionType.equalsIgnoreCase(s.getType()))
                .flatMap(s -> s.getItems().stream())
                .flatMap(item -> item.getTags() != null ? item.getTags().stream() : 
                     (item.getTitle() != null ? List.of(item.getTitle()).stream() : List.<String>of().stream()))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
    
    private List<ResumeData.SectionItem> getItems(ResumeData data, String sectionType) {
        if (data.getSections() == null) return new ArrayList<>();
        return data.getSections().stream()
                .filter(s -> sectionType.equalsIgnoreCase(s.getType()))
                .flatMap(s -> s.getItems().stream())
                .collect(Collectors.toList());
    }
}
