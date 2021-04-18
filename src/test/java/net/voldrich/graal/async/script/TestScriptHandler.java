package net.voldrich.graal.async.script;

import net.voldrich.graal.async.ScriptTestUtils;
import net.voldrich.graal.async.api.MockedHttpClient;
import net.voldrich.graal.async.api.ScriptMockedHttpClient;
import net.voldrich.graal.async.api.ScriptTimeout;
import org.graalvm.polyglot.Value;

import static net.voldrich.graal.async.script.AsyncScriptExecutor.JS_LANGUAGE_TYPE;

public class TestScriptHandler extends BaseScriptHandler {

    private final MockedHttpClient mockedHttpClient;

    public TestScriptHandler(String scriptResource, MockedHttpClient mockedHttpClient) {
        super(ScriptTestUtils.sourceFromResource(scriptResource));
        this.mockedHttpClient = mockedHttpClient;
    }

    @Override
    public void initiateContext(ScriptContext scriptContext) {
        Value bindings = scriptContext.getContext().getBindings(JS_LANGUAGE_TYPE);
        bindings.putMember("timeout", new ScriptTimeout(scriptContext));
        if (mockedHttpClient != null) {
            ScriptMockedHttpClient client = new ScriptMockedHttpClient(scriptContext, mockedHttpClient);
            bindings.putMember("client", client);
        }
    }
}
