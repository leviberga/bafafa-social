package com.leviberga.bafafa.outbox.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "outbox_messages")
@CompoundIndex(name = "claimable", def = "{'status': 1, 'availableAt': 1, '_id': 1}")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessage {

    public enum Status {PENDING, DONE, DEAD}

    @Id
    private ObjectId id;

    private String type;
    private String payload;
    private Status status;
    private int attempts;
    private Instant createdAt;
    private Instant availableAt;
    private Instant lockedUntil;

    /** Só mensagens concluídas expiram (7 dias); as em dead-letter ficam até alguém tratá-las. */
    @Indexed(expireAfter = "7d")
    private Instant processedAt;

    private String lastError;

    public static OutboxMessage pending(String type, String payload, Instant now) {
        OutboxMessage message = new OutboxMessage();
        message.type = type;
        message.payload = payload;
        message.status = Status.PENDING;
        message.createdAt = now;
        message.availableAt = now;
        return message;
    }
}