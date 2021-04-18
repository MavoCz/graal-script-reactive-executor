package net.voldrich.graal.async.api;

import org.graalvm.polyglot.HostAccess;

import net.voldrich.graal.async.ScriptTestUtils;

public class ScriptMockedHttpResponse {
    @HostAccess.Export
    public final int status;
    @HostAccess.Export
    public final String data;

    public final int responseTimeoutMs;

    public ScriptMockedHttpResponse(int status, String responseResource, int responseTimeoutMs) {
        this.status = status;
        this.data = ScriptTestUtils.fromResource(responseResource);
        this.responseTimeoutMs = responseTimeoutMs;
    }

    public ScriptMockedHttpResponse(String data) {
        this.status = 200;
        this.data = data;
        this.responseTimeoutMs = 100;
    }
}
