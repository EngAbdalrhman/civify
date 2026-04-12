package com.app.civify.controller.v1.resume;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.linkedin.LinkedInParseRequest;
import com.app.civify.dto.linkedin.LinkedInParseResponse;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.external.LinkedInProfileParserService;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiV1Controller
@RequestMapping("/linkedin")
@Tag(name = "LinkedIn Import", description = "Endpoints for importing and parsing LinkedIn profile content")
@RequiredArgsConstructor
public class LinkedInController {

    private final LinkedInProfileParserService parserService;

    @Operation(
            summary = "Parse LinkedIn Profile",
            description = "Parses pasted text from a LinkedIn profile into structured resume data.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "CV.PARSE")
    @PostMapping("/parse-profile")
    public ResponseEntity<CommonResponse<LinkedInParseResponse>> parseProfile(
            @Valid @RequestBody LinkedInParseRequest request) {
        
        LinkedInParseResponse response = parserService.parseProfile(request.getProfileText());
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(CommonResponse.success("Profile parsed successfully", response));
        } else {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error(response.getErrorMessage()));
        }
    }
}
