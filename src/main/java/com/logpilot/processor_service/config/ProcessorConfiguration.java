package com.logpilot.processor_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logpilot.processor")
public record ProcessorConfiguration(
        String stream,
        String deadLetterStream,
        String consumerGroup,
        String consumerName,
        int batchSize,
        long pollDelayMs
) {
}
