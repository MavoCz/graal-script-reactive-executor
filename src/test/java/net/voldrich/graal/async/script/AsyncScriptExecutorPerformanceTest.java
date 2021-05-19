package net.voldrich.graal.async.script;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import net.voldrich.graal.async.api.MockedHttpClient;
import net.voldrich.graal.async.api.ScriptMockedHttpResponse;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;

import java.util.concurrent.TimeUnit;

@Disabled
@Slf4j
public class AsyncScriptExecutorPerformanceTest {
    private AsyncScriptExecutor executor;

    private static final int numberOfWarmupRequests = 100;
    private static final int numberOfRequests = 100000;

    //private static final int concurrency = 10;
    private static final int concurrency = Queues.SMALL_BUFFER_SIZE;

    protected final MetricRegistry metrics = new MetricRegistry();

    protected Timer scriptTimer = metrics.timer("script-execution");

    private MockedHttpClient mockedHttpClient;

    @BeforeAll
    static void beforeAll() {
        LoggingMeterRegistry loggingMeterRegistry = new LoggingMeterRegistry();
        Metrics.addRegistry(loggingMeterRegistry);
        Schedulers.enableMetrics();
    }

    @BeforeEach
    void setUp() {
        executor = new AsyncScriptExecutor.Builder().build();
        mockedHttpClient = new MockedHttpClient();
        mockedHttpClient.addResponse("/company/info", new ScriptMockedHttpResponse(200, "json/company-info.json", 50));
        mockedHttpClient.addResponse("/company/ceo", new ScriptMockedHttpResponse(200, "json/ceo-list.json", 50));
    }

    @AfterEach
    void tearDown() {
        executor.getScriptSchedulers().dispose();
    }

    @Test
    void smallPerfTest() {
        Mono<String> script = executeScript("scripts/test-http-get.js");
        runScript(script, numberOfWarmupRequests, numberOfRequests);
    }

    private Mono<String> executeScript(String scriptResource) {
        return executor.executeScript(new TestScriptHandler(scriptResource, mockedHttpClient));
    }

    private void runScript(Mono<String> script, int warmupRequestCount, int requestCount) {
        runScript(script, warmupRequestCount);
        runScriptWithTimer(script, requestCount);

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.MILLISECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.report();
    }

    private void runScript(Mono<String> scriptMono, int requestCount) {
        Flux.range(1, requestCount)
                .flatMap(integer -> scriptMono)
                .blockLast();
    }

    private void runScriptWithTimer(Mono<String> scriptMono, int requestCount) {
        Flux.range(1, requestCount)
                .flatMap(integer -> Mono.using(
                        () -> scriptTimer.time(),
                        context -> scriptMono,
                        Timer.Context::stop), concurrency)
                .blockLast();
    }
}
