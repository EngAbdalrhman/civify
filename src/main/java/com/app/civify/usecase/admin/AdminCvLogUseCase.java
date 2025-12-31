package com.app.civify.usecase.admin;

import com.app.civify.entity.logs.CvOperationLog;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

public interface AdminCvLogUseCase {
    PagedResponse<CvOperationLog> getCvLogs(InputDataModel inputData);
}
