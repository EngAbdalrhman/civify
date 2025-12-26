package com.app.civify.usecase.admin;

import com.app.civify.dto.subscription.UserSubscriptionDto;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

public interface AdminSubscriptionUseCase {
    PagedResponse<UserSubscriptionDto> filterSubscriptions(InputDataModel inputData);
}
