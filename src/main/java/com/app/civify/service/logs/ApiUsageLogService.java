package com.app.civify.service.logs;

import com.app.civify.dto.ApiKeyUsageLogDTO;
import com.app.civify.entity.User;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

/**
 * Service interface for API Key Usage Logs
 */
public interface ApiUsageLogService {

    /**
     * Filter logs for a specific user (auto-filters by current month)
     */
    PagedResponse<ApiKeyUsageLogDTO> filterMyLogs(User user, InputDataModel inputData);

    /**
     * Get usage stats for a specific user
     */
    ApiUsageStatsDTO getMyStats(User user);

    /**
     * Filter all logs (admin only)
     */
    PagedResponse<ApiKeyUsageLogDTO> filterAllLogs(InputDataModel inputData);

    /**
     * DTO for API usage statistics
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class ApiUsageStatsDTO {
        private long totalCalls;
        private long successCount;
        private long failureCount;
        private long thisMonthCalls;
        private long successRate;
    }
}
