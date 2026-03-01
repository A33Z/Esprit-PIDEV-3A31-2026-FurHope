package integrations.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

public class FaceTemplateStore {

    private final Path storePath;

    public FaceTemplateStore() {
        this(Paths.get(System.getProperty("user.home"), ".furhope-user"));
    }

    FaceTemplateStore(Path baseDir) {
        this.storePath = baseDir.resolve("face-templates.properties");
    }

    public synchronized void saveTemplate(String email, double[] vector) throws IOException {
        Properties properties = loadProperties();
        properties.setProperty(key(email), serialize(vector));
        storeProperties(properties);
    }

    public synchronized Optional<double[]> getTemplate(String email) throws IOException {
        Properties properties = loadProperties();
        String value = properties.getProperty(key(email));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(deserialize(value));
    }

    public synchronized boolean hasTemplate(String email) throws IOException {
        Properties properties = loadProperties();
        return properties.containsKey(key(email));
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        if (Files.exists(storePath)) {
            try (InputStream inputStream = Files.newInputStream(storePath)) {
                properties.load(inputStream);
            }
        }
        return properties;
    }

    private void storeProperties(Properties properties) throws IOException {
        Files.createDirectories(storePath.getParent());
        try (OutputStream outputStream = Files.newOutputStream(storePath)) {
            properties.store(outputStream, "FurHope face templates");
        }
    }

    private String serialize(double[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    private double[] deserialize(String value) {
        String[] parts = value.split(",");
        double[] vector = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Double.parseDouble(parts[i]);
        }
        return vector;
    }
}
