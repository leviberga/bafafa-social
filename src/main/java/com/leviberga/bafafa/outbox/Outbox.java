package com.leviberga.bafafa.outbox;

import com.leviberga.bafafa.outbox.domain.OutboxMessage;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

@Component
public class Outbox {

    private final MongoTemplate mongo;
    private final JsonMapper json;
    private final Clock clock;

    public Outbox(MongoTemplate mongo, JsonMapper json, Clock clock) {
        this.mongo = mongo;
        this.json = json;
        this.clock = clock;
    }

    /** Grava o evento na mesma transação do chamador; só será entregue se ela for confirmada. */
    public void append(OutboxEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Outbox.append precisa rodar dentro de MongoTransactions");
        }
        mongo.insert(OutboxMessage.pending(
                event.getClass().getName(), json.writeValueAsString(event), clock.instant()));
    }
}