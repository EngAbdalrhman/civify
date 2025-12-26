package com.app.civify.usecase.logs;

import com.app.civify.dto.ApiKeyUsageLogDTO;
import com.app.civify.entity.User;
import com.app.civify.service.logs.ApiUsageLogService;
import com.app.civify.service.logs.ApiUsageLogService.ApiUsageStatsDTO;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiUsageLogUseCaseImpl extends CommonService implements ApiUsageLogUseCase {

    private final ApiUsageLogService apiUsageLogService;

    @Override
    public PagedResponse<ApiKeyUsageLogDTO> filterMyLogs(User user, InputDataModel inputData) {
        log.debug("UseCase: Filtering logs for user: {}", user.getEmail());
        return apiUsageLogService.filterMyLogs(user, inputData);
    }

    @Override
    public ApiUsageStatsDTO getMyStats(User user) {
        log.debug("UseCase: Fetching usage stats for user: {}", user.getEmail());
        return apiUsageLogService.getMyStats(user);
    }

    @Override
    public PagedResponse<ApiKeyUsageLogDTO> filterAllLogs(InputDataModel inputData) {
        log.debug("UseCase: Admin filtering all logs with criteria: {}", inputData);
        return apiUsageLogService.filterAllLogs(inputData);
    }
}
