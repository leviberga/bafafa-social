package com.leviberga.bafafa.graph.application;

import com.leviberga.bafafa.graph.domain.Follow;
import com.leviberga.bafafa.graph.domain.FollowRepository;
import com.leviberga.bafafa.identity.IdentityApi;
import com.leviberga.bafafa.ratelimit.RateLimitPolicy;
import com.leviberga.bafafa.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    static final int MAX_FOLLOWING = 2000;
    private static final RateLimitPolicy POLICY = new RateLimitPolicy(60, Duration.ofMinutes(1));

    private final FollowRepository follows;
    private final IdentityApi identity;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public record FollowStats(long followerCount, long followingCount, boolean followedByMe) {
    }

    public void follow(UUID followerId, String handle) {
        rateLimiter.acquire("graph:follow:" + followerId, POLICY);
        String followee = resolve(handle);
        String follower = followerId.toString();

        if (follower.equals(followee)) {
            throw new GraphErrors.CannotFollowSelf();
        }
        if (follows.existsByFollowerIdAndFolloweeId(follower, followee)) {
            return; // idempotente
        }
        if (follows.countByFollowerId(follower) >= MAX_FOLLOWING) {
            throw new GraphErrors.FollowLimitReached();
        }
        try {
            follows.insert(Follow.of(follower, followee, clock.instant()));
        } catch (DuplicateKeyException e) {
            // Requisição simultânea do mesmo usuário: o resultado é o mesmo.
        }
    }

    public void unfollow(UUID followerId, String handle) {
        rateLimiter.acquire("graph:follow:" + followerId, POLICY);
        follows.deleteByFollowerIdAndFolloweeId(followerId.toString(), resolve(handle));
    }

    public FollowStats stats(String handle, UUID viewerId) {
        String account = resolve(handle);
        boolean followedByMe = viewerId != null
                && follows.existsByFollowerIdAndFolloweeId(viewerId.toString(), account);
        return new FollowStats(follows.countByFolloweeId(account), follows.countByFollowerId(account), followedByMe);
    }

    private String resolve(String handle) {
        return identity.findByHandle(handle.trim().toLowerCase(Locale.ROOT))
                .map(account -> account.id().toString())
                .orElseThrow(GraphErrors.ProfileNotFound::new);
    }
}