package com.leviberga.bafafa.engagement.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostStatsRepository extends MongoRepository<PostStats, String>, PostStatsOperations {
}