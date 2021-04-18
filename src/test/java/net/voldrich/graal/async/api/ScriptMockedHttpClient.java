package net.voldrich.graal.async.api;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.graal.async.script.ScriptContext;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

@Slf4j
public class ScriptMockedHttpClient {
    private final ScriptContext scriptContext;
    private MockedHttpClient mockedHttpClient;

    public ScriptMockedHttpClient(ScriptContext scriptContext, MockedHttpClient mockedHttpClient) {
        this.scriptContext = scriptContext;
        this.mockedHttpClient = mockedHttpClient;
    }

    @HostAccess.Export
    public Value get(String url) {
        return scriptContext.executeAsPromise(mockedHttpClient.get(url), "get " + url);
    }
}
