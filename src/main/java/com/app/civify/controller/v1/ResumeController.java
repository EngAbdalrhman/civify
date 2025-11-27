package com.app.civify.controller.v1;

import com.app.civify.dto.resume.CreateResumeRequest;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.dto.resume.ResumeDto;
import com.app.civify.dto.resume.UpdateResumeRequest;
import com.app.civify.entity.User;
import com.app.civify.service.ResumeService;
import com.app.civify.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ApiV1Controller
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<ResumeDto>> getUserResumes(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(resumeService.getAllResumesForUser(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeDto> getResume(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(resumeService.getResumeById(id, user.getId()));
    }

    @PostMapping
    public ResponseEntity<ResumeDto> createResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateResumeRequest request) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(resumeService.createResume(user.getId(), request.getTitle(), request.getData()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeDto> updateResume(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateResumeRequest request) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(resumeService.updateResume(id, user.getId(), request.getTitle(), request.getData()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser();
        resumeService.deleteResume(id, user.getId());
        return ResponseEntity.noContent().build();
    }

}
