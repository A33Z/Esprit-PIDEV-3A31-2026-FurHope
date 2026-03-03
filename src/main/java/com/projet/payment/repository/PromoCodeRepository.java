package com.projet.payment.repository;

import com.projet.payment.entity.PromoCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCode(String code);

    Optional<PromoCode> findByCodeIgnoreCase(String code);
}
