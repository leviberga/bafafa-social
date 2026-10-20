package com.leviberga.bafafa.engagement.domain;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class PostStatsOperationsImpl implements PostStatsOperations {

    private final MongoTemplate mongo;

    PostStatsOperationsImpl(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void incrementLikes(String postId) {
        mongo.upsert(
                Query.query(Criteria.where("_id").is(postId)),
                new Update().inc("likeCount", 1),
                PostStats.class);
    }

    @Override
    public void decrementLikes(String postId) {
        mongo.updateFirst(
                Query.query(Criteria.where("_id").is(postId).and("likeCount").gt(0)),
                new Update().inc("likeCount", -1),
                PostStats.class);
    }
}