package com.leviberga.bafafa.graph.domain;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FollowRepository extends MongoRepository<Follow, ObjectId> {

    boolean existsByFollowerIdAndFolloweeId(String followerId, String followeeId);

    long deleteByFollowerIdAndFolloweeId(String followerId, String followeeId);

    long countByFollowerId(String followerId);

    long countByFolloweeId(String followeeId);

    List<Follow> findByFollowerId(String followerId);
}