package com.leviberga.bafafa.graph.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "follows")
@CompoundIndexes({
        @CompoundIndex(name = "follow_unique", def = "{'followerId': 1, 'followeeId': 1}", unique = true),
        @CompoundIndex(name = "followers_recent", def = "{'followeeId': 1, '_id': -1}")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow {

    @Id
    private ObjectId id;

    private String followerId;
    private String followeeId;
    private Instant createdAt;

    public static Follow of(String followerId, String followeeId, Instant now) {
        Follow follow = new Follow();
        follow.followerId = followerId;
        follow.followeeId = followeeId;
        follow.createdAt = now;
        return follow;
    }
}