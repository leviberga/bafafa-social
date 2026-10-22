package com.leviberga.bafafa.outbox;

import com.leviberga.bafafa.TestcontainersConfiguration;
import com.leviberga.bafafa.outbox.domain.OutboxMessage;
import com.leviberga.bafafa.outbox.infra.OutboxProperties;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OutboxRelayIntegrationTest {

    @Autowired
    Outbox outbox;

    @Autowired
    MongoTransactions tx;

    @Autowired
    PingListener listener;

    @Autowired
    MongoTemplate mongo;

    @Autowired
    OutboxProperties props;

    @Test
    void eventsAppendedInATransactionAreDelivered() {
        String value = "ping-" + UUID.randomUUID();

        tx.run(() -> outbox.append(new TestPing(value)));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertTrue(listener.received.contains(value)));
    }

    @Test
    void rolledBackTransactionsLeaveNoEventBehind() {
        String value = "rollback-" + UUID.randomUUID();

        assertThrows(IllegalStateException.class, () -> tx.run(() -> {
            outbox.append(new TestPing(value));
            throw new IllegalStateException("desfaz tudo");
        }));

        long found = mongo.count(Query.query(Criteria.where("payload").regex(value)), OutboxMessage.class);
        assertEquals(0, found);
    }

    @Test
    void failedDeliveriesAreRetried() {
        String value = "retry-" + UUID.randomUUID();
        listener.failOnce.add(value);

        tx.run(() -> outbox.append(new TestPing(value)));

        // A primeira entrega falha; a segunda acontece depois do backoff (cerca de 1 s).
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertTrue(listener.received.contains(value)));
    }

    @Test
    void unknownOrForbiddenTypesEndUpInDeadLetter() {
        ObjectId unknown = insertWithLastAttempt("com.leviberga.bafafa.NaoExiste", "{}");
        ObjectId forbidden = insertWithLastAttempt("java.lang.String", "\"texto\"");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertEquals(OutboxMessage.Status.DEAD, mongo.findById(unknown, OutboxMessage.class).getStatus());
            assertEquals(OutboxMessage.Status.DEAD, mongo.findById(forbidden, OutboxMessage.class).getStatus());
        });
    }

    /** Insere uma mensagem que já gastou todas as tentativas menos uma, para não esperar o backoff. */
    private ObjectId insertWithLastAttempt(String type, String payload) {
        ObjectId id = new ObjectId();
        Date now = new Date();
        mongo.insert(new Document()
                .append("_id", id)
                .append("type", type)
                .append("payload", payload)
                .append("status", "PENDING")
                .append("attempts", props.maxAttempts() - 1)
                .append("createdAt", now)
                .append("availableAt", now), "outbox_messages");
        return id;
    }
}