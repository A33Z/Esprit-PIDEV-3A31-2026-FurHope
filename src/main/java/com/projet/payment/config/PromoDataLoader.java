package com.projet.payment.config;

import com.projet.payment.entity.PromoCode;
import com.projet.payment.repository.PromoCodeRepository;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PromoDataLoader implements CommandLineRunner {

    private final PromoCodeRepository promoCodeRepository;

    public PromoDataLoader(PromoCodeRepository promoCodeRepository) {
        this.promoCodeRepository = promoCodeRepository;
    }

    @Override
    public void run(String... args) {
        if (promoCodeRepository.count() == 0) {
            PromoCode promoCode = new PromoCode(
                    "azerty",
                    10.0,
                    LocalDateTime.now().plusDays(30),
                    100,
                    0
            );
            promoCodeRepository.save(promoCode);
        }
    }
}
