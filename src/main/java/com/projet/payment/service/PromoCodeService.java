package com.projet.payment.service;

import com.projet.payment.dto.PromoResponse;
import com.projet.payment.entity.PromoCode;
import com.projet.payment.repository.PromoCodeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    public PromoCodeService(PromoCodeRepository promoCodeRepository) {
        this.promoCodeRepository = promoCodeRepository;
    }

    @Transactional
    public PromoResponse applyPromo(String code, double amount) {
        String normalizedCode = code == null ? "" : code.trim();
        if (normalizedCode.isEmpty()) {
            return invalidResponse(amount, "Promo code is required");
        }

        Optional<PromoCode> optionalPromoCode = promoCodeRepository.findByCodeIgnoreCase(normalizedCode);
        if (optionalPromoCode.isEmpty()) {
            return invalidResponse(amount, "Invalid promo code");
        }

        PromoCode promoCode = optionalPromoCode.get();
        if (promoCode.getExpirationDate().isBefore(LocalDateTime.now())) {
            return invalidResponse(amount, "Promo code expired");
        }

        if (promoCode.getUsedCount() >= promoCode.getUsageLimit()) {
            return invalidResponse(amount, "Promo code usage limit reached");
        }

        double discount = amount * promoCode.getDiscountPercent() / 100.0;
        double finalAmount = amount - discount;

        promoCode.setUsedCount(promoCode.getUsedCount() + 1);
        promoCodeRepository.save(promoCode);

        return new PromoResponse(
                true,
                roundToTwoDecimals(discount),
                roundToTwoDecimals(finalAmount),
                "Promo applied successfully"
        );
    }

    private PromoResponse invalidResponse(double amount, String message) {
        return new PromoResponse(false, 0.0, roundToTwoDecimals(amount), message);
    }

    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
