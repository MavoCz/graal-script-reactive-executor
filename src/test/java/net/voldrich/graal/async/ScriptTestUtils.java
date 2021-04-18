package net.voldrich.graal.async;

import net.voldrich.graal.async.script.ScriptUtils;
import org.graalvm.polyglot.Source;

import java.io.*;
import java.util.stream.Collectors;

public class ScriptTestUtils {

    public static Source sourceFromResource(String resourcePath) {
        return ScriptUtils.parseScript(stringFromResource(resourcePath));
    }

    public static String stringFromResource(String resourcePath) {
        try {
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
                if (is == null) return null;
                try (InputStreamReader isr = new InputStreamReader(is);
                     BufferedReader reader = new BufferedReader(isr)) {
                    return reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
