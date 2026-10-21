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
        increment(postId, "likeCount");
    }

    @Override
    public void decrementLikes(String postId) {
        decrement(postId, "likeCount");
    }

    @Override
    public void incrementReposts(String postId) {
        increment(postId, "repostCount");
    }

    @Override
    public void decrementReposts(String postId) {
        decrement(postId, "repostCount");
    }

    private void increment(String postId, String field) {
        mongo.upsert(
                Query.query(Criteria.where("_id").is(postId)),
                new Update().inc(field, 1),
                PostStats.class);
    }

    private void decrement(String postId, String field) {
        mongo.updateFirst(
                Query.query(Criteria.where("_id").is(postId).and(field).gt(0)),
                new Update().inc(field, -1),
                PostStats.class);
    }
}