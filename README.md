# logpilot-processor-service — Stream Processing Engine

> **Consumes raw logs from Redis Streams, normalizes and enriches them, then writes to Apache Druid (analytics) and PostgreSQL (metadata).**

---

## 📋 Service Overview

| Property | Value |
|----------|-------|
| **Service Name** | `logpilot-processor-service` |
| **Technology** | Spring Boot 3.x, Java 21, Redis Streams Consumer, Apache Druid, PostgreSQL |
| **Type** | Backend Microservice (Stream Consumer) |
| **Port** | `8082` |
| **Communication** | Redis Streams (inbound) → Druid + PostgreSQL (outbound) |
| **Team Owner** | Backend Engineer #2 |
| **Build Priority** | Month 1, Week 3–4 |

---

## 🎯 Responsibilities

1. **Redis Stream Consumer** — Continuously reads from `logpilot:logs:{org_id}` streams using consumer groups
2. **Log Normalization** — Standardize timestamps, severity levels, and field names across different log formats
3. **Log Enrichment** — Add service metadata (service name, environment, host), assign correlation/trace IDs
4. **Druid Ingestion** — Write normalized log events to Apache Druid `log_events` datasource via Druid's HTTP ingestion API
5. **PostgreSQL Writes** — Write incident-related metadata, service health snapshots to PostgreSQL
6. **Dead Letter Queue** — Route unparseable/invalid logs to a DLQ Redis stream for manual inspection
7. **Backpressure Handling** — If Druid is slow, buffer in Redis and retry without losing data

---

## 📦 Codebase Structure

```
logpilot-processor-service/
├── src/
│   └── main/
│       ├── java/com/logpilot/processor/
│       │   ├── ProcessorApplication.java            # Spring Boot entry point
│       │   ├── config/
│       │   │   ├── RedisConsumerConfig.java          # Redis Streams consumer group setup
│       │   │   ├── DruidConfig.java                  # Druid connection settings
│       │   │   └── ProcessorConfig.java              # Processing tuning params
│       │   ├── consumer/
│       │   │   ├── LogStreamConsumer.java             # Main Redis consumer loop
│       │   │   └── ConsumerGroupManager.java          # Consumer group lifecycle
│       │   ├── pipeline/
│       │   │   ├── LogNormalizer.java                 # Normalize timestamp, level formats
│       │   │   ├── LogEnricher.java                   # Add service metadata, trace IDs
│       │   │   ├── LogRouter.java                     # Route to Druid / Postgres / DLQ
│       │   │   └── ProcessingPipeline.java            # Orchestrate normalization → enrichment → routing
│       │   ├── writer/
│       │   │   ├── DruidWriter.java                   # HTTP POST to Druid ingestion endpoint
│       │   │   ├── PostgresWriter.java                # JPA writes for metadata
│       │   │   └── DeadLetterWriter.java              # Write failed events to DLQ stream
│       │   ├── model/
│       │   │   ├── RawLogEvent.java                   # Event from Redis Stream
│       │   │   ├── NormalizedLogEvent.java             # After normalization
│       │   │   └── DruidIngestionPayload.java          # Druid-format payload
│       │   └── metrics/
│       │       └── ProcessorMetrics.java               # Processing rate, lag, errors
│       └── resources/
│           ├── application.yml
│           └── logback-spring.xml
├── src/test/java/
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 🔗 Dependencies

| Dependency | Purpose |
|------------|---------|
| Spring Boot Starter Data Redis | Redis Streams consumer |
| Spring Boot Starter Data JPA | PostgreSQL writes |
| Spring Boot Starter WebFlux | Non-blocking HTTP client for Druid API |
| Spring Boot Starter Actuator | Health, metrics |
| PostgreSQL Driver | Database connectivity |
| Jackson | JSON parsing/serialization |
| Micrometer + Prometheus | Processing lag and throughput metrics |

---

## 🔄 Processing Pipeline

```
Redis Stream → LogStreamConsumer → ProcessingPipeline:
                                    ├── LogNormalizer (standardize format)
                                    ├── LogEnricher (add metadata)
                                    └── LogRouter:
                                         ├── DruidWriter (time-series analytics)
                                         ├── PostgresWriter (metadata)
                                         └── DeadLetterWriter (failures)
```

---

## 🗄️ Data Stores Written To

| Store | What's Written | Schema |
|-------|---------------|--------|
| Apache Druid — `log_events` | All normalized log events | org_id, service_id, level, host, env, trace_id, message, timestamp |
| PostgreSQL — `service_health` | Periodic health snapshots | service_id, error_rate, p99_latency, log_volume, timestamp |
| Redis — `logpilot:dlq:{org_id}` | Failed/unparseable events | Raw event + error reason |

---

## ✅ Testing Checklist

- [ ] Unit tests for LogNormalizer (different timestamp formats, severity mappings)
- [ ] Unit tests for LogEnricher (metadata injection)
- [ ] Integration test: Redis stream → full pipeline → verify Druid payload
- [ ] DLQ test: malformed log → routed to dead letter queue
- [ ] Backpressure test: Druid unavailable → events buffered, not lost
- [ ] Consumer group test: multiple consumer instances sharing load
