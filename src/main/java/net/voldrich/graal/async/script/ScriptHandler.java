package net.voldrich.graal.async.script;

import org.graalvm.polyglot.Value;

public interface ScriptHandler<T> {

    void initiateContext(ScriptContext scriptContext);

    Value evaluateScript(ScriptContext scriptContext);

    T transformScriptResponse(ScriptContext scriptContext, Object value);
}
