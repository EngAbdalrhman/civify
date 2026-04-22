package com.app.civify.service.billing;

import com.app.civify.dto.billing.BillingTransactionDto;
import com.app.civify.entity.billing.BillingTransaction;
import com.app.civify.repository.billing.BillingTransactionRepository;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingTransactionService extends CommonService {

    private final BillingTransactionRepository billingTransactionRepository;

    public PagedResponse<BillingTransactionDto> filterTransactions(InputDataModel inputData) {
        Page<BillingTransaction> page = filterEntities(billingTransactionRepository, inputData);
        Page<BillingTransactionDto> dtoPage = page.map(this::mapToDto);
        return PagedResponse.of(dtoPage);
    }

    private BillingTransactionDto mapToDto(BillingTransaction transaction) {
        return BillingTransactionDto.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .orderId(transaction.getOrderId())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .gateway(transaction.getGateway())
                .subscriptionId(transaction.getSubscriptionId())
                .completedAt(transaction.getCompletedAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
