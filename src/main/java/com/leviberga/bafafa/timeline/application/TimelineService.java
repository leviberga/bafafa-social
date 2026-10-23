package com.leviberga.bafafa.timeline.application;

import com.leviberga.bafafa.engagement.EngagementApi;
import com.leviberga.bafafa.engagement.EngagementSummary;
import com.leviberga.bafafa.graph.GraphApi;
import com.leviberga.bafafa.identity.AccountSummary;
import com.leviberga.bafafa.identity.IdentityApi;
import com.leviberga.bafafa.post.PostApi;
import com.leviberga.bafafa.post.PostView;
import com.leviberga.bafafa.profile.AuthorCard;
import com.leviberga.bafafa.profile.ProfileApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private static final int MAX_LIMIT = 50;

    private final GraphApi graph;
    private final PostApi posts;
    private final ProfileApi profiles;
    private final EngagementApi engagement;
    private final IdentityApi identity;

    private record Entry(PostView candidate, PostView shown) {
    }

    public Timeline.Page home(UUID viewerId, String cursor, int limit) {
        Set<String> authors = new HashSet<>(graph.followingIds(viewerId.toString()));
        authors.add(viewerId.toString());
        return assemble(authors, viewerId, cursor, limit);
    }

    public Timeline.Page ofUser(String handle, UUID viewerId, String cursor, int limit) {
        AccountSummary account = identity.findByHandle(handle.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(TimelineErrors.AuthorNotFound::new);
        return assemble(Set.of(account.id().toString()), viewerId, cursor, limit);
    }

    private Timeline.Page assemble(Collection<String> authorIds, UUID viewerId, String cursor, int rawLimit) {
        int limit = Math.min(Math.max(rawLimit, 1), MAX_LIMIT);

        List<PostView> candidates = new ArrayList<>(posts.findFeedCandidates(authorIds, cursor, limit + 1));
        String nextCursor = null;
        if (candidates.size() > limit) {
            candidates = new ArrayList<>(candidates.subList(0, limit));
            nextCursor = candidates.getLast().id();
        }
        Set<String> originalIds = candidates.stream()
                .map(PostView::repostOfId).filter(id -> id != null).collect(Collectors.toSet());
        Map<String, PostView> originals = posts.findVisibleByIds(originalIds);

        List<Entry> entries = new ArrayList<>();
        for (PostView candidate : candidates) {
            PostView shown = candidate.repostOfId() == null ? candidate : originals.get(candidate.repostOfId());
            if (shown != null) {
                entries.add(new Entry(candidate, shown));
            }
        }

        Set<UUID> accountIds = new HashSet<>();
        for (Entry entry : entries) {
            accountIds.add(UUID.fromString(entry.shown().authorId()));
            if (entry.candidate().repostOfId() != null) {
                accountIds.add(UUID.fromString(entry.candidate().authorId()));
            }
        }
        Map<UUID, AuthorCard> cards = profiles.cards(accountIds);
        Map<String, EngagementSummary> summaries = engagement.summaries(
                entries.stream().map(entry -> entry.shown().id()).collect(Collectors.toSet()), viewerId);

        List<Timeline.Item> items = entries.stream()
                .map(entry -> toItem(entry, cards, summaries))
                .filter(item -> item != null)
                .toList();
        return new Timeline.Page(items, nextCursor);
    }

    private static Timeline.Item toItem(Entry entry, Map<UUID, AuthorCard> cards,
                                        Map<String, EngagementSummary> summaries) {
        PostView shown = entry.shown();
        AuthorCard author = cards.get(UUID.fromString(shown.authorId()));
        if (author == null) {
            return null;
        }
        EngagementSummary summary = summaries.get(shown.id());
        Timeline.PostCard post = new Timeline.PostCard(
                shown.id(), actor(author), shown.text(), shown.createdAt(),
                new Timeline.Engagement(summary.likeCount(), summary.repostCount(),
                        summary.likedByMe(), summary.repostedByMe()));

        if (entry.candidate().repostOfId() == null) {
            return new Timeline.Item("POST", post, null);
        }
        AuthorCard reposter = cards.get(UUID.fromString(entry.candidate().authorId()));
        return reposter == null ? null : new Timeline.Item("REPOST", post, actor(reposter));
    }

    private static Timeline.Actor actor(AuthorCard card) {
        return new Timeline.Actor(card.accountId().toString(), card.handle(), card.displayName());
    }
}