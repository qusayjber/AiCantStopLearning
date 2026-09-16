package search;

import utils.Log;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyless HTML search via DuckDuckGo.
 *
 * <p>Scrapes {@code https://html.duckduckgo.com/html/} and extracts result
 * titles, URLs and snippets. No API key required. Suitable for low-volume
 * research use; for production, implement a paid {@link SearchProvider}.
 */
public final class DuckDuckGoSearchProvider implements SearchProvider {

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private static final Pattern RESULT = Pattern.compile(
            "result__a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", Pattern.DOTALL);

    private static final Pattern SNIPPET = Pattern.compile(
            "result__snippet[^>]*>(.*?)</a>", Pattern.DOTALL);

    @Override
    public String name() { return "DuckDuckGo"; }

    @Override
    public CompletableFuture<List<SearchProvider.SearchResult>> search(String query, int max) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://html.duckduckgo.com/html/?q=" + encoded))
                    .header("User-Agent", "Mozilla/5.0 AICSL/1.0")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenApply(resp -> parse(resp.body(), max, query));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Parses DuckDuckGo HTML into structured results.
     *
     * @param html  raw HTML from html.duckduckgo.com
     * @param max   maximum number of results to return
     * @param query the original query — used only for logging
     */
    private List<SearchProvider.SearchResult> parse(String html, int max, String query) {
        List<SearchProvider.SearchResult> out = new ArrayList<>();

        // Extract all snippets first so we can pair them by index with results.
        List<String> snippets = new ArrayList<>();
        Matcher ms = SNIPPET.matcher(html);
        while (ms.find()) snippets.add(strip(ms.group(1)));

        Matcher mr = RESULT.matcher(html);
        int idx = 0;
        while (mr.find() && out.size() < max) {
            String rawUrl = mr.group(1);
            String url = decodeDdg(rawUrl);
            String title = strip(mr.group(2));
            String snippet = idx < snippets.size() ? snippets.get(idx) : "";
            idx++;

            if (url == null || !url.startsWith("http")) continue;
            if (out.stream().anyMatch(r -> r.url().equals(url))) continue;

            out.add(new SearchProvider.SearchResult(title, url, snippet));
        }

        Log.info("DDG search returned " + out.size() + " results for: " + query);
        return out;
    }

    /**
     * Decodes a DuckDuckGo redirect URL of the form
     * {@code /l/?uddg=<url-encoded>} back to the destination URL.
     */
    private static String decodeDdg(String u) {
        try {
            int i = u.indexOf("uddg=");
            if (i >= 0) {
                String tail = u.substring(i + 5);
                int amp = tail.indexOf('&');
                if (amp >= 0) tail = tail.substring(0, amp);
                return URLDecoder.decode(tail, StandardCharsets.UTF_8);
            }
            if (u.startsWith("//")) return "https:" + u;
            return u;
        } catch (Exception e) {
            return u;
        }
    }

    /** Removes HTML tags and unescapes common entities. */
    private static String strip(String html) {
        return html.replaceAll("<[^>]+>", "")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#x27;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }
}