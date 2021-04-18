package net.voldrich.graal.async.script;

import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import static net.voldrich.graal.async.script.ScriptUtils.stringifyToString;

public class BaseScriptHandler implements ScriptHandler<String> {

    private final Source source;

    public BaseScriptHandler(Source source) {
        this.source = source;
    }

    @Override
    public void initiateContext(ScriptContext scriptContext) {
        // do nothing
    }

    @Override
    public Value evaluateScript(ScriptContext scriptContext) {
        Value response = scriptContext.getContext().eval(source);

        if (response.canExecute()) {
            response = response.execute();
        }

        return response;
    }

    @Override
    public String transformScriptResponse(ScriptContext context, Object value) {
        return stringifyToString(context.getContext(), value);
    }
}
