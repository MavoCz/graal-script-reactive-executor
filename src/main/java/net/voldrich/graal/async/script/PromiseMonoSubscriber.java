package net.voldrich.graal.async.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

@Slf4j
public class PromiseMonoSubscriber extends BaseSubscriber<Object> {
    private final ScriptContextImpl scriptContextImpl;
    private final Value resolve;
    private final Value reject;
    private final String operationDesc;
    private final String jsStack;

    private volatile Subscription subscription;

    public PromiseMonoSubscriber(ScriptContextImpl scriptContextImpl, Value resolve, Value reject, String operationDesc, String jsStack) {
        this.scriptContextImpl = scriptContextImpl;
        this.resolve = resolve;
        this.reject = reject;
        this.operationDesc = operationDesc;
        this.jsStack = jsStack;
    }

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        super.hookOnSubscribe(subscription);
        this.subscription = subscription;
        scriptContextImpl.registerSubscriber(subscription, this);
    }

    @Override
    protected void hookOnNext(Object value) {
        // we resolve the promise when receiving first value
        resolve.executeVoid(value);
    }

    @Override
    protected void hookOnError(Throwable error) {
        if (error instanceof ScriptExecutionException) {
            reject.executeVoid(error);
        } else {
            reject.executeVoid(new ScriptExecutionException(scriptContextImpl, error, operationDesc + "\n" + jsStack));
        }
        scriptContextImpl.unregisterSubscriber(subscription);
    }

    @Override
    protected void hookOnCancel() {
        log.debug("Operation cancelled: {}", operationDesc);
    }

    @Override
    protected void hookOnComplete() {
        scriptContextImpl.unregisterSubscriber(subscription);
    }

}
