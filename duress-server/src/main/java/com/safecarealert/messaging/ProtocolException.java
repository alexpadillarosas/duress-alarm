package com.safecarealert.messaging;

import com.safecarealert.core.TerminationReason;

public class ProtocolException extends RuntimeException {

    private final TerminationReason reason;

    public ProtocolException(TerminationReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public TerminationReason reason() {
        return reason;
    }
}
