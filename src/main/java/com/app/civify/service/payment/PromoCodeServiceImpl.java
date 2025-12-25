package com.app.civify.service.payment;

import com.app.civify.entity.PromoCode;
import com.app.civify.exception.BadRequestException;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.PromoCodeRepository;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoCodeServiceImpl extends CommonService implements PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    @Override
    public PagedResponse<PromoCode> filterPromoCodes(InputDataModel inputData) {
        log.debug("Filtering promo codes with criteria: {}", inputData);
        Page<PromoCode> page = filterEntities(promoCodeRepository, inputData);

        PagedResponse<PromoCode> response = new PagedResponse<>();
        response.setContent(page.getContent());
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

    @Override
    @Transactional
    public PromoCode createPromoCode(PromoCode promoCode) {
        log.info("Creating new promo code: {}", promoCode.getCode());

        // Validate code uniqueness
        if (promoCodeRepository.findByCodeAndActiveTrue(promoCode.getCode()).isPresent()) {
            throw new BadRequestException("Active promo code '" + promoCode.getCode() + "' already exists");
        }

        // Validate values
        if (promoCode.getDiscountValue() != null && promoCode.getDiscountValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Discount value cannot be negative");
        }
        if (promoCode.getMinimumPurchase() != null && promoCode.getMinimumPurchase().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimum purchase cannot be negative");
        }
        if (promoCode.getUsageLimit() != null && promoCode.getUsageLimit() < 0) {
            throw new BadRequestException("Usage limit cannot be negative");
        }

        // Set defaults
        if (promoCode.getActive() == null) {
            promoCode.setActive(true);
        }
        if (promoCode.getUsedCount() == null) {
            promoCode.setUsedCount(0);
        }

        return promoCodeRepository.save(promoCode);
    }

    @Override
    @Transactional
    public PromoCode updatePromoCode(Long id, PromoCode updatedPromo) {
        log.info("Updating promo code: {}", id);

        PromoCode existingPromo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found with ID: " + id));

        // Update allowed fields
        if (updatedPromo.getCode() != null && !updatedPromo.getCode().equals(existingPromo.getCode())) {
            // Check uniqueness if code is changing
            if (promoCodeRepository.findByCodeAndActiveTrue(updatedPromo.getCode()).isPresent()) {
                throw new BadRequestException("Active promo code '" + updatedPromo.getCode() + "' already exists");
            }
            existingPromo.setCode(updatedPromo.getCode());
        }

        if (updatedPromo.getDescription() != null) {
            existingPromo.setDescription(updatedPromo.getDescription());
        }
        if (updatedPromo.getDiscountType() != null) {
            existingPromo.setDiscountType(updatedPromo.getDiscountType());
        }
        if (updatedPromo.getDiscountValue() != null) {
            if (updatedPromo.getDiscountValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Discount value cannot be negative");
            }
            existingPromo.setDiscountValue(updatedPromo.getDiscountValue());
        }
        if (updatedPromo.getMinimumPurchase() != null) {
            if (updatedPromo.getMinimumPurchase().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Minimum purchase cannot be negative");
            }
            existingPromo.setMinimumPurchase(updatedPromo.getMinimumPurchase());
        }
        if (updatedPromo.getUsageLimit() != null) {
            if (updatedPromo.getUsageLimit() < 0) {
                throw new BadRequestException("Usage limit cannot be negative");
            }
            existingPromo.setUsageLimit(updatedPromo.getUsageLimit());
        }
        if (updatedPromo.getExpiresAt() != null) {
            existingPromo.setExpiresAt(updatedPromo.getExpiresAt());
        }
        if (updatedPromo.getActive() != null) {
            existingPromo.setActive(updatedPromo.getActive());
        }

        return promoCodeRepository.save(existingPromo);
    }

    @Override
    @Transactional
    public PromoCode deactivatePromoCode(Long id) {
        log.info("Deactivating promo code: {}", id);
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found with ID: " + id));

        promoCode.setActive(false);
        return promoCodeRepository.save(promoCode);
    }

    @Override
    @Transactional
    public void deletePromoCode(Long id) {
        log.info("Soft deleting promo code: {}", id);
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found with ID: " + id));

        promoCode.setIsDeleted(true);
        promoCode.setActive(false); // Also deactivate it
        promoCodeRepository.save(promoCode);
    }

    @Override
    public Optional<PromoCode> getPromoCodeById(Long id) {
        return promoCodeRepository.findById(id);
    }

    @Override
    public Optional<PromoCode> findByCode(String code) {
        return promoCodeRepository.findByCodeAndActiveTrue(code.toUpperCase());
    }

    @Override
    public Optional<PromoCode> validatePromoCode(String code, BigDecimal amount) {
        log.debug("Validating promo code: {} for amount: {}", code, amount);
        
        Optional<PromoCode> promoOpt = promoCodeRepository.findByCodeAndActiveTrue(code.toUpperCase());
        
        if (promoOpt.isEmpty()) {
            log.debug("Promo code not found or inactive: {}", code);
            return Optional.empty();
        }
        
        PromoCode promo = promoOpt.get();
        
        // Check if promo is valid (not expired, not over usage limit)
        if (!promo.isValid()) {
            log.debug("Promo code is not valid (expired or usage limit reached): {}", code);
            return Optional.empty();
        }
        
        // Check minimum purchase requirement
        if (promo.getMinimumPurchase() != null && amount.compareTo(promo.getMinimumPurchase()) < 0) {
            log.debug("Amount {} is below minimum purchase {} for promo: {}", 
                amount, promo.getMinimumPurchase(), code);
            return Optional.empty();
        }
        
        return Optional.of(promo);
    }
}

