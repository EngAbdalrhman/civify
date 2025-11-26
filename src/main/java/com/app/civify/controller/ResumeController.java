package com.app.civify.controller;

import com.app.civify.dto.resume.ResumeData;
import com.app.civify.dto.resume.ResumeDto;
import com.app.civify.entity.User;
import com.app.civify.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public ResponseEntity<List<ResumeDto>> getUserResumes(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(resumeService.getAllResumesForUser(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeDto> getResume(@PathVariable String id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(resumeService.getResumeById(id, user.getId()));
    }

    @PostMapping
    public ResponseEntity<ResumeDto> createResume(
            @AuthenticationPrincipal User user,
            @RequestBody CreateResumeRequest request) {
        return ResponseEntity.ok(resumeService.createResume(user.getId(), request.getTitle(), request.getData()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeDto> updateResume(
            @PathVariable String id,
            @AuthenticationPrincipal User user,
            @RequestBody UpdateResumeRequest request) {
        return ResponseEntity.ok(resumeService.updateResume(id, user.getId(), request.getTitle(), request.getData()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable String id, @AuthenticationPrincipal User user) {
        resumeService.deleteResume(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @lombok.Data
    public static class CreateResumeRequest {
        private String title;
        private ResumeData data;
    }

    @lombok.Data
    public static class UpdateResumeRequest {
        private String title;
        private ResumeData data;
    }
}
