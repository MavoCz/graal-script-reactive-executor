package net.voldrich.graal.async.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

// TODO implement Timeout when script is infinite and never ends ({response}) => new Promise(() => "Neverending story")
// TODO implement Statement limits
@Slf4j
class ScriptContextImpl implements ScriptContext {

    private final Context context;
    private final Source source;
    private final Scheduler scheduler;
    private final ByteArrayOutputStream scriptOutputStream;
    private boolean closed = false;

    /** Map of currently running async operations in this context. These are cancelled if context is closed. */
    private final ConcurrentHashMap<Subscription, PromiseMonoSubscriber> runningOperationMap = new ConcurrentHashMap<>();

    public ScriptContextImpl(Context context, Source source, Scheduler scheduler, ByteArrayOutputStream outputStream) {
        this.context = context;
        this.source = source;
        this.scheduler = scheduler;
        this.scriptOutputStream = outputStream;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public Value eval() {
        return context.eval(source);
    }

    @Override
    public void close() {
        this.close(false);
    }

    @Override
    public void forceClose() {
        this.close(true);
    }

    @Override
    public Value executeAsPromise(Mono<?> operation, String description) {
        String currentJsStack = ScriptUtils.getCurrentJsStack(context);
        return ScriptUtils.getGlobalPromise(context).newInstance((ProxyExecutable) arguments -> {
            Value resolve = arguments[0];
            Value reject = arguments[1];

            PromiseMonoSubscriber subscriber = new PromiseMonoSubscriber(this, resolve, reject,
                    description, currentJsStack);

            // Operation result needs to be published on a thread that executed the script.
            // This ensures that one particular script code is always executed on the same thread
            // and we don't need to manage context.enter and context.leave.
            operation
                    .publishOn(scheduler)
                    .subscribe(subscriber);
            return null;
        });
    }

    private synchronized void close(boolean force) {
        if (!closed) {
            log.debug("Closing context, force: {}", force);
            this.closed = true;
            this.context.close(force);
            if (!runningOperationMap.isEmpty()) {
                runningOperationMap.forEachEntry(1, subscriptionStringEntry -> {
                    subscriptionStringEntry.getValue().cancel();
                });
                runningOperationMap.clear();
            }
        }
    }

    @Override
    public String getScriptOutput() {
        return scriptOutputStream.toString(Charset.defaultCharset());
    }

    protected void registerSubscriber(Subscription subscription, PromiseMonoSubscriber promiseMonoSubscriber) {
        runningOperationMap.put(subscription, promiseMonoSubscriber);
    }

    protected void unregisterSubscriber(Subscription subscription) {
        runningOperationMap.remove(subscription);
    }
}
