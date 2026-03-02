package com.esprit.services.ai;

import com.esprit.entities.Reclamation;

import java.util.Locale;

public class ReclamationAutoReplyService {

    private final SmartReplyGenerator primaryGenerator;
    private final SmartReplyGenerator fallbackGenerator;
    private final ReclamationAutoReplyConfig config;

    public ReclamationAutoReplyService() {
        this(ReclamationAutoReplyConfig.load());
    }

    public ReclamationAutoReplyService(ReclamationAutoReplyConfig config) {
        this.config = config;
        this.primaryGenerator = config.isOpenAiCompatibleEnabled()
                ? new OpenAiCompatibleSmartReplyGenerator(config)
                : new RuleBasedSmartReplyGenerator();
        this.fallbackGenerator = new RuleBasedSmartReplyGenerator();
    }

    public ReclamationAutoReplyDecision decide(Reclamation reclamation) {
        SmartReplyGeneration generation = primaryGenerator.generate(reclamation);
        if (!isUsable(generation)) {
            generation = fallbackGenerator.generate(reclamation);
        }

        boolean forcedHumanReview = generation.isRequiresHumanReview()
                || generation.getConfidence() < config.getMinimumConfidence()
                || containsHighRiskKeywords(reclamation);
        boolean autoSendCandidate = !forcedHumanReview;

        return new ReclamationAutoReplyDecision(
                generation.getIntent(),
                forcedHumanReview,
                autoSendCandidate,
                generation.getMessage(),
                generation.getReason() + " | confidence=" + String.format(Locale.ROOT, "%.2f", generation.getConfidence())
        );
    }

    private boolean isUsable(SmartReplyGeneration generation) {
        return generation != null
                && generation.getMessage() != null
                && !generation.getMessage().isBlank();
    }

    private boolean containsHighRiskKeywords(Reclamation reclamation) {
        String text = normalize((reclamation == null ? "" : reclamation.getSujet()) + " " +
                (reclamation == null ? "" : reclamation.getDescription()));
        return containsAny(text, "lawyer", "attorney", "legal", "court", "fraud", "police", "sue", "threat");
    }

    private boolean containsAny(String source, String... terms) {
        for (String term : terms) {
            if (source.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace('\n', ' ').trim();
    }
}
