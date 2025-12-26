package com.app.civify.usecase.admin;

import com.app.civify.dto.plan.PlanCreateRequest;
import com.app.civify.dto.plan.PlanDetailResponse;
import com.app.civify.dto.plan.PlanMapper;
import com.app.civify.dto.plan.PlanUpdateRequest;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.security.Group;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.security.GroupRepository;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPlanUseCaseImpl extends CommonService implements AdminPlanUseCase {

    private final SubscriptionService subscriptionService;
    private final PlanMapper planMapper;
    private final GroupRepository groupRepository;

    @Override
    public PagedResponse<SubscriptionPlan> filterPlans(InputDataModel inputData) {
        log.info("Filtering plans with criteria: {}", inputData);
        // Assuming subscriptionService.filterPlans is already cleanish or we just delegate
        // Actually, SubscriptionService extends CommonService too, but here we can just delegate
        return subscriptionService.filterPlans(inputData);
    }

    @Override
    public List<PlanDetailResponse> getAllPlans() {
        log.info("Fetching all subscription plans");
        List<SubscriptionPlan> plans = subscriptionService.getAllPlans();
        return plans.stream()
                .map(planMapper::toDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PlanDetailResponse getPlanWithStats(Long planId) {
        log.info("Fetching plan {} with statistics", planId);
        SubscriptionPlan plan = subscriptionService.getPlanById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + planId));
        
        long activeSubscriptionCount = subscriptionService.getActiveSubscriptionCountForPlan(planId);
        
        PlanDetailResponse response = planMapper.toDetailResponse(plan);
        response.setActiveSubscriptionCount(activeSubscriptionCount);
        
        return response;
    }

    @Override
    @Transactional
    public PlanDetailResponse createPlan(PlanCreateRequest request) {
        log.info("Creating new subscription plan: {}", request.getName());

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setDetails(request.getDetails());
        plan.setCvLimit(request.getCvLimit());
        plan.setApiLimit(request.getApiLimit());
        plan.setWatermark(request.getWatermark());
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setAnnualPrice(request.getAnnualPrice());
        plan.setActive(request.getActive());

        // Link group if specified
        if (request.getLinkedGroupId() != null) {
            Group linkedGroup = groupRepository.findById(request.getLinkedGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Group not found: " + request.getLinkedGroupId()));
            plan.setLinkedGroup(linkedGroup);
            log.info("Linked plan '{}' to group '{}'", request.getName(), linkedGroup.getName());
        }

        SubscriptionPlan createdPlan = subscriptionService.createPlan(plan);
        return planMapper.toDetailResponse(createdPlan);
    }

    @Override
    @Transactional
    public PlanDetailResponse updatePlan(Long planId, PlanUpdateRequest request) {
        log.info("Updating subscription plan: {}", planId);

        // Map request to partial entity for service update logic
        SubscriptionPlan updatedPlan = new SubscriptionPlan();
        if (request.getName() != null) updatedPlan.setName(request.getName());
        if (request.getDescription() != null) updatedPlan.setDescription(request.getDescription());
        if (request.getDetails() != null) updatedPlan.setDetails(request.getDetails());
        if (request.getCvLimit() != null) updatedPlan.setCvLimit(request.getCvLimit());
        if (request.getApiLimit() != null) updatedPlan.setApiLimit(request.getApiLimit());
        if (request.getWatermark() != null) updatedPlan.setWatermark(request.getWatermark());
        if (request.getMonthlyPrice() != null) updatedPlan.setMonthlyPrice(request.getMonthlyPrice());
        if (request.getAnnualPrice() != null) updatedPlan.setAnnualPrice(request.getAnnualPrice());
        if (request.getActive() != null) updatedPlan.setActive(request.getActive());

        SubscriptionPlan updated = subscriptionService.updatePlan(planId, updatedPlan);
        return planMapper.toDetailResponse(updated);
    }

    @Override
    public SubscriptionPlan activatePlan(Long planId) {
        return subscriptionService.activatePlan(planId);
    }

    @Override
    public SubscriptionPlan deactivatePlan(Long planId) {
        return subscriptionService.deactivatePlan(planId);
    }

    @Override
    public void deletePlan(Long planId) {
        subscriptionService.deletePlan(planId);
    }
}
