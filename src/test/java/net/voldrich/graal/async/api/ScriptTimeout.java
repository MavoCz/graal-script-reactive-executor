package net.voldrich.graal.async.api;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.graal.async.script.ScriptExecutionException;
import net.voldrich.graal.async.script.ScriptContext;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
public class ScriptTimeout {

    private final ScriptContext scriptContext;

    public ScriptTimeout(ScriptContext scriptContext) {
        this.scriptContext = scriptContext;
    }

    @HostAccess.Export
    public Value ms(int timeoutMs, Value response) {
        Mono<Value> operation = Mono.delay(Duration.ofMillis(timeoutMs)).thenReturn(response);
        return scriptContext.executeAsPromise(operation, "timeout.ms for " + timeoutMs);
    }

    @HostAccess.Export
    public Value blockSleep(int timeoutMs, Value response) {
        try {
            Thread.sleep(timeoutMs);
            return response;
        } catch (InterruptedException e) {
            throw new ScriptExecutionException(scriptContext, "Timeout interrupted");
        }
    }

}
