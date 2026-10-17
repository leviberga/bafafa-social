package com.leviberga.bafafa.post.application;

import com.leviberga.bafafa.identity.AccountSummary;
import com.leviberga.bafafa.identity.IdentityApi;
import com.leviberga.bafafa.post.domain.Post;
import com.leviberga.bafafa.post.domain.PostRepository;
import com.leviberga.bafafa.ratelimit.RateLimitPolicy;
import com.leviberga.bafafa.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MAX_LIMIT = 100;
    private static final RateLimitPolicy CREATE_POLICY = new RateLimitPolicy(10, Duration.ofMinutes(1));
    private static final RateLimitPolicy REPLY_POLICY = new RateLimitPolicy(30, Duration.ofMinutes(1));


    private final PostRepository posts;
    private final IdentityApi identity;
    private final Clock clock;
    private final RateLimiter rateLimiter;



    public record PostPage(List<Post> items, String nextCursor) {
    }

    public Post create(UUID authorId, String text) {
        rateLimiter.acquire("post:create:" + authorId, CREATE_POLICY);
        return posts.save(Post.create(authorId.toString(), text.trim(), clock.instant()));
    }

    public Post reply(UUID authorId, String parentId, String text) {
        rateLimiter.acquire("post:reply:" + authorId, REPLY_POLICY);
        Post parent = findVisible(parentId);
        return posts.save(Post.reply(authorId.toString(), parent, text.trim(), clock.instant()));
    }

    public Post get(String id) {
        return findVisible(id);
    }

    public void delete(UUID requesterId, String id) {
        Post post = findVisible(id);
        if (!post.getAuthorId().equals(requesterId.toString())) {
            throw new PostErrors.NotOwner();
        }
        post.markDeleted(clock.instant());
        posts.save(post);
    }

    public PostPage listReplies(String postId, String cursor, int limit) {
        Post parent = findVisible(postId);
        int size = clamp(limit);
        Limit fetch = Limit.of(size + 1); // um a mais, só para saber se existe próxima página

        List<Post> found = isBlank(cursor)
                ? posts.findByParentIdAndDeletedAtIsNullOrderByIdAsc(parent.getId(), fetch)
                : posts.findByParentIdAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
                parent.getId(), parseCursor(cursor), fetch);
        return toPage(found, size);
    }

    public PostPage listByAuthor(String handle, String cursor, int limit) {
        AccountSummary author = identity.findByHandle(handle.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(PostErrors.AuthorNotFound::new);
        String authorId = author.id().toString();
        int size = clamp(limit);
        Limit fetch = Limit.of(size + 1);

        List<Post> found = isBlank(cursor)
                ? posts.findByAuthorIdAndDeletedAtIsNullOrderByIdDesc(authorId, fetch)
                : posts.findByAuthorIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
                authorId, parseCursor(cursor), fetch);
        return toPage(found, size);
    }

    private Post findVisible(String id) {
        if (!ObjectId.isValid(id)) {
            throw new PostErrors.PostNotFound();
        }
        return posts.findById(new ObjectId(id))
                .filter(post -> post.getDeletedAt() == null)
                .orElseThrow(PostErrors.PostNotFound::new);
    }

    private static PostPage toPage(List<Post> found, int size) {
        if (found.size() > size) {
            List<Post> items = found.subList(0, size);
            return new PostPage(items, items.getLast().getId().toHexString());
        }
        return new PostPage(found, null);
    }

    private static ObjectId parseCursor(String cursor) {
        if (!ObjectId.isValid(cursor)) {
            throw new PostErrors.InvalidCursor();
        }
        return new ObjectId(cursor);
    }

    private static int clamp(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}