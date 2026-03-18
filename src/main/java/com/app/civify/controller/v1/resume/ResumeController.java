package com.app.civify.controller.v1.resume;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.resume.CreateResumeRequest;
import com.app.civify.dto.resume.ResumeDto;
import com.app.civify.dto.resume.ResumeListDto;
import com.app.civify.dto.resume.UpdateResumeRequest;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.ResumeService;
import com.app.civify.util.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ApiV1Controller
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<ResumeListDto>>> getUserResumes(@AuthenticationPrincipal UserPrincipal userDetails) {
        return ResponseEntity.ok(CommonResponse.success(resumeService.getAllResumesForUser(userDetails.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ResumeDto>> getResume(@PathVariable String id,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        return ResponseEntity.ok(CommonResponse.success(resumeService.getResumeById(id, userDetails.getId())));
    }

    @PostMapping
    public ResponseEntity<CommonResponse<ResumeDto>> createResume(
            @AuthenticationPrincipal UserPrincipal userDetails,
            @RequestBody CreateResumeRequest request) {
        return ResponseEntity
                .ok(CommonResponse.success(resumeService.createResume(userDetails.getId(), request.getTitle(), request.getData())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<ResumeDto>> updateResume(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal userDetails,
            @RequestBody UpdateResumeRequest request) {
        return ResponseEntity
                .ok(CommonResponse.success(resumeService.updateResume(id, userDetails.getId(), request.getTitle(), request.getData())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> deleteResume(@PathVariable String id,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        resumeService.deleteResume(id, userDetails.getId());
        return ResponseEntity.ok(CommonResponse.success("Resume deleted successfully", null));
    }

}
