package com.esprit.services.ai;

import java.util.Locale;

public class LocalReclamationResponseModel {

    public String generate(String complaintText) {
        String text = complaintText == null ? "" : complaintText.trim();
        if (text.isEmpty()) {
            return "Thank you for contacting FurHope Support. I could not detect the issue details yet. "
                    + "Please share what happened, when it happened, and any related ticket or reservation reference so we can help immediately.";
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        String tone = detectTone(normalized);
        String issue = detectIssue(normalized);
        String urgency = detectUrgency(normalized);
        String opening = buildOpening(tone);
        String strategy = buildStrategy(tone, issue, urgency);
        String actions = buildActions(issue, urgency);
        String clarification = buildClarification(text, issue);
        String closure = buildClosure(tone, urgency);

        return opening + " "
                + "From your report, the main issue appears to be " + issue + ". "
                + strategy + " "
                + actions + " "
                + clarification + " "
                + closure;
    }

    private String detectTone(String text) {
        if (containsAny(text,
                "angry", "frustrated", "unacceptable", "furious", "bad service", "never again",
                "this is ridiculous", "this is unacceptable", "i am upset", "i'm upset",
                "terrible", "awful", "nobody helped", "ignored")) {
            return "angry";
        }
        if (containsAny(text, "worried", "concerned", "urgent", "asap", "immediately", "scared", "afraid")) {
            return "worried";
        }
        if (containsAny(text, "confused", "not sure", "i don't understand", "why", "how")) {
            return "confused";
        }
        return "neutral";
    }

    private String detectIssue(String text) {
        if (containsAny(text, "refund", "payment", "charged", "invoice", "billing")) {
            return "a payment or billing problem";
        }
        if (containsAny(text, "delay", "late", "no response", "waiting")) {
            return "a delay in handling your request";
        }
        if (containsAny(text, "login", "password", "account", "blocked", "cannot sign in")) {
            return "an account access issue";
        }
        if (containsAny(text, "reservation", "booking", "cancel", "date", "hotel")) {
            return "a reservation management issue";
        }
        if (containsAny(text, "bug", "error", "crash", "not working", "fails")) {
            return "a technical issue in the platform";
        }
        if (containsAny(text, "staff", "agent", "support", "rude", "behavior")) {
            return "a service quality concern";
        }
        if (containsAny(text, "animal", "pet", "adoption", "shelter", "health")) {
            return "an animal care or adoption-related concern";
        }
        return "a support issue that needs case-specific review";
    }

    private String detectUrgency(String text) {
        if (containsAny(text, "urgent", "immediately", "asap", "now", "today", "stranded", "emergency")) {
            return "high";
        }
        return "normal";
    }

    private String buildOpening(String tone) {
        if ("angry".equals(tone)) {
            return "I understand why this situation feels frustrating, and your concern is valid.";
        }
        if ("worried".equals(tone)) {
            return "Thank you for raising this quickly; I understand this situation can feel stressful.";
        }
        if ("confused".equals(tone)) {
            return "Thanks for sharing the details; I understand why this feels confusing.";
        }
        return "Thank you for reporting this issue, and we appreciate the detail you provided.";
    }

    private String buildStrategy(String tone, String issue, String urgency) {
        if ("angry".equals(tone)) {
            return "We will prioritize de-escalation and ownership, with clear updates at each step so you are not left without visibility.";
        }
        if ("worried".equals(tone) || "high".equals(urgency)) {
            return "We will treat this with priority and share a concrete update quickly to reduce uncertainty.";
        }
        if ("confused".equals(tone)) {
            return "We will keep the explanation simple and verify each step to make the resolution path clear.";
        }
        if (issue.contains("technical")) {
            return "We will reproduce the problem and confirm a workaround while a permanent fix is prepared.";
        }
        return "We will resolve this through a direct, case-specific support flow.";
    }

    private String buildActions(String issue, String urgency) {
        if (issue.contains("payment or billing")) {
            return "We will verify the transaction trail, payment status, and account logs to confirm exactly what was charged and whether correction or refund action is required.";
        }
        if (issue.contains("delay")) {
            return "We will review the ticket timeline and assign immediate follow-up so your case no longer remains without updates.";
        }
        if (issue.contains("account access")) {
            return "We will check authentication logs and account status, then guide you through a secure unlock or reset path if needed.";
        }
        if (issue.contains("reservation")) {
            return "We will verify reservation records, date conflicts, and status transitions to correct the booking flow and restore expected behavior.";
        }
        if (issue.contains("technical")) {
            if ("high".equals(urgency)) {
                return "We will immediately inspect system errors and reproducible steps, then provide a short-term workaround while final fix work is prepared.";
            }
            return "We will inspect recent system errors and reproducible steps to isolate the bug and provide a workaround or fix plan.";
        }
        if (issue.contains("service quality")) {
            return "We will escalate this to the service lead and review the interaction quality to ensure respectful, professional handling.";
        }
        if (issue.contains("animal care")) {
            return "We will coordinate with the shelter operations team to verify the animal case details and ensure proper follow-up.";
        }
        return "We will review the full ticket context and route it to the right team for a precise resolution.";
    }

    private String buildClarification(String text, String issue) {
        if (text.length() < 70 || issue.contains("case-specific review")) {
            return "To speed this up, please share the approximate date/time, what you expected, what happened instead, and any related ID.";
        }
        return "If available, please include any reference ID, screenshot, or exact timestamp so we can finalize the investigation faster.";
    }

    private String buildClosure(String tone, String urgency) {
        if ("high".equals(urgency)) {
            return "We will keep this as high priority and send a progress update as soon as the first verification step is completed.";
        }
        if ("angry".equals(tone)) {
            return "We appreciate your patience and will keep communication clear and actionable until this is resolved.";
        }
        return "Once we receive the final details, we will share a clear next step and expected timeline.";
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
