package com.app.civify.service.logs;

import com.app.civify.dto.ApiKeyUsageLogDTO;
import com.app.civify.entity.User;
import com.app.civify.entity.logs.ApiKeyUsageLog;
import com.app.civify.repository.ApiKeyUsageLogRepository;
import com.app.civify.util.filter.FilterCriteria;
import com.app.civify.util.filter.FilterOperation;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.filter.SortCriteria;
import com.app.civify.util.filter.SortDirection;
import com.app.civify.util.persistence.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for API Key Usage Logs
 * Extends CommonService to use generic filtering
 */
@Slf4j
@Service
public class ApiUsageLogServiceImpl extends CommonService implements ApiUsageLogService {

    private final ApiKeyUsageLogRepository apiKeyUsageLogRepository;

    public ApiUsageLogServiceImpl(ApiKeyUsageLogRepository apiKeyUsageLogRepository) {
        this.apiKeyUsageLogRepository = apiKeyUsageLogRepository;
    }

    @Override
    public PagedResponse<ApiKeyUsageLogDTO> filterMyLogs(User user, InputDataModel inputData) {
        // Initialize filters if null
        if (inputData.getFilters() == null) {
            inputData.setFilters(new ArrayList<>());
        }

        // Add mandatory user filter
        inputData.getFilters().add(FilterCriteria.builder()
                .fieldName("user.id")
                .operation(FilterOperation.EQUAL)
                .value(user.getId())
                .build());

        // Add current month filter
        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        inputData.getFilters().add(FilterCriteria.builder()
                .fieldName("timestamp")
                .operation(FilterOperation.GREATER_THAN_EQUAL)
                .value(startOfMonth.toString())
                .build());

        // Default sort by timestamp desc if not specified
        if (inputData.getSorts() == null || inputData.getSorts().isEmpty()) {
            inputData.setSorts(List.of(SortCriteria.builder()
                    .fieldName("timestamp")
                    .direction(SortDirection.DESC)
                    .build()));
        }

        // Use inherited CommonService.filterEntities()
        Page<ApiKeyUsageLog> page = filterEntities(apiKeyUsageLogRepository, inputData);

        return buildPagedResponse(page);
    }

    @Override
    public ApiUsageStatsDTO getMyStats(User user) {
        long successCount = apiKeyUsageLogRepository.countByUserIdAndSuccess(user.getId(), true);
        long failureCount = apiKeyUsageLogRepository.countByUserIdAndSuccess(user.getId(), false);
        long totalCount = successCount + failureCount;

        // Get this month's count
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        long thisMonthCount = apiKeyUsageLogRepository.countByUserIdAndMonth(user.getId(), currentMonth);

        return ApiUsageStatsDTO.builder()
                .totalCalls(totalCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .thisMonthCalls(thisMonthCount)
                .successRate(totalCount > 0 ? Math.round((double) successCount / totalCount * 100) : 0)
                .build();
    }

    @Override
    public PagedResponse<ApiKeyUsageLogDTO> filterAllLogs(InputDataModel inputData) {
        // Default sort by timestamp desc if not specified
        if (inputData.getSorts() == null || inputData.getSorts().isEmpty()) {
            inputData.setSorts(List.of(SortCriteria.builder()
                    .fieldName("timestamp")
                    .direction(SortDirection.DESC)
                    .build()));
        }

        // Use inherited CommonService.filterEntities()
        Page<ApiKeyUsageLog> page = filterEntities(apiKeyUsageLogRepository, inputData);

        return buildPagedResponse(page);
    }

    private PagedResponse<ApiKeyUsageLogDTO> buildPagedResponse(Page<ApiKeyUsageLog> page) {
        PagedResponse<ApiKeyUsageLogDTO> response = new PagedResponse<>();
        response.setContent(page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        return response;
    }

    private ApiKeyUsageLogDTO mapToDTO(ApiKeyUsageLog log) {
        return ApiKeyUsageLogDTO.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userEmail(log.getUser() != null ? log.getUser().getEmail() : null)
                .endpoint(log.getEndpoint())
                .method(log.getMethod())
                .statusCode(log.getStatusCode())
                .success(log.isSuccess())
                .ipAddress(log.getIpAddress())
                .errorMessage(log.getErrorMessage())
                .timestamp(log.getTimestamp())
                .apiKeyId(log.getApiKey() != null ? log.getApiKey().getId() : null)
                .build();
    }
}
