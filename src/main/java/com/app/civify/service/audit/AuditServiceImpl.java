package com.app.civify.service.audit;

import com.app.civify.dto.audit.AuditLogDTO;
import com.app.civify.dto.audit.AuditLogMapper;
import com.app.civify.dto.audit.AuditLogRequest;
import com.app.civify.entity.AuditLog;
import com.app.civify.entity.enums.AuditAction;
import com.app.civify.repository.AuditLogRepository;
import com.app.civify.service.filter.GenericFilterService;
import com.app.civify.util.filter.*;
import com.app.civify.util.persistence.CommonService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of AuditService Handles audit logging and retrieval with
 * automatic context capture
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final GenericFilterService filterService;
    private final CommonService commonService;

    @Override
    @Transactional
    public void logAudit(String entityType, String entityId, AuditAction action,
            Map<String, Object> oldValue, Map<String, Object> newValue, String comment) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);
            auditLog.setOldValue(oldValue);
            auditLog.setNewValue(newValue);
            auditLog.setComment(comment);

            // Calculate changes
            if (oldValue != null && newValue != null) {
                auditLog.setChanges(calculateChanges(oldValue, newValue));
            }

            // Capture current user
            auditLog.setPerformedBy(getCurrentUsername());

            // Capture request context (IP, User-Agent)
            captureRequestContext(auditLog);

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} {} for {}/{}", action, entityType, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to create audit log for {}/{}: {}", entityType, entityId, e.getMessage(), e);
            // Don't throw exception - audit logging should not break business logic
        }
    }

    @Override
    @Transactional
    public void logAudit(String entityType, String entityId, AuditAction action,
            Map<String, Object> oldValue, Map<String, Object> newValue) {
        logAudit(entityType, entityId, action, oldValue, newValue, null);
    }

    @Override
    @Transactional
    public void logAudit(String entityType, String entityId, AuditAction action, String comment) {
        logAudit(entityType, entityId, action, null, null, comment);
    }

    @Override
    public PagedResponse<AuditLogDTO> getAuditHistory(String entityType, String entityId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "performedAt"));
        Page<AuditLog> auditPage = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
        return convertToPagedResponse(auditPage);
    }

    @Override
    public PagedResponse<AuditLogDTO> getAuditHistoryByType(String entityType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "performedAt"));
        Page<AuditLog> auditPage = auditLogRepository.findByEntityType(entityType, pageable);
        return convertToPagedResponse(auditPage);
    }

    @Override
    public PagedResponse<AuditLogDTO> getAuditHistory(AuditLogRequest request) {
        // Build filter group from request
        FilterGroup filterGroup = buildFilterGroup(request);

        // Create InputDataModel for filtering
        InputDataModel inputData = new InputDataModel();

        if (filterGroup != null && !filterGroup.isEmpty()) {
            inputData.addFilterGroup(filterGroup);
        }

        // Add sorting
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "performedAt";
        String sortDirection = request.getSortDirection() != null ? request.getSortDirection() : "DESC";
        inputData.addSort(sortBy, SortDirection.valueOf(sortDirection));

        // Add pagination
        inputData.setPagination(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 20
        );

        // Use CommonService to filter
        Page<AuditLog> auditPage = commonService.filterEntities(auditLogRepository, inputData);
        return convertToPagedResponse(auditPage);
    }

    @Override
    public AuditLogDTO getLatestAudit(String entityType, String entityId) {
        AuditLog auditLog = auditLogRepository.findFirstByEntityTypeAndEntityIdOrderByPerformedAtDesc(entityType, entityId);
        return auditLogMapper.toDTO(auditLog);
    }

    @Override
    @Transactional
    public int deleteOldAuditLogs(int daysToKeep) {
        // This would require a custom query - implement if needed
        log.warn("deleteOldAuditLogs not yet implemented");
        return 0;
    }

    /**
     * Calculate field-level changes between old and new values
     */
    private Map<String, Object> calculateChanges(Map<String, Object> oldValue, Map<String, Object> newValue) {
        Map<String, Object> changes = new HashMap<>();

        // Find all keys from both maps
        oldValue.keySet().forEach(key -> {
            Object oldVal = oldValue.get(key);
            Object newVal = newValue.get(key);

            if (!isEqual(oldVal, newVal)) {
                Map<String, Object> change = new HashMap<>();
                change.put("old", oldVal);
                change.put("new", newVal);
                changes.put(key, change);
            }
        });

        return changes;
    }

    /**
     * Check if two values are equal
     */
    private boolean isEqual(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !authentication.getPrincipal().equals("anonymousUser")) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("Could not get current username: {}", e.getMessage());
        }
        return "SYSTEM";
    }

    /**
     * Capture request context (IP address, User-Agent)
     */
    private void captureRequestContext(AuditLog auditLog) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.debug("Could not capture request context: {}", e.getMessage());
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Build FilterGroup from AuditLogRequest
     */
    private FilterGroup buildFilterGroup(AuditLogRequest request) {
        // If advanced filter group is provided, use it
        if (request.getFilterGroup() != null) {
            return request.getFilterGroup();
        }

        // Otherwise, build from simple filters
        FilterGroup filterGroup = FilterGroup.and();
        boolean hasFilters = false;

        if (request.getEntityType() != null) {
            filterGroup.addFilter("entityType", FilterOperation.EQUAL, request.getEntityType());
            hasFilters = true;
        }

        if (request.getEntityId() != null) {
            filterGroup.addFilter("entityId", FilterOperation.EQUAL, request.getEntityId());
            hasFilters = true;
        }

        if (request.getAction() != null) {
            filterGroup.addFilter("action", FilterOperation.EQUAL, request.getAction());
            hasFilters = true;
        }

        if (request.getPerformedBy() != null) {
            filterGroup.addFilter("performedBy", FilterOperation.EQUAL, request.getPerformedBy());
            hasFilters = true;
        }

        if (request.getStartDate() != null && request.getEndDate() != null) {
            filterGroup.addFilter("performedAt", FilterOperation.DATE_BETWEEN,
                    request.getStartDate(), request.getEndDate());
            hasFilters = true;
        } else if (request.getStartDate() != null) {
            filterGroup.addFilter("performedAt", FilterOperation.DATE_GREATER_THAN_EQUAL,
                    request.getStartDate());
            hasFilters = true;
        } else if (request.getEndDate() != null) {
            filterGroup.addFilter("performedAt", FilterOperation.DATE_LESS_THAN_EQUAL,
                    request.getEndDate());
            hasFilters = true;
        }

        return hasFilters ? filterGroup : null;
    }

    /**
     * Convert Page<AuditLog> to PagedResponse<AuditLogDTO>
     */
    private PagedResponse<AuditLogDTO> convertToPagedResponse(Page<AuditLog> auditPage) {
        PagedResponse<AuditLogDTO> response = new PagedResponse<>();
        response.setContent(auditPage.getContent().stream()
                .map(auditLogMapper::toDTO)
                .collect(Collectors.toList()));
        response.setPage(auditPage.getNumber());
        response.setSize(auditPage.getSize());
        response.setTotalElements(auditPage.getTotalElements());
        response.setTotalPages(auditPage.getTotalPages());
        response.setFirst(auditPage.isFirst());
        response.setLast(auditPage.isLast());
        response.setHasNext(auditPage.hasNext());
        response.setHasPrevious(auditPage.hasPrevious());
        return response;
    }
}
