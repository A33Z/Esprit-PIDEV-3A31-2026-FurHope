package com.esprit.services;

import com.esprit.entities.Reclamation;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ReclamationAiAssistantService {

    private static final Set<String> SWEAR_WORDS = Set.of(
            "fuck", "shit", "bitch", "asshole", "bastard", "damn",
            "merde", "putain", "connard", "con", "salope",
            "nik", "9ahba", "zebi", "zebbi", "ks", "ksom"
    );

    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = normalize(text);
        for (String token : normalized.split("\\s+")) {
            if (SWEAR_WORDS.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public String findFirstProfanity(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = normalize(text);
        for (String token : normalized.split("\\s+")) {
            if (SWEAR_WORDS.contains(token)) {
                return token;
            }
        }
        return null;
    }

    public String buildImmediateReply(Reclamation reclamation) {
        String subject = safe(reclamation == null ? null : reclamation.getSujet()).toLowerCase(Locale.ROOT);
        String description = safe(reclamation == null ? null : reclamation.getDescription()).toLowerCase(Locale.ROOT);
        String text = subject + " " + description;

        if (containsAny(text, List.of("login", "signin", "password", "mot de passe", "compte bloque"))) {
            return """
                    Merci pour votre reclamation. Essayez d'abord:
                    1) Reinitialiser votre mot de passe
                    2) Verifier email/tel associe
                    3) Relancer l'application
                    Si le probleme persiste, un admin va verifier votre compte rapidement.
                    """.trim();
        }
        if (containsAny(text, List.of("payment", "paiement", "facture", "stripe", "refund", "remboursement"))) {
            return """
                    Merci, nous avons detecte un sujet de paiement.
                    Verifiez le statut de la transaction et gardez la reference de paiement.
                    Un admin va confirmer l'operation et vous guider pour remboursement/correction si necessaire.
                    """.trim();
        }
        if (containsAny(text, List.of("reservation", "booking", "hotel", "rendezvous", "vet", "veterinaire"))) {
            return """
                    Merci pour le signalement.
                    Verifiez la date, l'heure et les informations de reservation dans votre espace.
                    Un administrateur prendra la conversation pour finaliser la solution.
                    """.trim();
        }

        return """
                Merci, votre reclamation est bien recue.
                Nous vous recommandons de joindre les details (heure, capture ecran, etapes realisees).
                Un administrateur vous repondra ici pour resoudre le probleme avec vous.
                """.trim();
    }

    public String buildChatReply(String userMessage) {
        String text = safe(userMessage).toLowerCase(Locale.ROOT);
        if (containsAny(text, List.of("merci", "thanks", "ok", "resolved", "regle"))) {
            return "Ravi d'avoir aide. Si le probleme revient, ecrivez ici et on continue ensemble.";
        }
        if (containsAny(text, List.of("error", "erreur", "bug", "crash", "fail"))) {
            return "Merci. Essayez de relancer l'application, puis envoyez une capture ecran + l'heure exacte de l'erreur.";
        }
        if (containsAny(text, List.of("payment", "paiement", "refund", "remboursement"))) {
            return "Pour paiement/remboursement, partagez la reference de transaction. Un admin va confirmer la suite.";
        }
        return "Message recu. Donnez plus de details (etapes, heure, capture) et un admin vous repondra vite.";
    }

    private boolean containsAny(String text, List<String> keys) {
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
