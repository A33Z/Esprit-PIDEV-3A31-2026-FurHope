package com.esprit.services.ai;

import com.esprit.entities.Reclamation;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReclamationAutoReplyCoordinatorTest {

    @Test
    void shouldNotPublishWhenModeIsOff() throws SQLException {
        AtomicInteger publishCount = new AtomicInteger();
        ReclamationAutoReplyCoordinator coordinator = new ReclamationAutoReplyCoordinator(
                new ReclamationAutoReplyService(),
                new ReclamationAutoReplyConfig(
                        AutoReplyMode.OFF, 99, "RULES", "", "", "", 0.70
                ),
                (reclamationId, adminId, message) -> publishCount.incrementAndGet()
        ) {
            @Override
            protected int resolveActorAdminId() {
                return 99;
            }
        };

        ReclamationAutoReplyResult result = coordinator.process(lowRiskReclamation());

        assertFalse(result.isEnabled());
        assertEquals(0, publishCount.get());
    }

    @Test
    void shouldPublishWhenSafeAutoSendAndAdminIdIsConfigured() throws SQLException {
        AtomicInteger publishCount = new AtomicInteger();
        ReclamationAutoReplyCoordinator coordinator = new ReclamationAutoReplyCoordinator(
                new ReclamationAutoReplyService(),
                new ReclamationAutoReplyConfig(
                        AutoReplyMode.SAFE_AUTO_SEND, 77, "RULES", "", "", "", 0.70
                ),
                (reclamationId, adminId, message) -> {
                    publishCount.incrementAndGet();
                    assertEquals(10, reclamationId);
                    assertEquals(77, adminId);
                    assertTrue(message != null && !message.isBlank());
                }
        ) {
            @Override
            protected int resolveActorAdminId() {
                return 77;
            }
        };

        ReclamationAutoReplyResult result = coordinator.process(lowRiskReclamation());

        assertTrue(result.isEnabled());
        assertTrue(result.isAutoSent());
        assertEquals(1, publishCount.get());
    }

    @Test
    void shouldFallbackToDraftWhenSystemAdminIdIsMissing() throws SQLException {
        AtomicInteger publishCount = new AtomicInteger();
        ReclamationAutoReplyCoordinator coordinator = new ReclamationAutoReplyCoordinator(
                new ReclamationAutoReplyService(),
                new ReclamationAutoReplyConfig(
                        AutoReplyMode.SAFE_AUTO_SEND, -1, "RULES", "", "", "", 0.70
                ),
                (reclamationId, adminId, message) -> publishCount.incrementAndGet()
        ) {
            @Override
            protected int resolveActorAdminId() {
                return -1;
            }
        };

        ReclamationAutoReplyResult result = coordinator.process(lowRiskReclamation());

        assertTrue(result.isEnabled());
        assertFalse(result.isAutoSent());
        assertTrue(result.getSummary().toLowerCase().contains("admin"));
        assertEquals(0, publishCount.get());
    }

    private Reclamation lowRiskReclamation() {
        Reclamation reclamation = new Reclamation();
        reclamation.setId(10);
        reclamation.setSujet("Status");
        reclamation.setDescription("Where is my update?");
        return reclamation;
    }
}
