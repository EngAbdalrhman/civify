package com.app.civify.usecase.admin;

import com.app.civify.dto.logs.CvOperationLogDto;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

public interface AdminCvLogUseCase {
    PagedResponse<CvOperationLogDto> getCvLogs(InputDataModel inputData);
}
