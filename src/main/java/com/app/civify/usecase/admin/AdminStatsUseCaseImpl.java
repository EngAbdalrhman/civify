package com.app.civify.usecase.admin;

import com.app.civify.dto.admin.AdminStatsDTO;
import com.app.civify.entity.enums.CvOperationAction;
import com.app.civify.enums.StatsPeriod;
import com.app.civify.repository.billing.InvoiceRepository;
import com.app.civify.repository.logs.ApiKeyUsageLogRepository;
import com.app.civify.repository.logs.CvOperationLogRepository;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.service.user.UserService;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatsUseCaseImpl extends CommonService implements AdminStatsUseCase {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final CvOperationLogRepository cvOperationLogRepository;
    private final ApiKeyUsageLogRepository apiKeyUsageLogRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    public AdminStatsDTO getStats(StatsPeriod period) {
        log.info("UseCase: Calculating dashboard statistics for period: {}", period);

        // Calculate date range based on period
        LocalDateTime startDate = calculateStartDate(period);
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startOfMonth = endDate.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        // Gather stats from various services
        long totalUsers = userService.countTotalUsers();
        long subscribedUsers = subscriptionService.countActiveSubscriptions();

        // CV counts based on period
        long totalCVs;
        long totalApiCalls;
        long apiCallsThisMonth;
        BigDecimal periodRevenue;

        if (period == StatsPeriod.OVERALL) {
            // All time stats
            totalCVs = cvOperationLogRepository.countByAction(CvOperationAction.UPLOAD);
            totalApiCalls = apiKeyUsageLogRepository.count();
            periodRevenue = invoiceRepository.sumPaidRevenue();
        } else {
            // Period-based stats
            long periodCVs = countOperationsInPeriod(CvOperationAction.UPLOAD, startDate, endDate);
            long periodApiCalls = apiKeyUsageLogRepository.countByDateRange(startDate, endDate);

            totalCVs = periodCVs;
            totalApiCalls = periodApiCalls;
            periodRevenue = invoiceRepository.sumPaidRevenueByDateRange(startDate, endDate);
        }

        // Always represent the current month to match UI label semantics.
        apiCallsThisMonth = apiKeyUsageLogRepository.countByDateRange(startOfMonth, endDate);
        BigDecimal revenueThisMonth = invoiceRepository.sumPaidRevenueByDateRange(startOfMonth, endDate);
        BigDecimal totalRevenue = invoiceRepository.sumPaidRevenue();

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .subscribedUsers(subscribedUsers)
                .totalCVs(totalCVs)
                .totalApiCalls(totalApiCalls)
                .apiCallsThisMonth(apiCallsThisMonth)
                .activeSubscriptions(subscribedUsers)
                .periodRevenue(periodRevenue)
                .revenueThisMonth(revenueThisMonth)
                .totalRevenue(totalRevenue)
                .period(period.name())
                .periodStart(startDate != null ? startDate.toString() : null)
                .periodEnd(endDate.toString())
                .build();
    }

    private LocalDateTime calculateStartDate(StatsPeriod period) {
        LocalDateTime now = LocalDateTime.now();

        return switch (period) {
            case MONTHLY -> now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case QUARTERLY -> {
                int currentMonth = now.getMonthValue();
                int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
                yield now.withMonth(quarterStartMonth).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            }
            case YEARLY -> now.withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case OVERALL -> null; // No start date for overall
        };
    }

    private long countOperationsInPeriod(CvOperationAction action, LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            return cvOperationLogRepository.countByAction(action);
        }
        // Use the date range query and filter by action
        return cvOperationLogRepository.findByDateRange(start, end, Pageable.unpaged())
                .stream()
                .filter(log -> log.getAction() == action)
                .count();
    }
}
