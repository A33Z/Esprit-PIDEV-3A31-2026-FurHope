package com.projet.payment.controller;

import com.projet.payment.dto.PromoRequest;
import com.projet.payment.dto.PromoResponse;
import com.projet.payment.service.PromoCodeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promo")
public class PromoController {

    private final PromoCodeService promoCodeService;

    public PromoController(PromoCodeService promoCodeService) {
        this.promoCodeService = promoCodeService;
    }

    @PostMapping("/apply")
    public ResponseEntity<PromoResponse> applyPromo(@Valid @RequestBody PromoRequest request) {
        PromoResponse response = promoCodeService.applyPromo(request.getCode(), request.getAmount());
        return ResponseEntity.ok(response);
    }
}
