package com.leviberga.bafafa.engagement.application;

import com.leviberga.bafafa.engagement.domain.Like;
import com.leviberga.bafafa.engagement.domain.LikeRepository;
import com.leviberga.bafafa.engagement.domain.PostStats;
import com.leviberga.bafafa.engagement.domain.PostStatsRepository;
import com.leviberga.bafafa.post.PostApi;
import com.leviberga.bafafa.ratelimit.RateLimitPolicy;
import com.leviberga.bafafa.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EngagementService {

    private static final RateLimitPolicy LIKE_POLICY = new RateLimitPolicy(60, Duration.ofMinutes(1));

    private final LikeRepository likes;
    private final PostStatsRepository stats;
    private final PostApi posts;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public record PostEngagement(long likeCount, long repostCount, boolean likedByMe, boolean repostedByMe) {
    }

    public void like(UUID userId, String rawPostId) {
        rateLimiter.acquire("engagement:like:" + userId, LIKE_POLICY);
        String postId = visiblePostId(rawPostId);

        try {
            likes.insert(Like.of(postId, userId.toString(), clock.instant()));
        } catch (DuplicateKeyException e) {
            return; // já curtido: a operação é idempotente e o contador não mexe
        }
        stats.incrementLikes(postId);
    }

    public void unlike(UUID userId, String rawPostId) {
        rateLimiter.acquire("engagement:like:" + userId, LIKE_POLICY);
        String postId = visiblePostId(rawPostId);

        if (likes.deleteByPostIdAndUserId(postId, userId.toString()) > 0) {
            stats.decrementLikes(postId);
        }
    }

    public PostEngagement get(String rawPostId, UUID viewerId) {
        String postId = visiblePostId(rawPostId);
        PostStats postStats = stats.findById(postId).orElse(null);
        long likeCount = postStats == null ? 0 : postStats.getLikeCount();
        long repostCount = postStats == null ? 0 : postStats.getRepostCount();
        boolean likedByMe = viewerId != null && likes.existsByPostIdAndUserId(postId, viewerId.toString());
        boolean repostedByMe = viewerId != null && posts.hasReposted(postId, viewerId.toString());
        return new PostEngagement(likeCount, repostCount, likedByMe, repostedByMe);
    }

    private String visiblePostId(String rawPostId) {
        return posts.resolveVisibleId(rawPostId).orElseThrow(EngagementErrors.PostNotFound::new);
    }
}