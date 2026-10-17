package com.leviberga.bafafa.post.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "posts")
@CompoundIndexes({
        @CompoundIndex(name = "author_newest", def = "{'authorId': 1, '_id': -1}"),
        @CompoundIndex(name = "replies_oldest", def = "{'parentId': 1, '_id': 1}")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    private ObjectId id;

    private String authorId;
    private PostType type;
    private ObjectId parentId;
    private ObjectId rootId;
    private String text;
    private Instant createdAt;
    private Instant deletedAt;

    public static Post create(String authorId, String text, Instant now) {
        Post post = new Post();
        post.authorId = authorId;
        post.type = PostType.POST;
        post.text = text;
        post.createdAt = now;
        return post;
    }

    public static Post reply(String authorId, Post parent, String text, Instant now) {
        Post post = new Post();
        post.authorId = authorId;
        post.type = PostType.REPLY;
        post.parentId = parent.id;
        // rootId aponta sempre para o primeiro post da conversa, mesmo em respostas de respostas.
        post.rootId = parent.rootId != null ? parent.rootId : parent.id;
        post.text = text;
        post.createdAt = now;
        return post;
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
    }
}