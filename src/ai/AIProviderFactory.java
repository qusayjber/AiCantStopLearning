package ai;

import settings.AppSettings;

public final class AIProviderFactory {
    private AIProviderFactory() {}
    public static AIProvider from(AppSettings s) {
        return new OpenAICompatibleProvider(s.aiEndpoint(), s.aiKey(), s.aiModel());
    }
}