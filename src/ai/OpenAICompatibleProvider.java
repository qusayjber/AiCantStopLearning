package ai;

import utils.Json;
import utils.Log;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** OpenAI-compatible chat completions client (works with OpenAI, LM Studio, Ollama w/ compat, etc.). */
public final class OpenAICompatibleProvider implements AIProvider {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20)).build();
    private final String endpoint, apiKey, model;

    public OpenAICompatibleProvider(String endpoint, String apiKey, String model) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length()-1) : endpoint;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override public String name() { return "OpenAI-compatible"; }

    @Override
    public CompletableFuture<String> complete(String system, String user, double temperature, int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Missing API key. Configure AI provider in Settings."));
        }
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/chat/completions"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body)))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() / 100 != 2) {
                        throw new RuntimeException("AI HTTP " + resp.statusCode() + ": " +
                                Log.redact(truncate(resp.body(), 300)));
                    }
                    Map<String,Object> json = Json.asMap(Json.parse(resp.body()));
                    List<Object> choices = Json.asList(json.get("choices"));
                    if (choices.isEmpty()) throw new RuntimeException("AI returned no choices");
                    Map<String,Object> choice = Json.asMap(choices.get(0));
                    Map<String,Object> msg = Json.asMap(choice.get("message"));
                    String content = Json.str(msg, "content");
                    if (content == null) throw new RuntimeException("AI returned empty content");
                    return content;
                });
    }

    private static String truncate(String s, int n) { return s.length() <= n ? s : s.substring(0, n) + "..."; }
}