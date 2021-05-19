package net.voldrich.graal.async.script;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Manages graal polyglot context and its related data.
 * Each script execution had its own unique context.
 * Instances are managed by the script executor, auto closed when script resolution is finished.
 **/
public interface ScriptContext extends AutoCloseable {

    Value executeAsPromise(Mono<?> operation, String description);

    Context getContext();

    String getScriptOutput();

    Scheduler getScheduler();

    boolean isClosed();

    @Override
    void close();

    void forceClose();

    void setTransactionId();

    String getTransactionId();
}
