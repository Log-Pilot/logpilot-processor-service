package com.logpilot.processor_service.consumer;

import com.logpilot.processor_service.config.ProcessorConfiguration;
import com.logpilot.processor_service.model.LogEvent;
import com.logpilot.processor_service.writer.PostgresLogEventWriter;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class LogStreamConsumer {
    private static final String CREATE_GROUP_SCRIPT = """
            local result = redis.pcall('XGROUP', 'CREATE', KEYS[1], ARGV[1], '0', 'MKSTREAM')
            if type(result) == 'table' and result.err and string.find(result.err, 'BUSYGROUP') then
              return 0
            end
            if type(result) == 'table' and result.err then
              return redis.error_reply(result.err)
            end
            return 1
            """;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProcessorConfiguration configuration;
    private final PostgresLogEventWriter writer;

    public LogStreamConsumer(StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper,
                             ProcessorConfiguration configuration,
                             PostgresLogEventWriter writer) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.configuration = configuration;
        this.writer = writer;
    }

    @PostConstruct
    void createConsumerGroup() {
        redisTemplate.execute(new DefaultRedisScript<>(CREATE_GROUP_SCRIPT, Long.class),
                List.of(configuration.stream()), configuration.consumerGroup());
    }

    @Scheduled(fixedDelayString = "${logpilot.processor.poll-delay-ms:1000}")
    public void poll() {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(configuration.consumerGroup(), configuration.consumerName()),
                StreamReadOptions.empty().count(configuration.batchSize()),
                StreamOffset.create(configuration.stream(), ReadOffset.lastConsumed()));
        if (records == null) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            process(record);
        }
    }

    private void process(MapRecord<String, Object, Object> record) {
        Object rawEvent = record.getValue().get("event");
        try {
            if (!(rawEvent instanceof String json)) {
                throw new IllegalArgumentException("Redis record is missing its event JSON field");
            }
            LogEvent event = objectMapper.readValue(json, LogEvent.class);
            validate(event);
            writer.write(event);
            acknowledge(record);
        } catch (JacksonException | IllegalArgumentException exception) {
            sendToDeadLetter(record, rawEvent, exception.getMessage());
            acknowledge(record);
        }
    }

    private void validate(LogEvent event) {
        if (event.eventId() == null || event.orgId() == null || event.orgId().isBlank()
                || event.service() == null || event.service().isBlank()
                || event.level() == null || event.level().isBlank()
                || event.timestamp() == null || event.message() == null || event.message().isBlank()) {
            throw new IllegalArgumentException("Event is missing a required field");
        }
    }

    private void sendToDeadLetter(MapRecord<String, Object, Object> record, Object rawEvent, String reason) {
        redisTemplate.opsForStream().add(configuration.deadLetterStream(), Map.of(
                "source_stream", configuration.stream(),
                "source_id", record.getId().getValue(),
                "event", rawEvent == null ? "" : rawEvent.toString(),
                "error", reason == null ? "Invalid event" : reason,
                "failed_at", java.time.Instant.now().toString()));
    }

    private void acknowledge(MapRecord<String, Object, Object> record) {
        redisTemplate.opsForStream().acknowledge(configuration.stream(),
                configuration.consumerGroup(), record.getId());
    }
}
