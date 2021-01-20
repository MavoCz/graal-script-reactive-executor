package net.voldrich.graal.async.script;

import org.graalvm.polyglot.HostAccess;

import java.util.function.Consumer;

public class PromiseConsumer<T> implements Consumer<T> {

    private final Consumer<T> callback;

    public PromiseConsumer(Consumer<T> callback) {
        this.callback = callback;
    }

    @Override
    @HostAccess.Export
    public void accept(T value) {
        callback.accept(value);
    }
}
