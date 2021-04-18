package net.voldrich.graal.async.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.voldrich.graal.async.script.AsyncScriptExecutor.JS_LANGUAGE_TYPE;

@Slf4j
public class ScriptUtils {

    public static Source parseScript(String script) {
        try {
            return Source.newBuilder(JS_LANGUAGE_TYPE, script, "script")
                    .cached(true)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse script", e);
        }
    }

    public static Value getGlobalMember(Context context, String name) {
        Value global = context.getBindings("js");
        return global.getMember(name);
    }

    public static Value getJSONMember(Context context) {
        return getGlobalMember(context, "JSON");
    }

    public static Value parseJson(Context context, String data) {
        return getJSONMember(context).getMember("parse").execute(data);
    }

    public static Value stringify(Context context, Object data) {
        return getJSONMember(context).getMember("stringify").execute(data);
    }

    public static String stringifyToString(Context context, Object data) {
        return stringify(context, data).toString();
    }

    public static Value stringifyPretty(Context context, Value data) {
        return getJSONMember(context).getMember("stringify").execute(data, null, 2);
    }

    public static Value getGlobalPromise(Context context) {
        return getGlobalMember(context, "Promise");
    }

    public static String getCurrentJsStack(Context context) {
        Value error = getGlobalMember(context, "Error").newInstance();
        return error.getMember("stack").toString();
    }

    public static void logValue(Value jsValue, int indent) {
        jsValue.getMemberKeys().forEach(key -> {
            StringBuilder sb = new StringBuilder();
            IntStream.range(0, indent).forEach(value -> sb.append("  "));
            log.info("{}{}", sb.toString(), key);
            logValue(jsValue.getMember(key), indent + 1);
        });
    }

    public static String getResourceFileAsString(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            if (is == null) return null;
            try (InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}
