package net.voldrich.graal.async.script;

import lombok.Getter;

@Getter
public class ScriptExecutionException extends RuntimeException {

    private final String scriptOutput;

    private final String source;

    private final String stack;

    public ScriptExecutionException(ScriptContext scriptContext, String message) {
        super(message);
        this.source = "unknown";
        this.stack = "";
        this.scriptOutput = scriptContext.getScriptOutput();
    }

    public ScriptExecutionException(ScriptContext scriptContext, String message, String stack) {
        super(message);
        this.source = "script";
        this.stack = stack;
        this.scriptOutput = scriptContext.getScriptOutput();
    }

    public ScriptExecutionException(ScriptContext scriptContext, Throwable hostException) {
        this(scriptContext, hostException, "");
    }

    public ScriptExecutionException(ScriptContext scriptContext, Throwable hostException, String stack) {
        super(hostException);
        this.source = "host";
        this.stack = stack;
        this.scriptOutput = scriptContext.getScriptOutput();
    }

}
