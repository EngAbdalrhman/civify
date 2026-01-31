package com.app.civify.controller.v1.admin;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.dto.logs.CvOperationLogDto;
import com.app.civify.usecase.admin.AdminCvLogUseCase;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiV1Controller
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Admin CV Log Controller", description = "Admin CV Operation Logs Management")
public class AdminCvLogController {

    private final AdminCvLogUseCase adminCvLogUseCase;

    @Operation(summary = "Get CV operation logs with filtering")
    @PostMapping("/cv-logs")
    @CivifyAuthorization(permission = "LOGS.CV")
    public ResponseEntity<PagedResponse<CvOperationLogDto>> getCvLogs(@RequestBody InputDataModel inputData) {
        return ResponseEntity.ok(adminCvLogUseCase.getCvLogs(inputData));
    }
}
