package com.app.civify.usecase.admin;

import com.app.civify.dto.logs.CvOperationLogDto;
import com.app.civify.dto.user.UserSummaryDto;
import com.app.civify.entity.logs.CvOperationLog;
import com.app.civify.service.cvlog.CvOperationLogService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminCvLogUseCaseImpl implements AdminCvLogUseCase {

    private final CvOperationLogService cvOperationLogService;

    @Override
    public PagedResponse<CvOperationLogDto> getCvLogs(InputDataModel inputData) {
        PagedResponse<CvOperationLog> entities = cvOperationLogService.getLogs(inputData);
        
        List<CvOperationLogDto> dtos = entities.getContent().stream()
                .map(this::mapToDto)
                .toList();

        PagedResponse<CvOperationLogDto> response = new PagedResponse<>();
        response.setContent(dtos);
        response.setTotalElements(entities.getTotalElements());
        response.setTotalPages(entities.getTotalPages());
        response.setPage(entities.getPage());
        response.setSize(entities.getSize());
        
        return response;
    }

    private CvOperationLogDto mapToDto(CvOperationLog log) {
        UserSummaryDto userDto = null;
        if (log.getUser() != null) {
            userDto = UserSummaryDto.builder()
                    .id(log.getUser().getId())
                    .email(log.getUser().getEmail())
                    .name(log.getUser().getName())
                    .build();
        }

        return CvOperationLogDto.builder()
                .id(log.getId())
                .user(userDto)
                .action(log.getAction().name())
                .source(log.getSource())
                .endpoint(log.getEndpoint())
                .requestId(log.getRequestId())
                .extractedPii(log.getExtractedPii())
                .processingTimeMs(log.getProcessingTimeMs())
                .success(log.getSuccess())
                .errorMessage(log.getErrorMessage())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .requestMetadata(log.getRequestMetadata())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
