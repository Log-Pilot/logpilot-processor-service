package com.logpilot.processor_service.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LogEvent(
        UUID eventId,
        String orgId,
        String service,
        String level,
        Instant timestamp,
        String message,
        String traceId,
        Map<String, Object> metadata
) {
}
