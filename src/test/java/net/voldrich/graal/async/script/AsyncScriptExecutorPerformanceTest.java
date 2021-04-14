package net.voldrich.graal.async.script;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.voldrich.graal.async.ScriptTestUtils;
import net.voldrich.graal.async.api.ScriptMockedHttpClient;
import net.voldrich.graal.async.api.ScriptMockedHttpResponse;
import net.voldrich.graal.async.api.ScriptTimeout;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static net.voldrich.graal.async.script.AsyncScriptExecutor.JS_LANGUAGE_TYPE;

@Disabled
public class AsyncScriptExecutorPerformanceTest {
    private AsyncScriptExecutor executor;

    private static Timer scriptTimer;

    @BeforeAll
    static void beforeAll() {
        scriptTimer = Timer
                .builder("my.timer")
                .description("a description of what this timer does") // optional
                .register(Metrics.globalRegistry);
        Schedulers.enableMetrics();
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
    }

    @BeforeEach
    void setUp() {
        executor = new AsyncScriptExecutor.Builder().build();
    }

    @AfterEach
    void tearDown() {
        executor.getScriptSchedulers().dispose();
    }

    @Test
    void smallPerfTest() {
        Mono<String> script = executeScript("scripts/test-http-get.js", client -> {
            client.addResponse("/company/info", new ScriptMockedHttpResponse(200, "json/company-info.json", 50));
            client.addResponse("/company/ceo", new ScriptMockedHttpResponse(200, "json/ceo-list.json", 50));
        });
        runScript(script, 100, 2000);
    }

    private Mono<String> executeScript(String scriptResource, Consumer<ScriptMockedHttpClient> configureClient) {
        String script = ScriptTestUtils.fromResource(scriptResource);
        return executor.executeScript(
                script,
                scriptContext -> {
                    Value bindings = scriptContext.getContext().getBindings(JS_LANGUAGE_TYPE);
                    bindings.putMember("timeout", new ScriptTimeout(scriptContext));
                    ScriptMockedHttpClient client = new ScriptMockedHttpClient(scriptContext);
                    configureClient.accept(client);
                    bindings.putMember("client", client);
                });
    }

    private void runScript(Mono<String> script, int warmupRequestCount, int requestCount) {
        runScript(script, warmupRequestCount);
        runScriptWithTimer(script, requestCount);

        /*ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.MILLISECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.report();*/
    }

    private void runScript(Mono<String> scriptMono, int requestCount) {
        Flux.range(1, requestCount)
                .flatMap(integer -> scriptMono)
                .blockLast();
    }

    private void runScriptWithTimer(Mono<String> scriptMono, int requestCount) {
        Flux.range(1, requestCount)
                .flatMap(integer -> scriptMono.name("script-execution").metrics())
                .blockLast();
    }
}
