package ai;

import java.util.concurrent.CompletableFuture;

/** Abstraction over AI completion backends. */
public interface AIProvider {
    String AIProviderFactory = null;
	String name();
    CompletableFuture<String> complete(String system, String user, double temperature, int maxTokens);
}