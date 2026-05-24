package com.safecarealert.model;

public enum Status {
    ACTIVE("A"),
    INACTIVE("I"),
    PENDING("P"),
    SUSPENDED("S"),
    DELETED("D");

    private final String code;

    Status(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Status fromCode(String code) {
        // implementation...
        for (Status status : Status.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}