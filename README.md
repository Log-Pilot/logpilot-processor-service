# LogPilot Processor Service

The first processor iteration consumes events from the shared Redis stream, validates the ingest contract, and stores events in PostgreSQL. It is the persistence stage used by the upcoming query API.

## Current behavior

- Reads `logpilot:logs` through the `logpilot-processors` consumer group.
- Expects each stream entry to contain an `event` field with snake_case JSON emitted by `logpilot-ingest-service`.
- Validates required event fields and stores valid events in `log_events`.
- Uses `event_id` as the primary key and `ON CONFLICT DO NOTHING` for safe duplicate delivery.
- Acknowledges a valid event only after its database write succeeds.
- Sends malformed events to `logpilot:logs:dlq`, then acknowledges the original entry.
- Runs Flyway migration `V1__create_log_events.sql` at startup.

## Local configuration

Start PostgreSQL and Redis using the Compose setup in `logpilot-infra`, then run this service:

```sh
sh ./mvnw spring-boot:run
```

Defaults match the local Compose configuration: PostgreSQL at `localhost:5432` (`logpilot` database and user) and Redis at `localhost:6379`. Override them with `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `REDIS_HOST`, or `REDIS_PORT`.

Consumer settings can be changed with `LOG_STREAM`, `LOG_DLQ_STREAM`, `LOG_CONSUMER_GROUP`, `LOG_CONSUMER_NAME`, `LOG_BATCH_SIZE`, and `LOG_POLL_DELAY_MS`.

## Stored schema

`log_events` stores the event ID, organization, service, level, event timestamp, message, trace ID, metadata as JSONB, and database ingestion time. Indexes support common organization/time, service/time, and level/time filters.

## Scope and limitations

This iteration uses one configured consumer name and reads new group entries. It does not yet reclaim abandoned pending entries after a process crash, expose processing metrics, enrich logs, or send data to Druid. Infrastructure and end-to-end behavior require a running Redis and PostgreSQL; Docker is needed if using the provided local Compose setup.
