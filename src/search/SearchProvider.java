package search;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchProvider {
    String name();
    CompletableFuture<List<SearchResult>> search(String query, int max);
    record SearchResult(String title, String url, String snippet) {}
}