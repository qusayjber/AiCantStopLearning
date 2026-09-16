package search;

import utils.Json;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Generic JSON search endpoint. Expects:
 *   GET {endpoint}?q=<query>&count=<n>
 *   Header Authorization: Bearer <key>
 *   Response JSON: { "results": [ { "title":..,"url":..,"snippet":.. }, ... ] }
 */
public final class CustomSearchProvider implements SearchProvider {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private final String endpoint, key;
    public CustomSearchProvider(String endpoint, String key) { this.endpoint = endpoint; this.key = key; }

    @Override public String name() { return "Custom"; }

    @Override
    public CompletableFuture<List<SearchResult>> search(String query, int max) {
        if (endpoint == null || endpoint.isBlank())
            return CompletableFuture.failedFuture(new IllegalStateException("Custom search endpoint not configured"));
        try {
            String url = endpoint + (endpoint.contains("?") ? "&" : "?") +
                    "q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&count=" + max;
            HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30)).GET();
            if (key != null && !key.isBlank()) b.header("Authorization", "Bearer " + key);
            return http.sendAsync(b.build(), HttpResponse.BodyHandlers.ofString()).thenApply(r -> {
                if (r.statusCode() / 100 != 2)
                    throw new RuntimeException("Search HTTP " + r.statusCode());
                List<SearchResult> out = new ArrayList<>();
                for (Object o : Json.asList(Json.asMap(Json.parse(r.body())).get("results"))) {
                    Map<String,Object> m = Json.asMap(o);
                    out.add(new SearchResult(Json.str(m, "title"), Json.str(m, "url"), Json.str(m, "snippet")));
                }
                return out;
            });
        } catch (Exception e) { return CompletableFuture.failedFuture(e); }
    }
}