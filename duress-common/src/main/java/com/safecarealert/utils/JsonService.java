package com.safecarealert.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JsonService {
    @Inject
    ObjectMapper mapper;

    public String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    public <T> T fromJson(String json, Class<T> t) {
        try {
            return mapper.readValue(json, t);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid DeviceMessage data: " + json, e
            );
        }
    }

    public <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return mapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize complex type: " + json, e);
        }
    }

}