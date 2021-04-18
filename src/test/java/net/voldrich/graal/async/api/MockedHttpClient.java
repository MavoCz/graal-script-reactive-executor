package net.voldrich.graal.async.api;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MockedHttpClient {
    private final Map<String, ScriptMockedHttpResponse> urlToResponseMap = new HashMap<>();

    public void addResponse(String url, ScriptMockedHttpResponse response) {
        this.urlToResponseMap.put(url, response);
    }

    public Mono<ScriptMockedHttpResponse> get(String url) {
        ScriptMockedHttpResponse response = urlToResponseMap.get(url);
        if (response == null) {
            throw new RuntimeException("404 NOT FOUND");
        }
        return Mono.delay(Duration.ofMillis(response.responseTimeoutMs))
                .thenReturn(response)
                .doOnSubscribe(subscription -> log.trace("Request {} started", url))
                .doOnSuccess(result -> log.trace("Request {} finished", url));
    }
}
