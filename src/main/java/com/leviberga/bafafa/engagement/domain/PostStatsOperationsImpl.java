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
    public void adjustLikes(String postId, int delta) {
        adjust(postId, "likeCount", delta);
    }

    @Override
    public void adjustReposts(String postId, int delta) {
        adjust(postId, "repostCount", delta);
    }

    private void adjust(String postId, String field, int delta) {
        mongo.upsert(
                Query.query(Criteria.where("_id").is(postId)),
                new Update().inc(field, delta),
                PostStats.class);
    }
}