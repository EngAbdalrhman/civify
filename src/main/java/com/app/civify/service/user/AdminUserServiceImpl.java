package com.app.civify.service.user;

import com.app.civify.dto.user.UserMapper;
import com.app.civify.dto.user.UserResponseDTO;
import com.app.civify.entity.User;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.ApiUsageLogRepository;
import com.app.civify.repository.CvRepository;
import com.app.civify.repository.UserRepository;
import com.app.civify.repository.UserSubscriptionRepository;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CvRepository cvRepository;
    private final ApiUsageLogRepository apiUsageLogRepository;
    private final UserMapper userMapper;
    
    @Override
    public PagedResponse<UserResponseDTO> filterUsers(InputDataModel inputData) {
        log.debug("Filtering users with criteria: {}", inputData);
        
        // Use CommonService (inherited) to filter entities
        Page<User> userPage = filterEntities(userRepository, inputData);
        
        // Convert entities to DTOs
        List<UserResponseDTO> userDTOs = userPage.getContent().stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());

        // Populate additional stats for each user
        String currentMonth = java.time.LocalDate.now().toString().substring(0, 7); // YYYY-MM

        for (UserResponseDTO dto : userDTOs) {
            // CV Count
            dto.setCvCount((int) cvRepository.countByUserId(dto.getId()));

            // API Usage Count
            dto.setApiUsageCount(Math.toIntExact(apiUsageLogRepository.countByUserIdAndMonth(dto.getId(), currentMonth)));

            // Plan
            List<com.app.civify.entity.UserSubscription> activeSubs = subscriptionRepository.findByUserIdAndStatus(
                    dto.getId(), 
                    com.app.civify.entity.enums.SubscriptionStatus.ACTIVE
            );
            
            if (!activeSubs.isEmpty()) {
                dto.setPlan(activeSubs.get(0).getPlan().getName());
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
    
    @Override
    public PagedResponse<UserResponseDTO> getAllUsers(int page, int size) {
        log.debug("Fetching all users - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.findAll(pageable);
        
        // Convert entities to DTOs
        List<UserResponseDTO> userDTOs = userPage.getContent().stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
        
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
        
        log.debug("Retrieved {} users", userDTOs.size());
        
        return response;
    }
    
    @Override
    public UserResponseDTO getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        return userMapper.toResponseDTO(user);
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
        
        return userMapper.toResponseDTOWithStats(user, activeSubscriptionCount, totalSubscriptionCount);
    }
    @Override
    @Transactional
    public UserResponseDTO updateUserLimits(Long userId, Integer cvLimit, Integer apiLimit) {
        log.info("Updating limits for user {}: CV={}, API={}", userId, cvLimit, apiLimit);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setCvLimit(cvLimit);
        user.setApiRateLimit(apiLimit);
        
        User savedUser = userRepository.save(user);
        
        return getUserByIdWithStats(savedUser.getId());
    }
}

