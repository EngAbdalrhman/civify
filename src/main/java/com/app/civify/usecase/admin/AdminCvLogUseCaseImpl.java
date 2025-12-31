package com.app.civify.usecase.admin;

import com.app.civify.entity.logs.CvOperationLog;
import com.app.civify.service.cvlog.CvOperationLogService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminCvLogUseCaseImpl implements AdminCvLogUseCase {

    private final CvOperationLogService cvOperationLogService;

    @Override
    public PagedResponse<CvOperationLog> getCvLogs(InputDataModel inputData) {
        return cvOperationLogService.getLogs(inputData);
    }
}
