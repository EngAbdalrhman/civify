package com.app.civify.usecase.admin;

import com.app.civify.dto.plan.PlanCreateRequest;
import com.app.civify.dto.plan.PlanDetailResponse;
import com.app.civify.dto.plan.PlanMapper;
import com.app.civify.dto.plan.PlanUpdateRequest;
import com.app.civify.entity.enums.AuditAction;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.security.Group;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.security.GroupRepository;
import com.app.civify.service.audit.AuditService;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPlanUseCaseImpl extends CommonService implements AdminPlanUseCase {

    private final SubscriptionService subscriptionService;
    private final PlanMapper planMapper;
    private final GroupRepository groupRepository;
    private final AuditService auditService;

    @Override
    public PagedResponse<SubscriptionPlan> filterPlans(InputDataModel inputData) {
        log.info("Filtering plans with criteria: {}", inputData);
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
        plan.setTokenLimit(request.getTokenLimit());
        plan.setWatermark(request.getWatermark());
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setAnnualPrice(request.getAnnualPrice());
        // Set base currency (default to EGP if null)
        plan.setCurrency(request.getCurrency() != null ? request.getCurrency() : "EGP");
        plan.setActive(request.getActive());

        // Billing restrictions
        if (request.getAllowAnnualBilling() != null) plan.setAllowAnnualBilling(request.getAllowAnnualBilling());
        if (request.getRenewalAllowed() != null) plan.setRenewalAllowed(request.getRenewalAllowed());
        if (request.getMaxBillingCycles() != null) plan.setMaxBillingCycles(request.getMaxBillingCycles());
        
        // Display settings
        if (request.getIsFeatured() != null) plan.setIsFeatured(request.getIsFeatured());
        if (request.getSortOrder() != null) plan.setSortOrder(request.getSortOrder());

        if (request.getLinkedGroupId() != null) {
            Group linkedGroup = groupRepository.findById(request.getLinkedGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Group not found: " + request.getLinkedGroupId()));
            plan.setLinkedGroup(linkedGroup);
            log.info("Linked plan '{}' to group '{}'", request.getName(), linkedGroup.getName());
        }

        SubscriptionPlan createdPlan = subscriptionService.createPlan(plan);
        
        logAudit(createdPlan.getId(), AuditAction.CREATE, null, planToMap(createdPlan),
                "Created plan: " + createdPlan.getName());
        
        return planMapper.toDetailResponse(createdPlan);
    }

    @Override
    @Transactional
    public PlanDetailResponse updatePlan(Long planId, PlanUpdateRequest request) {
        log.info("Updating subscription plan: {}", planId);

        SubscriptionPlan existing = subscriptionService.getPlanById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
        Map<String, Object> oldState = planToMap(existing);

        SubscriptionPlan updatedPlan = new SubscriptionPlan();
        if (request.getName() != null) updatedPlan.setName(request.getName());
        if (request.getDescription() != null) updatedPlan.setDescription(request.getDescription());
        if (request.getDetails() != null) updatedPlan.setDetails(request.getDetails());
        if (request.getCvLimit() != null) updatedPlan.setCvLimit(request.getCvLimit());
        if (request.getTokenLimit() != null) updatedPlan.setTokenLimit(request.getTokenLimit());
        if (request.getWatermark() != null) updatedPlan.setWatermark(request.getWatermark());
        if (request.getMonthlyPrice() != null) updatedPlan.setMonthlyPrice(request.getMonthlyPrice());
        if (request.getAnnualPrice() != null) updatedPlan.setAnnualPrice(request.getAnnualPrice());
        if (request.getCurrency() != null) updatedPlan.setCurrency(request.getCurrency());
        if (request.getActive() != null) updatedPlan.setActive(request.getActive());
        
        // Billing restrictions
        if (request.getAllowAnnualBilling() != null) updatedPlan.setAllowAnnualBilling(request.getAllowAnnualBilling());
        if (request.getRenewalAllowed() != null) updatedPlan.setRenewalAllowed(request.getRenewalAllowed());
        if (request.getMaxBillingCycles() != null) updatedPlan.setMaxBillingCycles(request.getMaxBillingCycles());
        
        // Display settings
        if (request.getIsFeatured() != null) updatedPlan.setIsFeatured(request.getIsFeatured());
        if (request.getSortOrder() != null) updatedPlan.setSortOrder(request.getSortOrder());
        
        // Linked group
        if (request.getLinkedGroupId() != null) {
            Group linkedGroup = groupRepository.findById(request.getLinkedGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Group not found: " + request.getLinkedGroupId()));
            updatedPlan.setLinkedGroup(linkedGroup);
        }

        SubscriptionPlan updated = subscriptionService.updatePlan(planId, updatedPlan);
        
        logAudit(planId, AuditAction.UPDATE, oldState, planToMap(updated),
                "Updated plan: " + updated.getName());
        
        return planMapper.toDetailResponse(updated);
    }

    @Override
    @Transactional
    public SubscriptionPlan activatePlan(Long planId) {
        SubscriptionPlan plan = subscriptionService.activatePlan(planId);
        logAudit(planId, AuditAction.UPDATE, null, null,
                "Activated plan: " + plan.getName());
        return plan;
    }

    @Override
    @Transactional
    public SubscriptionPlan deactivatePlan(Long planId) {
        SubscriptionPlan plan = subscriptionService.deactivatePlan(planId);
        logAudit(planId, AuditAction.UPDATE, null, null,
                "Deactivated plan: " + plan.getName());
        return plan;
    }

    @Override
    @Transactional
    public void deletePlan(Long planId) {
        SubscriptionPlan plan = subscriptionService.getPlanById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
        String planName = plan.getName();
        subscriptionService.deletePlan(planId);
        logAudit(planId, AuditAction.DELETE, planToMap(plan), null,
                "Deleted plan: " + planName);
    }

    private void logAudit(Long entityId, AuditAction action, Map<String, Object> oldVal, 
                          Map<String, Object> newVal, String comment) {
        try {
            auditService.logAudit("SubscriptionPlan", entityId.toString(), action, oldVal, newVal, comment);
        } catch (Exception e) {
            log.warn("Failed to log audit for SubscriptionPlan action: {}", action, e);
        }
    }

    private Map<String, Object> planToMap(SubscriptionPlan plan) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", plan.getName());
        map.put("monthlyPrice", plan.getMonthlyPrice());
        map.put("annualPrice", plan.getAnnualPrice());
        map.put("cvLimit", plan.getCvLimit());
        map.put("active", plan.getActive());
        return map;
    }
}
