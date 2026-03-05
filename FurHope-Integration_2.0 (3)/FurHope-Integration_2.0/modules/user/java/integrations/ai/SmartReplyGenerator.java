package com.esprit.services.ai;

import com.esprit.entities.Reclamation;

public interface SmartReplyGenerator {
    SmartReplyGeneration generate(Reclamation reclamation);
}
