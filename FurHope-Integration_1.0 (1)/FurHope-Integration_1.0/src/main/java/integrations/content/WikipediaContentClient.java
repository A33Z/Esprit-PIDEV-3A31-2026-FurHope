package integrations.content;

import com.fasterxml.jackson.databind.JsonNode;
import integrations.http.ExternalApiException;
import integrations.http.HttpJsonClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WikipediaContentClient {

    private static final String WIKIPEDIA_API = "https://en.wikipedia.org/w/api.php";
    private static final String COMMONS_API = "https://commons.wikimedia.org/w/api.php";

    private final HttpJsonClient httpJsonClient;

    public WikipediaContentClient() {
        this.httpJsonClient = new HttpJsonClient();
    }

    public WikiContent fetchHotelContent(String hotelName, String city) {
        if (hotelName == null || hotelName.trim().isEmpty()) {
            return empty();
        }

        try {
            String title = resolveTitle(hotelName, city);
            String fullDescription = "";
            String shortDescription = "";
            String hero = "";

            if (!title.isBlank()) {
                JsonNode details = loadPageDetails(title);
                JsonNode page = extractFirstPage(details);
                if (page != null) {
                    fullDescription = sanitizeText(page.path("extract").asText(""));
                    shortDescription = summarize(fullDescription);
                    hero = resolveHeroImage(page);
                }
            }

            List<String> gallery = resolveGallery(title, hotelName, city);
            if (hero.isBlank() && !gallery.isEmpty()) {
                hero = gallery.get(0);
            }

            return new WikiContent(shortDescription, fullDescription, hero, gallery);
        } catch (ExternalApiException e) {
            return empty();
        }
    }

    private String resolveTitle(String hotelName, String city) {
        String query = hotelName + " " + city + " hotel";
        String url = WIKIPEDIA_API + "?action=query&format=json&list=search&srlimit=1&srsearch=" + urlEncode(query);
        JsonNode response = httpJsonClient.getJson(url);

        JsonNode results = response.path("query").path("search");
        if (!results.isArray() || results.isEmpty()) {
            return "";
        }
        return results.get(0).path("title").asText("").trim();
    }

    private JsonNode loadPageDetails(String title) {
        String url = WIKIPEDIA_API
                + "?action=query&format=json&prop=extracts|pageimages"
                + "&explaintext=1&exchars=1500&piprop=original|thumbnail&pithumbsize=1200&titles="
                + urlEncode(title);
        return httpJsonClient.getJson(url);
    }

    private JsonNode extractFirstPage(JsonNode response) {
        JsonNode pages = response.path("query").path("pages");
        if (!pages.isObject()) {
            return null;
        }
        Iterator<JsonNode> iterator = pages.elements();
        return iterator.hasNext() ? iterator.next() : null;
    }

    private String resolveHeroImage(JsonNode page) {
        String original = page.path("original").path("source").asText("").trim();
        if (!original.isBlank()) {
            return original;
        }
        return page.path("thumbnail").path("source").asText("").trim();
    }

    private List<String> resolveGallery(String title, String hotelName, String city) {
        Set<String> images = new LinkedHashSet<>();
        images.addAll(searchCommonsImages(title + " " + hotelName + " " + city, 6));
        if (images.size() < 6) {
            images.addAll(searchCommonsImages(hotelName + " " + city + " hotel exterior", 6 - images.size()));
        }
        if (images.size() < 6) {
            images.addAll(searchCommonsImages(city + " hotel exterior", 6 - images.size()));
        }
        return new ArrayList<>(images);
    }

    private List<String> searchCommonsImages(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }
        String url = COMMONS_API
                + "?action=query&format=json&generator=search&gsrnamespace=6&gsrlimit=" + limit
                + "&prop=imageinfo&iiprop=url&gsrsearch=" + urlEncode(query);
        JsonNode response = httpJsonClient.getJson(url);
        JsonNode pages = response.path("query").path("pages");

        if (!pages.isObject()) {
            return List.of();
        }

        Set<String> images = new LinkedHashSet<>();
        Iterator<JsonNode> iterator = pages.elements();
        while (iterator.hasNext()) {
            JsonNode page = iterator.next();
            JsonNode imageInfo = page.path("imageinfo");
            if (!imageInfo.isArray() || imageInfo.isEmpty()) {
                continue;
            }
            String imageUrl = imageInfo.get(0).path("url").asText("").trim();
            if (!imageUrl.isBlank()) {
                images.add(imageUrl);
            }
        }

        return new ArrayList<>(images);
    }

    private String summarize(String fullDescription) {
        if (fullDescription == null || fullDescription.isBlank()) {
            return "";
        }
        String normalized = fullDescription.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 220) {
            return normalized;
        }
        return normalized.substring(0, 217).trim() + "...";
    }

    private String sanitizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private WikiContent empty() {
        return new WikiContent("", "", "", List.of());
    }

    private String urlEncode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }
}
