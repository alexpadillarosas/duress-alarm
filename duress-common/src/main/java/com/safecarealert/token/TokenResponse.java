package com.safecarealert.token;


import java.util.Set;

/**
 * Token response sent to clients (including duress-client)
 */
public record TokenResponse(
        String token,
        String subjectId,
        Set<String> roles   // Keep as String for JSON compatibility
) {

}