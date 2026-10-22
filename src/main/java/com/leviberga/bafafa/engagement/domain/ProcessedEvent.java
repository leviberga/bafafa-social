package com.leviberga.bafafa.engagement.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "processed_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    private String id;

    @Indexed(expireAfter = "30d")
    private Instant processedAt;

    public static ProcessedEvent of(String id, Instant now) {
        ProcessedEvent event = new ProcessedEvent();
        event.id = id;
        event.processedAt = now;
        return event;
    }
}