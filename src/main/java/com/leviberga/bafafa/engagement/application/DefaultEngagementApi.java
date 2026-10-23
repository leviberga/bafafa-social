package com.leviberga.bafafa.engagement.application;

import com.leviberga.bafafa.engagement.EngagementApi;
import com.leviberga.bafafa.engagement.EngagementSummary;
import com.leviberga.bafafa.engagement.domain.Like;
import com.leviberga.bafafa.engagement.domain.LikeRepository;
import com.leviberga.bafafa.engagement.domain.PostStats;
import com.leviberga.bafafa.engagement.domain.PostStatsRepository;
import com.leviberga.bafafa.post.PostApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class DefaultEngagementApi implements EngagementApi {

    private final PostStatsRepository stats;
    private final LikeRepository likes;
    private final PostApi posts;

    @Override
    public Map<String, EngagementSummary> summaries(Collection<String> postIds, UUID viewerId) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<String, PostStats> statsById = new HashMap<>();
        stats.findAllById(postIds).forEach(postStats -> statsById.put(postStats.getPostId(), postStats));

        String viewer = viewerId == null ? null : viewerId.toString();
        Set<String> liked = viewer == null ? Set.of()
                : likes.findByUserIdAndPostIdIn(viewer, postIds).stream().map(Like::getPostId).collect(Collectors.toSet());
        Set<String> reposted = viewer == null ? Set.of() : posts.repostedAmong(viewer, postIds);

        Map<String, EngagementSummary> result = new HashMap<>();
        for (String id : postIds) {
            PostStats postStats = statsById.get(id);
            result.put(id, new EngagementSummary(
                    postStats == null ? 0 : postStats.likeCountOrZero(),
                    postStats == null ? 0 : postStats.repostCountOrZero(),
                    liked.contains(id),
                    reposted.contains(id)));
        }
        return result;
    }
}