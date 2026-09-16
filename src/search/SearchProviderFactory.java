package search;

import settings.AppSettings;

public final class SearchProviderFactory {
    private SearchProviderFactory() {}
    public static SearchProvider from(AppSettings s) {
        return switch (s.searchKind()) {
            case "custom" -> new CustomSearchProvider(s.searchEndpoint(), s.searchKey());
            default -> new DuckDuckGoSearchProvider();
        };
    }
}