package com.app.civify.usecase.logs;

import com.app.civify.dto.ApiKeyUsageLogDTO;
import com.app.civify.entity.User;
import com.app.civify.service.logs.ApiUsageLogService.ApiUsageStatsDTO;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

public interface ApiUsageLogUseCase {
    PagedResponse<ApiKeyUsageLogDTO> filterMyLogs(User user, InputDataModel inputData);
    ApiUsageStatsDTO getMyStats(User user);
    PagedResponse<ApiKeyUsageLogDTO> filterAllLogs(InputDataModel inputData);
}
