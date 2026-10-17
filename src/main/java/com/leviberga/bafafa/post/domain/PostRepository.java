package com.leviberga.bafafa.post.domain;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Limit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PostRepository extends MongoRepository<Post, ObjectId> {

    List<Post> findByAuthorIdAndDeletedAtIsNullOrderByIdDesc(String authorId, Limit limit);

    List<Post> findByAuthorIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
            String authorId, ObjectId before, Limit limit);

    List<Post> findByParentIdAndDeletedAtIsNullOrderByIdAsc(ObjectId parentId, Limit limit);

    List<Post> findByParentIdAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
            ObjectId parentId, ObjectId after, Limit limit);
}