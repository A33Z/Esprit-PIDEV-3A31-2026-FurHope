package integrations.auth;

import com.github.sarxos.webcam.Webcam;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FaceAuthService {

    private static final double MATCH_THRESHOLD = 0.92d;
    private static volatile boolean opencvLoaded = false;
    private static final Object CAMERA_LOCK = new Object();
    private static Webcam sharedWebcam;
    private static int sharedWebcamUsers = 0;

    private final FaceTemplateStore templateStore = new FaceTemplateStore();
    private volatile CascadeClassifier faceCascade;

    public static final class CameraSession implements AutoCloseable {
        private final Webcam webcam;
        private volatile boolean closed = false;

        private CameraSession(Webcam webcam) {
            this.webcam = webcam;
        }

        public BufferedImage captureFrame() {
            if (closed) {
                throw new IllegalStateException("Camera session is already closed.");
            }
            BufferedImage image = null;
            for (int i = 0; i < 12 && image == null; i++) {
                image = webcam.getImage();
                if (image == null) {
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Camera capture interrupted.", e);
                    }
                }
            }
            if (image == null) {
                throw new RuntimeException("Opened camera but failed to capture frame: " + webcam.getName());
            }
            return image;
        }

        @Override
        public void close() {
            synchronized (CAMERA_LOCK) {
                if (closed) {
                    return;
                }
                closed = true;

                if (sharedWebcam == webcam) {
                    if (sharedWebcamUsers > 0) {
                        sharedWebcamUsers--;
                    }
                    if (sharedWebcamUsers == 0) {
                        if (webcam.isOpen()) {
                            webcam.close();
                        }
                        sharedWebcam = null;
                    }
                } else if (webcam.isOpen()) {
                    webcam.close();
                }
            }
        }
    }

    public static final class PreviewResult {
        private final BufferedImage image;
        private final BufferedImage rawImage;
        private final boolean detected;
        private final String message;

        public PreviewResult(BufferedImage image, boolean detected, String message) {
            this(image, image, detected, message);
        }

        public PreviewResult(BufferedImage image, BufferedImage rawImage, boolean detected, String message) {
            this.image = image;
            this.rawImage = rawImage;
            this.detected = detected;
            this.message = message;
        }

        public BufferedImage getImage() {
            return image;
        }

        public boolean isDetected() {
            return detected;
        }

        public BufferedImage getRawImage() {
            return rawImage;
        }

        public String getMessage() {
            return message;
        }
    }

    public boolean hasEnrollment(String email) {
        try {
            return templateStore.hasTemplate(email);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read face template store.", e);
        }
    }

    public void enroll(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required for face enrollment.");
        }

        BufferedImage image = captureSingleFrame();
        enroll(email, image);
    }

    public void enroll(String email, BufferedImage image) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required for face enrollment.");
        }
        if (image == null) {
            throw new IllegalArgumentException("Captured image is required for face enrollment.");
        }

        double[] vector = extractFaceVector(image);

        try {
            templateStore.saveTemplate(email, vector);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save face template.", e);
        }
    }

    public boolean verify(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required for face login.");
        }

        Optional<double[]> enrolled;
        try {
            enrolled = templateStore.getTemplate(email);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read face template.", e);
        }

        if (enrolled.isEmpty()) {
            return false;
        }

        BufferedImage image = captureSingleFrame();
        return verify(email, image);
    }

    public boolean verify(String email, BufferedImage image) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required for face login.");
        }
        if (image == null) {
            throw new IllegalArgumentException("Captured image is required for face login.");
        }

        Optional<double[]> enrolled;
        try {
            enrolled = templateStore.getTemplate(email);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read face template.", e);
        }

        if (enrolled.isEmpty()) {
            return false;
        }

        double[] probe = extractFaceVector(image);

        double similarity = cosineSimilarity(enrolled.get(), probe);
        return similarity >= MATCH_THRESHOLD;
    }

    public PreviewResult capturePreview() {
        try (CameraSession session = openCameraSession()) {
            return capturePreview(session);
        }
    }

    public PreviewResult capturePreview(CameraSession session) {
        ensureOpenCvLoaded();
        BufferedImage image = session.captureFrame();
        return annotatePreview(image);
    }

    private BufferedImage captureSingleFrame() {
        try (CameraSession session = openCameraSession()) {
            return session.captureFrame();
        }
    }

    public CameraSession openCameraSession() {
        synchronized (CAMERA_LOCK) {
            if (sharedWebcam != null) {
                if (!sharedWebcam.isOpen()) {
                    try {
                        openWithoutLock(sharedWebcam);
                    } catch (RuntimeException ex) {
                        sharedWebcam = null;
                        sharedWebcamUsers = 0;
                    }
                }
                if (sharedWebcam != null && sharedWebcam.isOpen()) {
                    sharedWebcamUsers++;
                    return new CameraSession(sharedWebcam);
                }
            }

            List<Webcam> webcams = Webcam.getWebcams();
            if (webcams == null || webcams.isEmpty()) {
                throw new RuntimeException("No webcam found. Check camera permission and that no other app is using it.");
            }

            RuntimeException lastError = null;
            for (Webcam webcam : webcams) {
                try {
                    openWithoutLock(webcam);
                    sharedWebcam = webcam;
                    sharedWebcamUsers = 1;
                    return new CameraSession(webcam);
                } catch (RuntimeException ex) {
                    lastError = ex;
                    if (webcam.isOpen()) {
                        webcam.close();
                    }
                }
            }

            String cameraNames = webcams.stream().map(Webcam::getName).collect(Collectors.joining(", "));
            if (lastError != null) {
                throw new RuntimeException("Unable to open camera from available webcams [" + cameraNames + "]. " + lastError.getMessage(), lastError);
            }
            throw new RuntimeException("Unable to open camera from available webcams [" + cameraNames + "].");
        }
    }

    private void openWithoutLock(Webcam webcam) {
        // Avoid file-lock contention from webcam-capture library in repeated open/close flows.
        webcam.getLock().disable();
        webcam.open();
    }

    private PreviewResult annotatePreview(BufferedImage image) {
        Mat mat = bufferedImageToMat(image);
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);

        MatOfRect faces = new MatOfRect();
        detector().detectMultiScale(gray, faces, 1.1, 5, 0, new Size(90, 90), new Size());
        Rect[] detectedFaces = faces.toArray();

        BufferedImage annotated = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = annotated.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.setColor(Color.GREEN);
        g.setStroke(new BasicStroke(3f));
        for (Rect face : detectedFaces) {
            g.drawRect(face.x, face.y, face.width, face.height);
        }
        g.dispose();

        boolean detected = detectedFaces.length > 0;
        String message = detected
                ? "Face detected (" + detectedFaces.length + ")."
                : "No face detected. Center your face and improve lighting.";
        return new PreviewResult(annotated, image, detected, message);
    }

    private double[] extractFaceVector(BufferedImage bufferedImage) {
        ensureOpenCvLoaded();
        Mat image = bufferedImageToMat(bufferedImage);
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        MatOfRect faces = new MatOfRect();
        detector().detectMultiScale(gray, faces, 1.1, 5, 0, new Size(90, 90), new Size());
        Rect[] detectedFaces = faces.toArray();

        if (detectedFaces.length == 0) {
            throw new RuntimeException("No face detected. Keep your face centered and retry.");
        }

        Rect bestFace = java.util.Arrays.stream(detectedFaces)
                .max(Comparator.comparingInt(face -> face.width * face.height))
                .orElseThrow(() -> new RuntimeException("No face detected."));

        Mat face = new Mat(gray, bestFace);
        Mat normalizedFace = new Mat();
        Imgproc.resize(face, normalizedFace, new Size(128, 128));
        Imgproc.equalizeHist(normalizedFace, normalizedFace);

        return buildHistogram(normalizedFace);
    }

    private double[] buildHistogram(Mat faceMat) {
        int bins = 64;
        double[] histogram = new double[bins];

        int rows = faceMat.rows();
        int cols = faceMat.cols();
        int pixels = rows * cols;

        byte[] row = new byte[cols];
        for (int y = 0; y < rows; y++) {
            faceMat.get(y, 0, row);
            for (int x = 0; x < cols; x++) {
                int pixel = row[x] & 0xFF;
                int bin = Math.min((pixel * bins) / 256, bins - 1);
                histogram[bin] += 1.0;
            }
        }

        for (int i = 0; i < bins; i++) {
            histogram[i] /= pixels;
        }
        return histogram;
    }

    static double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) {
            return 0.0d;
        }

        double dot = 0.0d;
        double normA = 0.0d;
        double normB = 0.0d;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0d || normB == 0.0d) {
            return 0.0d;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    static boolean isMatch(double[] enrolled, double[] probe) {
        return cosineSimilarity(enrolled, probe) >= MATCH_THRESHOLD;
    }

    private Mat bufferedImageToMat(BufferedImage bufferedImage) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            MatOfByte mob = new MatOfByte(baos.toByteArray());
            return Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR);
        } catch (IOException e) {
            throw new RuntimeException("Unable to process camera image.", e);
        }
    }

    private CascadeClassifier detector() {
        CascadeClassifier local = faceCascade;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            local = faceCascade;
            if (local == null) {
                ensureOpenCvLoaded();
                local = new CascadeClassifier(loadCascadeClassifierPath());
                if (local.empty()) {
                    throw new IllegalStateException("Unable to initialize face detector.");
                }
                faceCascade = local;
            }
            return local;
        }
    }

    private String loadCascadeClassifierPath() {
        try (InputStream is = getClass().getResourceAsStream("/opencv/haarcascade_frontalface_default.xml")) {
            if (is == null) {
                throw new RuntimeException("Cascade classifier resource missing.");
            }

            Path tempFile = Files.createTempFile("furhope-haarcascade-", ".xml");
            Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return tempFile.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load cascade classifier.", e);
        }
    }

    private static void ensureOpenCvLoaded() {
        if (opencvLoaded) {
            return;
        }
        synchronized (FaceAuthService.class) {
            if (!opencvLoaded) {
                OpenCV.loadLocally();
                if (Core.VERSION.isBlank()) {
                    throw new IllegalStateException("OpenCV failed to initialize.");
                }
                opencvLoaded = true;
            }
        }
    }
}
