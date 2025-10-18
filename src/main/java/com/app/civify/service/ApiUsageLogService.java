package com.app.civify.service;

import com.app.civify.dto.filter.FilterRequest;
import com.app.civify.dto.filter.FilterResponse;
import com.app.civify.entity.ApiUsageLog;
import com.app.civify.repository.ApiUsageLogRepository;
import com.app.civify.service.filter.GenericFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing API usage logs with advanced filtering capabilities
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiUsageLogService {

    private final ApiUsageLogRepository apiUsageLogRepository;
    private final GenericFilterService genericFilterService;

    /**
     * Filter API usage logs with advanced criteria
     *
     * Example usage:
     * <pre>
     * FilterGroup group = FilterGroup.and()
     *     .addFilter("endpoint", FilterOperation.MATCH, "/api/v1/cv")
     *     .addFilter("success", FilterOperation.EQUAL, true)
     *     .addFilter("timestamp", FilterOperation.DATE_GREATER_THAN, "2024-01-01");
     *
     * FilterRequest request = FilterRequest.builder()
     *     .filterGroup(group)
     *     .page(0)
     *     .size(20)
     *     .sortBy("timestamp")
     *     .sortDirection(Sort.Direction.DESC)
     *     .build();
     *
     * FilterResponse<ApiUsageLog> response = service.filterLogs(request);
     * </pre>
     */
    public FilterResponse<ApiUsageLog> filterLogs(FilterRequest request) {
        Specification<ApiUsageLog> spec = genericFilterService.buildSpecification(request.getFilterGroup());

        Pageable pageable = PageRequest.of(
            request.getPage(),
            request.getSize(),
            request.getSort()
        );

        Page<ApiUsageLog> page = spec != null
            ? apiUsageLogRepository.findAll(spec, pageable)
            : apiUsageLogRepository.findAll(pageable);

        return FilterResponse.fromPage(page);
    }

    /**
     * Get all logs for a specific user
     */
    public List<ApiUsageLog> getLogsByUserId(Long userId) {
        return apiUsageLogRepository.findByUserId(userId);
    }

    /**
     * Get logs for a user in a specific month
     */
    public List<ApiUsageLog> getLogsByUserAndMonth(Long userId, String month) {
        return apiUsageLogRepository.findByUserIdAndMonth(userId, month);
    }

    /**
     * Get logs for a user in a date range
     */
    public List<ApiUsageLog> getLogsByUserAndDateRange(Long userId, LocalDateTime start, LocalDateTime end) {
        return apiUsageLogRepository.findByUserIdAndTimestampBetween(userId, start, end);
    }

    /**
     * Count API calls for a user in a specific month
     */
    public Long countApiCallsByUserAndMonth(Long userId, String month) {
        return apiUsageLogRepository.countByUserIdAndMonth(userId, month);
    }

    /**
     * Create a new API usage log entry
     */
    @Transactional
    public ApiUsageLog createLog(ApiUsageLog log) {
        if (log.getTimestamp() == null) {
            log.setTimestamp(LocalDateTime.now());
        }
        if (log.getMonth() == null) {
            log.setMonth(log.getTimestamp().getYear() + "-" +
                String.format("%02d", log.getTimestamp().getMonthValue()));
        }
        return apiUsageLogRepository.save(log);
    }
}
