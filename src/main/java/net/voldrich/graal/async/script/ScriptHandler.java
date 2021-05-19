package net.voldrich.graal.async.script;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.time.ZoneId;

/**
 * Handles the script from initiation, execution to response transformation.
 **/
public interface ScriptHandler<T> {

    /**
     * Initiate script context builder.
     **/
    default Context.Builder initiateContextBuilder(Context.Builder contextBuilder) {
        return contextBuilder.timeZone(ZoneId.of("UTC"));
    }

    /**
     * Initiate script context bindings and settings. Called before script evaluation
     **/
    void initiateContext(ScriptContext scriptContext);

    /**
     * Use initiated context to evaluate script(s)
     * If returned value is a promise then that promise is resolved.
     **/
    Value evaluateScript(ScriptContext scriptContext);

    /**
     * Transform value returned by the script into T
     **/
    T transformScriptResponse(ScriptContext scriptContext, Object value);
}
