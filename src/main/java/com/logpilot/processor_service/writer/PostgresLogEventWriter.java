package com.logpilot.processor_service.writer;

import com.logpilot.processor_service.model.LogEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
public class PostgresLogEventWriter {
    private static final String INSERT_SQL = """
            INSERT INTO log_events (event_id, org_id, service, level, event_timestamp, message, trace_id, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PostgresLogEventWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void write(LogEvent event) {
        try {
            String metadata = objectMapper.writeValueAsString(
                    event.metadata() == null ? java.util.Map.of() : event.metadata());
            jdbcTemplate.update(INSERT_SQL,
                    event.eventId(), event.orgId(), event.service(), event.level(),
                    event.timestamp(), event.message(), event.traceId(), metadata);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Could not serialize event metadata", exception);
        }
    }
}
