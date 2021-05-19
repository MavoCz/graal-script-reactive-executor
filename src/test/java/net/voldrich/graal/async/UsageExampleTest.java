package net.voldrich.graal.async;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.graal.async.api.ScriptTimeout;
import net.voldrich.graal.async.script.*;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static net.voldrich.graal.async.script.AsyncScriptExecutor.JS_LANGUAGE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class UsageExampleTest {
    @Test
    public void exampleUsingBaseScriptHandler() {
        AsyncScriptExecutor executor = new AsyncScriptExecutor.Builder().build();

        String script = "(function() { return 'Hello simple world' })";

        BaseScriptHandler scriptHandler = new BaseScriptHandler(ScriptUtils.parseScript(script));
        Mono<String> result = executor.executeScript(scriptHandler);

        String resultStr = result.block();
        log.info("Script finished {}", resultStr);

        assertEquals("Hello simple world", resultStr);
    }

    @Test
    public void exampleUsingBaseScriptHandlerWithAsyncOperation() {
        AsyncScriptExecutor executor = new AsyncScriptExecutor.Builder().build();

        String script = "(async function() { " +
                "var ret = await timeout.ms(50, 'async world'); " +
                "return 'Hello ' + ret + ' after timeout'; " +
                "})";

        BaseScriptHandler scriptHandler = new BaseScriptHandler(ScriptUtils.parseScript(script)) {
            @Override
            public void initiateContext(ScriptContext scriptContext) {
                Value bindings = scriptContext.getContext().getBindings(JS_LANGUAGE_TYPE);
                bindings.putMember("timeout", new ScriptTimeout(scriptContext));
            }
        };
        Mono<String> result = executor.executeScript(scriptHandler);

        String resultStr = result.block();
        log.info("Script finished {}", resultStr);

        assertEquals("Hello async world after timeout", resultStr);
    }
}
