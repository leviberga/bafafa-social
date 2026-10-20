package com.leviberga.bafafa.engagement.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "likes")
@CompoundIndexes({
        @CompoundIndex(name = "post_user_unique", def = "{'postId': 1, 'userId': 1}", unique = true),
        @CompoundIndex(name = "user_recent", def = "{'userId': 1, '_id': -1}")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like {

    @Id
    private ObjectId id;

    private String postId;
    private String userId;
    private Instant createdAt;

    public static Like of(String postId, String userId, Instant now) {
        Like like = new Like();
        like.postId = postId;
        like.userId = userId;
        like.createdAt = now;
        return like;
    }
}