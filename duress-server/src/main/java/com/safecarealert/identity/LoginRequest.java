package com.safecarealert.identity;

public record LoginRequest(
        String serialNumber,
        String licenceKey,
        String deviceAppVersion
) {
}
