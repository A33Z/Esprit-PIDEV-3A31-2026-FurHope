package com.esprit.furhope.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class FunApiService {

    private enum Category {
        JOKE,
        QUOTE,
        CAT_FACT,
        DOG_IMAGE,
        MEME
    }

    public record MemePayload(String title, String imageUrl) {
    }

    private static final int CACHE_LIMIT = 5;
    private static final long CACHE_COOLDOWN_MS = 1200L;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(7);
    private static final String FRIENDLY_ERROR = "Service unavailable, try again";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Map<Category, Deque<Object>> cache;
    private final Map<Category, CompletableFuture<?>> inFlight;
    private final Map<Category, Long> lastRequestAtMs;

    public FunApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.mapper = new ObjectMapper();
        this.cache = new ConcurrentHashMap<>();
        this.inFlight = new ConcurrentHashMap<>();
        this.lastRequestAtMs = new ConcurrentHashMap<>();
    }

    public CompletableFuture<String> fetchRandomJoke() {
        return fetch(
                Category.JOKE,
                List.of("https://official-joke-api.appspot.com/random_joke"),
                node -> {
                    String setup = readText(node, "setup");
                    String punchline = readText(node, "punchline");
                    if (setup.isBlank() && punchline.isBlank()) {
                        throw new IllegalStateException("Invalid joke payload");
                    }
                    if (setup.isBlank()) return punchline;
                    if (punchline.isBlank()) return setup;
                    return setup + "\n" + punchline;
                }
        );
    }

    public CompletableFuture<String> fetchRandomQuote() {
        return fetch(
                Category.QUOTE,
                List.of(
                        "https://api.quotable.io/random",
                        "http://api.quotable.io/random"
                ),
                node -> {
                    String quote = readText(node, "content");
                    String author = readText(node, "author");
                    if (quote.isBlank()) {
                        throw new IllegalStateException("Invalid quote payload");
                    }
                    if (author.isBlank()) return quote;
                    return quote + "\n- " + author;
                }
        );
    }

    public CompletableFuture<String> fetchCatFact() {
        return fetch(
                Category.CAT_FACT,
                List.of("https://catfact.ninja/fact"),
                node -> {
                    String fact = readText(node, "fact");
                    if (fact.isBlank()) {
                        throw new IllegalStateException("Invalid cat fact payload");
                    }
                    return fact;
                }
        );
    }

    public CompletableFuture<String> fetchRandomDogImageUrl() {
        return fetch(
                Category.DOG_IMAGE,
                List.of("https://dog.ceo/api/breeds/image/random"),
                node -> {
                    String status = readText(node, "status");
                    String imageUrl = readText(node, "message");
                    if (!status.isBlank() && !"success".equalsIgnoreCase(status)) {
                        throw new IllegalStateException("Dog API response was not successful");
                    }
                    if (imageUrl.isBlank()) {
                        throw new IllegalStateException("Invalid dog image payload");
                    }
                    return imageUrl;
                }
        );
    }

    public CompletableFuture<MemePayload> fetchRandomMeme() {
        return fetch(
                Category.MEME,
                List.of("https://meme-api.com/gimme"),
                node -> {
                    String title = readText(node, "title");
                    String imageUrl = readText(node, "url");
                    if (imageUrl.isBlank()) {
                        JsonNode preview = node.get("preview");
                        if (preview != null && preview.isArray() && preview.size() > 0) {
                            imageUrl = preview.get(0).asText("").trim();
                        }
                    }
                    if (title.isBlank() || imageUrl.isBlank()) {
                        throw new IllegalStateException("Invalid meme payload");
                    }
                    return new MemePayload(title, imageUrl);
                }
        );
    }

    private <T> CompletableFuture<T> fetch(Category category, List<String> endpoints, Function<JsonNode, T> parser) {
        CompletableFuture<T> active = getInFlight(category);
        if (active != null && !active.isDone()) {
            return active;
        }

        long now = System.currentTimeMillis();
        long last = lastRequestAtMs.getOrDefault(category, 0L);
        if (now - last < CACHE_COOLDOWN_MS) {
            T cached = randomCached(category);
            if (cached != null) {
                return CompletableFuture.completedFuture(cached);
            }
        }

        CompletableFuture<T> request = fetchFromEndpoints(endpoints, parser)
                .thenApply(value -> {
                    cacheValue(category, value);
                    lastRequestAtMs.put(category, System.currentTimeMillis());
                    return value;
                })
                .exceptionally(ex -> {
                    throw new CompletionException(asFriendlyException(ex));
                });

        inFlight.put(category, request);
        request.whenComplete((unused, unusedErr) -> inFlight.remove(category));
        return request;
    }

    private <T> CompletableFuture<T> fetchFromEndpoints(List<String> endpoints, Function<JsonNode, T> parser) {
        if (endpoints == null || endpoints.isEmpty()) {
            return CompletableFuture.failedFuture(asFriendlyException(null));
        }

        String endpoint = endpoints.get(0);
        return fetchOne(endpoint, parser)
                .handle((result, err) -> {
                    if (err == null) {
                        return CompletableFuture.completedFuture(result);
                    }
                    if (endpoints.size() == 1) {
                        return CompletableFuture.<T>failedFuture(err);
                    }
                    return fetchFromEndpoints(endpoints.subList(1, endpoints.size()), parser);
                })
                .thenCompose(Function.identity());
    }

    private <T> CompletableFuture<T> fetchOne(String endpoint, Function<JsonNode, T> parser) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Accept", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .orTimeout(8, TimeUnit.SECONDS)
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new CompletionException(new IOException("HTTP " + response.statusCode()));
                    }
                    try {
                        JsonNode node = mapper.readTree(response.body());
                        T parsed = parser.apply(node);
                        if (parsed == null) {
                            throw new IllegalStateException("Empty API payload");
                        }
                        return parsed;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }

    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> getInFlight(Category category) {
        return (CompletableFuture<T>) inFlight.get(category);
    }

    private void cacheValue(Category category, Object value) {
        Deque<Object> deque = cache.computeIfAbsent(category, key -> new ArrayDeque<>());
        synchronized (deque) {
            deque.remove(value);
            deque.addFirst(value);
            while (deque.size() > CACHE_LIMIT) {
                deque.removeLast();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T randomCached(Category category) {
        Deque<Object> deque = cache.get(category);
        if (deque == null) {
            return null;
        }
        synchronized (deque) {
            if (deque.isEmpty()) {
                return null;
            }
            int targetIndex = ThreadLocalRandom.current().nextInt(deque.size());
            int currentIndex = 0;
            Iterator<Object> iterator = deque.iterator();
            while (iterator.hasNext()) {
                Object value = iterator.next();
                if (currentIndex == targetIndex) {
                    return (T) value;
                }
                currentIndex++;
            }
            return (T) deque.peekFirst();
        }
    }

    private RuntimeException asFriendlyException(Throwable throwable) {
        return new RuntimeException(FRIENDLY_ERROR, unwrap(throwable));
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException || current instanceof ExecutionException) {
            if (current.getCause() == null) {
                break;
            }
            current = current.getCause();
        }
        return current;
    }
}
