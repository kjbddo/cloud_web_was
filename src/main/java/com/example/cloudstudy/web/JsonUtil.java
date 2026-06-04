package com.example.cloudstudy.web;

import java.util.Map;
import java.util.stream.Collectors;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static String object(Map<?, ?> values) {
        return values.entrySet().stream()
                .map(entry -> quote(String.valueOf(entry.getKey())) + ":" + value(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map) {
            return object((Map<?, ?>) value);
        }
        if (value instanceof Iterable) {
            StringBuilder json = new StringBuilder();
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    json.append(",");
                }
                json.append(value(item));
                first = false;
            }
            return "[" + json + "]";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }
}
