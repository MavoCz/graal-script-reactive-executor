package net.voldrich.graal.async.script;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.graal.async.ScriptTestUtils;
import net.voldrich.graal.async.api.ScriptMockedHttpClient;
import net.voldrich.graal.async.api.ScriptMockedHttpResponse;
import net.voldrich.graal.async.api.ScriptTimeout;
import org.graalvm.polyglot.Value;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.function.Consumer;

import static net.voldrich.graal.async.script.AsyncScriptExecutor.JS_LANGUAGE_TYPE;

@Slf4j
class AsyncScriptExecutorTest {

    private AsyncScriptExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new AsyncScriptExecutor.Builder().build();
    }

    @AfterEach
    void tearDown() {
        executor.getScriptSchedulers().dispose();
    }

    @Test
    void executeScript() {
        Mono<String> scriptMono = executeScript("scripts/test-script-timeout.js");

        StepVerifier.create(scriptMono)
                .expectNext("[{\"id\":1,\"name\":\"Steve Jobs\"},{\"id\":2,\"name\":\"Bob Balmer\"}]")
                .expectComplete()
                .verify();
    }

    @Test
    void executeScriptCancel() throws InterruptedException {
        Mono<String> scriptMono = executeScript("scripts/test-script-timeout.js");

        StepVerifier.create(scriptMono)
                .thenRequest(1)
                .thenAwait(Duration.ofMillis(50))
                .thenCancel()
                .verify();
    }

    private Mono<String> executeScript(String scriptResource) {
        return executeScript(scriptResource, scriptMockedHttpClient -> {});
    }

    private Mono<String> executeScript(String scriptResource, Consumer<ScriptMockedHttpClient> configureClient) {

        return executor.executeScript(
                ScriptTestUtils.fromResource(scriptResource),
                scriptContext -> {
                    Value bindings = scriptContext.getContext().getBindings(JS_LANGUAGE_TYPE);
                    bindings.putMember("timeout", new ScriptTimeout(scriptContext));
                    ScriptMockedHttpClient client = new ScriptMockedHttpClient(scriptContext);
                    configureClient.accept(client);
                    bindings.putMember("client", client);
                });
    }

    @Test
    void testScriptErrorSyntax() {
        Mono<String> stringMono = executeScript("scripts/test-script-error-syntax.js");
        StepVerifier.create(stringMono)
                .expectErrorSatisfies(verifyScriptException(
                        Matchers.startsWith("org.graalvm.polyglot.PolyglotException: SyntaxError: script:2:30 Expected"), null))
                .verify();
    }

    @Test
    void testScriptErrorEval() {
        Mono<String> stringMono = executeScript("scripts/test-script-error-eval.js");
        StepVerifier.create(stringMono)
                .expectErrorSatisfies(verifyScriptException(
                        Matchers.startsWith("ReferenceError: company is not defined"), null))
                .verify();
    }

    @Test
    void testScriptWithUsingClient() {
        Mono<String> stringMono = executeScript("scripts/test-http-get.js", client -> {
            client.addResponse("/company/info", new ScriptMockedHttpResponse(200, "json/company-info.json", 50));
            client.addResponse("/company/ceo", new ScriptMockedHttpResponse(200, "json/ceo-list.json", 100));
        });
        StepVerifier.create(stringMono)
                .consumeNextWith(verifyJsonMatchesResource("json/expected-response-client.json"))
                .verifyComplete();
    }

    @Test
    void testScriptWithUsingClientParralel() {
        Mono<String> stringMono = executeScript("scripts/test-http-get-parralel.js", client -> {
            client.addResponse("/company/info", new ScriptMockedHttpResponse(200, "json/company-info.json", 100));
        });
        StepVerifier.create(stringMono)
                .consumeNextWith(verifyJsonMatchesResource("json/expected-response-parralel.json"))
                .verifyComplete();
    }

    @Test
    void testScriptWithUsingClientException() {
        Mono<String> stringMono = executeScript("scripts/test-http-get.js", client -> {
            client.addResponse("/company/info", new ScriptMockedHttpResponse(200, "json/company-info.json", 50));
        });
        StepVerifier.create(stringMono)
                .expectErrorSatisfies(verifyScriptException(
                        Matchers.startsWith("java.lang.RuntimeException: 404 NOT FOUND"),
                        Matchers.startsWith("Status for company info: 200")))
                .verify();
    }

    private Consumer<String> verifyJsonMatchesResource(String resourceName) {
        return json -> {
            log.debug("Received next: {}", json);
            try {
                JSONAssert.assertEquals(ScriptTestUtils.fromResource(resourceName), json, false);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        };
    }

    private Consumer<Throwable> verifyScriptException(Matcher<String> messageMatcher, Matcher<String> outputMatcher) {
        return exception -> {
            if (!(exception instanceof ScriptExecutionException)) {
                throw new AssertionError("ScriptExecutionException expected");
            }

            ScriptExecutionException scriptException = (ScriptExecutionException) exception;

            if (!messageMatcher.matches(scriptException.getMessage())) {
                throw new AssertionError("Message matcher " + messageMatcher.toString()
                        + " failed to match '" + scriptException.getMessage() + "'");
            }

            if (outputMatcher != null) {
                if (!outputMatcher.matches(scriptException.getScriptOutput())) {
                    throw new AssertionError("Output matcher " + outputMatcher.toString()
                            + " failed to match '" + scriptException.getScriptOutput() + "'");
                }
            }
        };
    }

}