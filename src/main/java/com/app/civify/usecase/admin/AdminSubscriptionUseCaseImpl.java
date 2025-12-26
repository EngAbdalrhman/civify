package com.app.civify.usecase.admin;

import com.app.civify.dto.subscription.UserSubscriptionDto;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.repository.plans.UserSubscriptionRepository;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSubscriptionUseCaseImpl extends CommonService implements AdminSubscriptionUseCase {

    private final UserSubscriptionRepository subscriptionRepository;

    @Override
    public PagedResponse<UserSubscriptionDto> filterSubscriptions(InputDataModel inputData) {
        log.debug("Filtering subscriptions with criteria: {}", inputData);
        
        Page<UserSubscription> page = filterEntities(subscriptionRepository, inputData);
        
        PagedResponse<UserSubscriptionDto> response = new PagedResponse<>();
        response.setContent(page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList()));
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        
        return response;
    }

    private UserSubscriptionDto mapToDto(UserSubscription sub) {
        return UserSubscriptionDto.builder()
                .id(sub.getId())
                .userId(sub.getUser() != null ? sub.getUser().getId() : null)
                .userName(sub.getUser() != null ? sub.getUser().getName() : null)
                .userEmail(sub.getUser() != null ? sub.getUser().getEmail() : null)
                .planName(sub.getPlan() != null ? sub.getPlan().getName() : null)
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .status(sub.getStatus())
                .currentCvCount(sub.getCurrentCvCount())
                .currentApiCount(sub.getCurrentApiCount())
                .currentTokenUsage(sub.getCurrentTokenUsage())
                .usageResetDate(sub.getUsageResetDate())
                .promoCode(sub.getPromoCode() != null ? sub.getPromoCode().getCode() : null)
                .paymentTransactionId(sub.getPaymentTransactionId())
                .build();
    }
}
