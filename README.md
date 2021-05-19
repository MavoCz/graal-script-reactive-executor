# graal-script-reactive-executor

Java library for executing Javascript scripts on GraalVM in async/nonblocking/reactive fashion.

Handles async calls from script bound to async operations executed in java on different thread.

Allows you to use 1 thread to serve multiple script executions at a time without blocking that
thread when executing asynchronous operations invoked by the script.
This makes it a better fit for reactive servers as Webflux or RSocket.


Example usage:
-------------

See unit test net.voldrich.graal.async.UsageExampleTest. 

Create and configure script async executor
     
    AsyncScriptExecutor executor = new AsyncScriptExecutor.Builder().build();

Use it to execute scripts. Script source and execution is managed by script handler.

    String script = "(async function() { " +
                "var ret = await timeout.ms(50, 'async world'); " +
                "return 'Hello ' + ret + ' after timeout'; " +
                "})";

    BaseScriptHandler scriptHandler = new BaseScriptHandler(ScriptUtils.parseScript(script)) {
        @Override
        public void initiateContext(ScriptContext scriptContext) {
            Value bindings = scriptContext.getContext().getBindings(JS_LANGUAGE_TYPE);
            bindings.putMember("timeout", new ScriptTimeout(scriptContext));
        }
    };
    Mono<String> result = executor.executeScript(scriptHandler);

    String resultStr = result.block();
    
Use result in normal reactive chain. Provided binding objects:

- have to implement all exported methods to the script as asynchronous and execute its logic on a different thread from script thread. 
- have to return Promise JS object created within script context which wraps the async java operation.
  
If they block the script thread the performance drops as with
any other reactive system. Use scriptContext interface to wrap operation Mono in Promise which is then returned
to the script. 

    public ScriptTimeout(ScriptContext scriptContext) {
        this.scriptContext = scriptContext;
    }

    @HostAccess.Export
    public Value ms(int timeoutMs, Value response) {
        Mono<Value> operation = Mono.delay(Duration.ofMillis(timeoutMs)).thenReturn(response);
        return scriptContext.executeAsPromise(operation, "timeout.ms for " + timeoutMs);
    }
}

Example script using the binding above:

    (async function test() {
        return await timeout.ms(100, [
            {
                "id": 1,
                "name": "Steve Jobs"
            },
            {
                "id": 2,
                "name": "Bob Balmer"
            }
        ]);
    })

Script execution in async world
-------------

GraalVM javascript engine provides support for async/promise handling. Its a bit difficult to implement as 
the documentation is vague and you have to dig the examples out of unit test code and one or two articles.
There are few limitations: 

- Only one thread can access the script context at a time.
- If some other thread wants to access the script, then it needs to call context.enter/leave.

This is easy to uphold standard blocking code: 

- script is evaluated on thread A
- script invokes an async binding method which is executed on thread B, thread A is blocked until B is done.
- operation finishes and thread A continues with script evaluation.

Note that thread A is blocked for the entire execution of the script. If the script is doing a lot of IO, then 
it waits most of its time and does nothing productive.

Things get a bit more difficult in reactive style:
- script 1 is evaluated on thread A
- script 1 invokes a binding method which is subscribed on thread B, thread A is released and serve next script 2 in line
- operation on thread B finishes and script 1 execution is subscribed back to thread A.
- As thread A is probably executing different script, the script 1 execution is queued on executor 
- Script 1 execution finishes 

