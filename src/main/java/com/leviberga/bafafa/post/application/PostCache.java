package com.leviberga.bafafa.post.application;

import com.leviberga.bafafa.post.domain.Post;
import org.bson.types.ObjectId;

import java.util.Optional;

public interface PostCache {

    Optional<Post> find(ObjectId id);

    void put(Post post);

    void evict(ObjectId id);
}