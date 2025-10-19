package com.app.civify.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@ApiV1Controller
@RequestMapping("/cvs")
@Tag(name = "CV Management", description = "CV upload and tailoring endpoints")
public class CvController {

    @Operation(
            summary = "Upload CV file",
            description = "Uploads a CV file for processing and storage.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CV uploaded successfully",
                content = @Content(mediaType = "text/plain")),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestPart("file") MultipartFile file) {
        // stub
        return ResponseEntity.ok("uploaded");
    }

    @Operation(
            summary = "Tailor CV for job",
            description = "Tailors an existing CV to match a specific job description using AI.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CV tailored successfully",
                content = @Content(mediaType = "text/plain")),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "CV not found",
                content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/{id}/tailor")
    public ResponseEntity<String> tailor(@PathVariable Long id, @RequestBody String jobDescription) {
        // stub
        return ResponseEntity.ok("tailored");
    }
}
