package com.esprit.services.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceAuthServiceTest {

    @Test
    void cosineSimilarityShouldReturnOneForSameVector() {
        double[] v = {0.1, 0.2, 0.3, 0.4};
        double similarity = FaceAuthService.cosineSimilarity(v, v);
        assertTrue(similarity > 0.999999);
    }

    @Test
    void isMatchShouldRejectClearlyDifferentVectors() {
        double[] enrolled = {1.0, 0.0, 0.0, 0.0};
        double[] probe = {0.0, 1.0, 0.0, 0.0};
        assertFalse(FaceAuthService.isMatch(enrolled, probe));
    }

    @Test
    void cosineSimilarityShouldHandleDifferentLengths() {
        double[] a = {0.1, 0.2};
        double[] b = {0.1, 0.2, 0.3};
        assertFalse(FaceAuthService.isMatch(a, b));
    }
}
