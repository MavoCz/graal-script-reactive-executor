package net.voldrich.graal.async.script;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public interface ScriptContext extends AutoCloseable {

    Value eval();

    Value executeAsPromise(Mono<?> operation, String description);

    Context getContext();

    String getScriptOutput();

    Scheduler getScheduler();

    boolean isClosed();

    @Override
    void close();

    void forceClose();

}
