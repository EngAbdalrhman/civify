package com.app.civify.usecase.admin;

import com.app.civify.dto.plan.PlanCreateRequest;
import com.app.civify.dto.plan.PlanDetailResponse;
import com.app.civify.dto.plan.PlanUpdateRequest;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

import java.util.List;

public interface AdminPlanUseCase {
    PagedResponse<SubscriptionPlan> filterPlans(InputDataModel inputData);
    List<PlanDetailResponse> getAllPlans();
    PlanDetailResponse getPlanWithStats(Long planId);
    PlanDetailResponse createPlan(PlanCreateRequest request);
    PlanDetailResponse updatePlan(Long planId, PlanUpdateRequest request);
    SubscriptionPlan activatePlan(Long planId);
    SubscriptionPlan deactivatePlan(Long planId);
    void deletePlan(Long planId);
}
