package com.leviberga.bafafa.post.application;

import com.leviberga.bafafa.post.PostApi;
import com.leviberga.bafafa.post.PostView;
import com.leviberga.bafafa.post.domain.Post;
import com.leviberga.bafafa.post.domain.PostRepository;
import com.leviberga.bafafa.post.domain.PostType;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class DefaultPostApi implements PostApi {

    private static final List<PostType> FEED_TYPES = List.of(PostType.POST, PostType.REPOST);

    private final PostService posts;
    private final PostRepository repository;

    @Override
    public Optional<String> resolveVisibleId(String rawPostId) {
        try {
            Post post = posts.get(rawPostId);
            if (post.getType() == PostType.REPOST) {
                return Optional.empty(); // quem quer curtir ou repostar usa o original
            }
            return Optional.of(post.getId().toHexString());
        } catch (PostErrors.PostNotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean hasReposted(String canonicalPostId, String userId) {
        return posts.hasReposted(canonicalPostId, userId);
    }

    @Override
    public List<PostView> findFeedCandidates(Collection<String> authorIds, String beforeCursor, int limit) {
        if (authorIds.isEmpty()) {
            return List.of();
        }
        Limit fetch = Limit.of(limit);
        List<Post> found = (beforeCursor == null || beforeCursor.isBlank())
                ? repository.findByAuthorIdInAndTypeInAndDeletedAtIsNullOrderByIdDesc(authorIds, FEED_TYPES, fetch)
                : repository.findByAuthorIdInAndTypeInAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
                authorIds, FEED_TYPES, parseCursor(beforeCursor), fetch);
        return found.stream().map(DefaultPostApi::toView).toList();
    }

    @Override
    public Map<String, PostView> findVisibleByIds(Collection<String> postIds) {
        Map<String, PostView> result = new HashMap<>();
        repository.findAllById(toObjectIds(postIds)).stream()
                .filter(post -> post.getDeletedAt() == null)
                .forEach(post -> result.put(post.getId().toHexString(), toView(post)));
        return result;
    }

    @Override
    public Set<String> repostedAmong(String userId, Collection<String> originalPostIds) {
        List<ObjectId> ids = toObjectIds(originalPostIds);
        if (ids.isEmpty()) {
            return Set.of();
        }
        return repository.findByAuthorIdAndRepostOfIdIn(userId, ids).stream()
                .map(repost -> repost.getRepostOfId().toHexString())
                .collect(Collectors.toSet());
    }

    private static ObjectId parseCursor(String cursor) {
        if (!ObjectId.isValid(cursor)) {
            throw new PostErrors.InvalidCursor();
        }
        return new ObjectId(cursor);
    }

    private static List<ObjectId> toObjectIds(Collection<String> ids) {
        return ids.stream().filter(ObjectId::isValid).map(ObjectId::new).toList();
    }

    private static PostView toView(Post post) {
        return new PostView(post.getId().toHexString(), post.getAuthorId(), post.getType().name(),
                hex(post.getParentId()), hex(post.getRootId()), post.getText(), post.getCreatedAt(),
                hex(post.getRepostOfId()));
    }

    private static String hex(ObjectId id) {
        return id == null ? null : id.toHexString();
    }
}