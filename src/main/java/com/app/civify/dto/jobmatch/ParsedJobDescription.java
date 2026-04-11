package com.app.civify.dto.jobmatch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedJobDescription {
    private String jobTitle;
    private String companyName;
    private String seniority;        // junior/mid/senior/lead/staff
    private String remotePolicy;     // remote/hybrid/onsite
    private String location;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private List<String> responsibilities;
    private int minExperienceYears;
    private String domain;           // e.g. "AI/ML", "Backend", "Frontend"
}
