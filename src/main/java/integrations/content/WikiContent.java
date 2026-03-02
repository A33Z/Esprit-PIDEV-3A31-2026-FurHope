package integrations.content;

import java.util.List;

public record WikiContent(
        String shortDescription,
        String fullDescription,
        String heroImageUrl,
        List<String> galleryImageUrls
) {
}
