package com.leviberga.bafafa.outbox.infra;

import com.leviberga.bafafa.outbox.OutboxEvent;
import com.leviberga.bafafa.outbox.domain.OutboxMessage;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
class OutboxRelay {

    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);

    private final MongoTemplate mongo;
    private final ApplicationEventPublisher events;
    private final JsonMapper json;
    private final OutboxProperties props;
    private final MeterRegistry meters;
    private final Clock clock;

    OutboxRelay(MongoTemplate mongo, ApplicationEventPublisher events, JsonMapper json,
                OutboxProperties props, MeterRegistry meters, Clock clock) {
        this.mongo = mongo;
        this.events = events;
        this.json = json;
        this.props = props;
        this.meters = meters;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${bafafa.outbox.poll-interval:PT0.5S}")
    void poll() {
        for (int i = 0; i < props.batchSize(); i++) {
            OutboxMessage message = claimNext();
            if (message == null) {
                return;
            }
            process(message);
        }
    }

    /** Reserva a próxima mensagem de forma atômica, então várias instâncias não entregam a mesma duas vezes. */
    private OutboxMessage claimNext() {
        Instant now = clock.instant();
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("status").is(OutboxMessage.Status.PENDING),
                Criteria.where("availableAt").lte(now),
                new Criteria().orOperator(
                        Criteria.where("lockedUntil").is(null),
                        Criteria.where("lockedUntil").lt(now))))
                .with(Sort.by(Sort.Direction.ASC, "id"));
        Update update = new Update()
                .set("lockedUntil", now.plus(props.lease()))
                .inc("attempts", 1);
        return mongo.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true),
                OutboxMessage.class);
    }

    private void process(OutboxMessage message) {
        try {
            dispatch(message);
            markDone(message);
        } catch (Exception e) {
            markFailed(message, e);
        }
    }

    private void dispatch(OutboxMessage message) throws ClassNotFoundException {
        Class<?> type = Class.forName(message.getType(), false, getClass().getClassLoader());
        if (!OutboxEvent.class.isAssignableFrom(type)) {
            throw new IllegalStateException("Tipo não permitido no outbox: " + message.getType());
        }
        Object event = json.readValue(message.getPayload(), type);
        events.publishEvent(event); // os consumidores rodam aqui; se lançarem exceção, haverá retentativa
    }

    private void markDone(OutboxMessage message) {
        mongo.updateFirst(byId(message.getId()),
                new Update()
                        .set("status", OutboxMessage.Status.DONE)
                        .set("processedAt", clock.instant())
                        .unset("lockedUntil"),
                OutboxMessage.class);
        count("processed");
    }

    private void markFailed(OutboxMessage message, Exception error) {
        String reason = truncate(error.toString());
        Update update = new Update().set("lastError", reason).unset("lockedUntil");

        if (message.getAttempts() >= props.maxAttempts()) {
            update.set("status", OutboxMessage.Status.DEAD);
            log.error("Outbox: mensagem {} foi para dead-letter após {} tentativas: {}",
                    message.getId(), message.getAttempts(), reason);
            count("dead");
        } else {
            update.set("availableAt", clock.instant().plus(backoff(message.getAttempts())));
            log.warn("Outbox: falha ao entregar a mensagem {} (tentativa {}): {}",
                    message.getId(), message.getAttempts(), reason);
            count("retry");
        }
        mongo.updateFirst(byId(message.getId()), update, OutboxMessage.class);
    }

    private Duration backoff(int attempts) {
        long factor = 1L << Math.min(Math.max(attempts - 1, 0), 10);
        Duration delay = props.retryBaseDelay().multipliedBy(factor);
        return delay.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : delay;
    }

    private static Query byId(ObjectId id) {
        return Query.query(Criteria.where("_id").is(id));
    }

    private static String truncate(String text) {
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    private void count(String result) {
        meters.counter("bafafa.outbox", "result", result).increment();
    }
}