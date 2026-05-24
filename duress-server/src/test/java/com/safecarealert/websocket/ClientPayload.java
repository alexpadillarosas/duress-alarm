package com.safecarealert.websocket;

import java.util.Map;

public final class ClientPayload {

    private final Map<String, Object> data;

    public ClientPayload(Map<String, Object> data) {
        this.data = data;
    }

    public String getString(String key) {
        Object v = data.get(key);
        if (v == null) return null;
        if (v instanceof String s) return s;
        throw new IllegalArgumentException("Field '" + key + "' must be a string");
    }

    public Integer getInt(String key) {
        Object v = data.get(key);
        if (v instanceof Integer i) return i;
        if (v instanceof Number n) return n.intValue();
        throw new IllegalArgumentException("Field '" + key + "' must be an integer");
    }

    public Boolean getBoolean(String key) {
        Object v = data.get(key);
        if (v instanceof Boolean b) return b;
        throw new IllegalArgumentException("Field '" + key + "' must be a boolean");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getObject(String key) {
        Object v = data.get(key);
        if (v instanceof Map<?,?> m) return (Map<String, Object>) m;
        throw new IllegalArgumentException("Field '" + key + "' must be an object");
    }

    public Map<String, Object> raw() {
        return data;
    }
}
