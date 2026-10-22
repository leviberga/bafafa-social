package com.leviberga.bafafa.engagement.domain;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LikeRepository extends MongoRepository<Like, ObjectId> {

    boolean existsByPostIdAndUserId(String postId, String userId);

    long deleteByPostIdAndUserId(String postId, String userId);

    Optional<Like> findByPostIdAndUserId(String postId, String userId);
}