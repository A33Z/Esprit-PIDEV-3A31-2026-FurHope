package com.esprit.services.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceTemplateStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSaveAndReadTemplateWithNormalizedEmailKey() throws Exception {
        FaceTemplateStore store = new FaceTemplateStore(tempDir);

        double[] vector = {0.12, 0.34, 0.56};
        store.saveTemplate("  USER@Example.com  ", vector);

        Optional<double[]> read = store.getTemplate("user@example.com");

        assertTrue(read.isPresent());
        assertArrayEquals(vector, read.get(), 1e-9);
        assertTrue(store.hasTemplate("USER@EXAMPLE.COM"));
    }

    @Test
    void shouldReturnEmptyWhenNoTemplateExists() throws Exception {
        FaceTemplateStore store = new FaceTemplateStore(tempDir);
        assertFalse(store.getTemplate("missing@example.com").isPresent());
        assertFalse(store.hasTemplate("missing@example.com"));
    }
}
