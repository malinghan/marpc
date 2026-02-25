package com.malinghan.marpc.context;

import java.util.HashMap;
import java.util.Map;

public class RpcContext {
    private static final ThreadLocal<Map<String, String>> CONTEXT =
            ThreadLocal.withInitial(HashMap::new);

    public static void set(String key, String value) {
        CONTEXT.get().put(key, value);
    }

    public static String get(String key) {
        return CONTEXT.get().get(key);
    }

    public static Map<String, String> getAll() {
        return new HashMap<>(CONTEXT.get());
    }

    public static void setAll(Map<String, String> params) {
        CONTEXT.get().putAll(params);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static void setGrayId(String grayId) {
        set("grayId", grayId);
    }

    public static String getGrayId() {
        return get("grayId");
    }
}
