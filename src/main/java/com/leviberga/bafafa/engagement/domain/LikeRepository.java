package com.leviberga.bafafa.engagement.domain;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LikeRepository extends MongoRepository<Like, ObjectId> {

    boolean existsByPostIdAndUserId(String postId, String userId);

    long deleteByPostIdAndUserId(String postId, String userId);
}