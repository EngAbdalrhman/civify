package com.app.civify.service.user;

import com.app.civify.dto.user.UserCustomeMapper;
import com.app.civify.dto.user.UserResponseDTO;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.entity.security.Group;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.UserRepository;
import com.app.civify.repository.logs.ApiKeyUsageLogRepository;
import com.app.civify.repository.plans.UserSubscriptionRepository;
import com.app.civify.repository.resume.ResumeRepository;
import com.app.civify.repository.security.GroupRepository;
import com.app.civify.util.filter.FilterCriteria;
import com.app.civify.util.filter.FilterGroup;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of admin user management service
 * Handles filtering, searching, and user management for administrators
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserServiceImpl extends CommonService implements AdminUserService {
    
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final ResumeRepository resumeRepository;
    private final ApiKeyUsageLogRepository apiKeyUsageLogRepository;
    private final GroupRepository groupRepository;
    private final UserCustomeMapper userCustomeMapper;
    
    @Override
    public PagedResponse<UserResponseDTO> filterUsers(InputDataModel inputData) {
        log.debug("Filtering users with criteria: {}", inputData);

        normalizePlanFilters(inputData);
        
        // Use CommonService (inherited) to filter entities
        Page<User> userPage = filterEntities(userRepository, inputData);
        
        // Convert entities to DTOs
        List<UserResponseDTO> userDTOs = userPage.getContent().stream()
                .map(userCustomeMapper::toResponseDTO)
                .collect(Collectors.toList());

        // Populate additional stats for each user
        String currentMonth = java.time.LocalDate.now().toString().substring(0, 7); // YYYY-MM

        for (UserResponseDTO dto : userDTOs) {
            // Resume Count (stored resumes)
            dto.setCvCount((int) resumeRepository.countByUserId(dto.getId()));

            // API Usage Count (External Keys)
            dto.setApiUsageCount(Math.toIntExact(apiKeyUsageLogRepository.countByUserIdAndMonth(dto.getId(), currentMonth)));

            // Plan
            List<UserSubscription> activeSubs = subscriptionRepository.findByUserIdAndStatus(
                    dto.getId(), 
                    com.app.civify.entity.enums.SubscriptionStatus.ACTIVE
            );
            
            if (!activeSubs.isEmpty()) {
                dto.setPlan(activeSubs.getFirst().getPlan().getName());
            } else {
                dto.setPlan("free");
            }
        }
        
        // Create and populate paged response
        PagedResponse<UserResponseDTO> response = new PagedResponse<>();
        response.setContent(userDTOs);
        response.setPage(userPage.getNumber());
        response.setSize(userPage.getSize());
        response.setTotalElements(userPage.getTotalElements());
        response.setTotalPages(userPage.getTotalPages());
        response.setFirst(userPage.isFirst());
        response.setLast(userPage.isLast());
        response.setHasNext(userPage.hasNext());
        response.setHasPrevious(userPage.hasPrevious());
        
        log.debug("Filtered {} users out of {} total", userDTOs.size(), userPage.getTotalElements());
        
        return response;
    }

    private void normalizePlanFilters(InputDataModel inputData) {
        if (inputData == null) {
            return;
        }

        normalizePlanFilters(inputData.getFilters());

        if (inputData.getFilterGroups() != null) {
            inputData.getFilterGroups().forEach(this::normalizePlanFilters);
        }
    }

    private void normalizePlanFilters(FilterGroup filterGroup) {
        if (filterGroup == null) {
            return;
        }

        normalizePlanFilters(filterGroup.getFilters());

        if (filterGroup.getGroups() != null) {
            filterGroup.getGroups().forEach(this::normalizePlanFilters);
        }
    }

    private void normalizePlanFilters(List<FilterCriteria> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }

        List<FilterCriteria> normalizedFilters = new ArrayList<>();
        for (FilterCriteria filter : filters) {
            if (isPlanFilter(filter)) {
                normalizedFilters.add(buildActiveSubscriptionFilter());
                normalizedFilters.add(buildPlanNameFilter(filter));
                continue;
            }

            normalizedFilters.add(filter);
        }

        filters.clear();
        filters.addAll(normalizedFilters);
    }

    private boolean isPlanFilter(FilterCriteria filter) {
        if (filter == null || filter.getFieldName() == null) {
            return false;
        }

        return "plan".equals(filter.getFieldName())
                || "subscriptions.plan.name".equals(filter.getFieldName());
    }

    private FilterCriteria buildActiveSubscriptionFilter() {
        return FilterCriteria.builder()
                .fieldName("subscriptions.status")
                .operation(com.app.civify.util.filter.FilterOperation.EQUAL)
                .value(SubscriptionStatus.ACTIVE.name())
                .build();
    }

    private FilterCriteria buildPlanNameFilter(FilterCriteria originalFilter) {
        return FilterCriteria.builder()
                .fieldName("subscriptions.plan.name")
                .operation(originalFilter.getOperation())
                .value(originalFilter.getValue())
                .value2(originalFilter.getValue2())
                .build();
    }

    @Override
    public UserResponseDTO getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        return userCustomeMapper.toResponseDTO(user);
    }
    
    @Override
    public UserResponseDTO getUserByIdWithStats(Long userId) {
        log.debug("Fetching user by ID with stats: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Get subscription statistics
        Long activeSubscriptionCount = subscriptionRepository.countByUserIdAndStatus(
                userId, 
                com.app.civify.entity.enums.SubscriptionStatus.ACTIVE
        );
        
        Long totalSubscriptionCount = subscriptionRepository.countByUserId(userId);
        
        return userCustomeMapper.toResponseDTOWithStats(user, activeSubscriptionCount, totalSubscriptionCount);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUserLimits(Long userId, Integer cvLimit) {
        log.info("Updating limits for user {}: CV={}", userId, cvLimit);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setCvLimit(cvLimit);
        
        User savedUser = userRepository.save(user);
        
        return getUserByIdWithStats(savedUser.getId());
    }

    @Override
    @Transactional
    public UserResponseDTO toggleAccountLock(Long userId) {
        log.info("Toggling account status for user {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Toggle between ACTIVE and DISABLED
        if (user.getStatus() == com.app.civify.entity.enums.UserStatus.ACTIVE) {
            user.setStatus(com.app.civify.entity.enums.UserStatus.DISABLED);
        } else {
            user.setStatus(com.app.civify.entity.enums.UserStatus.ACTIVE);
        }
        
        User savedUser = userRepository.save(user);
        log.info("User {} status changed to: {}", userId, savedUser.getStatus());
        
        return userCustomeMapper.toResponseDTO(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUserGroup(Long userId, Long groupId) {
        log.info("Updating group for user {}: groupId={}", userId, groupId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with ID: " + groupId));
        
        user.setGroup(group);
        User savedUser = userRepository.save(user);
        log.info("User {} assigned to group: {}", userId, group.getName());
        
        return userCustomeMapper.toResponseDTO(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUserTokens(Long userId, Integer extraTokens) {
        log.info("Updating extra tokens for user {}: extraTokens={}", userId, extraTokens);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setExtraTokens(extraTokens != null ? extraTokens : 0);
        User savedUser = userRepository.save(user);
        log.info("User {} extra tokens set to: {}", userId, savedUser.getExtraTokens());
        
        return userCustomeMapper.toResponseDTO(savedUser);
    }
}

