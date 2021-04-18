package net.voldrich.graal.async.api;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.graal.async.script.ScriptContext;
import reactor.core.publisher.Mono;

@Slf4j
public class ScriptMockedHttpClient {
    private final ScriptContext scriptContext;

    private final Map<String, ScriptMockedHttpResponse> urlToResponseMap = new HashMap<>();

    public ScriptMockedHttpClient(ScriptContext scriptContext) {
        this.scriptContext = scriptContext;
    }

    public void addResponse(String url, ScriptMockedHttpResponse response) {
        this.urlToResponseMap.put(url, response);
    }

    @HostAccess.Export
    public Value get(String url) {
        ScriptMockedHttpResponse response = urlToResponseMap.get(url);
        if (response == null) {
            throw new RuntimeException("404 NOT FOUND");
        }
        Mono<ScriptMockedHttpResponse> operation = Mono.delay(Duration.ofMillis(response.responseTimeoutMs))
                .thenReturn(response)
                .doOnSubscribe(subscription -> log.trace("Request {} started", url))
                .doOnSuccess(result -> log.trace("Request {} finished", url));
        return scriptContext.executeAsPromise(operation, "get " + url);
    }
}
