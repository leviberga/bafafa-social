package com.leviberga.bafafa.timeline.application;

import com.leviberga.bafafa.engagement.EngagementApi;
import com.leviberga.bafafa.engagement.EngagementSummary;
import com.leviberga.bafafa.graph.GraphApi;
import com.leviberga.bafafa.identity.AccountSummary;
import com.leviberga.bafafa.identity.IdentityApi;
import com.leviberga.bafafa.post.PostApi;
import com.leviberga.bafafa.post.PostView;
import com.leviberga.bafafa.post.PostViewPage;
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
import java.util.Objects;
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

    public Timeline.PostThread thread(String postId, UUID viewerId) {
        PostView post = findInteractable(postId);
        PostView parent = post.parentId() == null
                ? null
                : posts.findVisibleByIds(List.of(post.parentId())).get(post.parentId());

        Set<UUID> accountIds = new HashSet<>();
        accountIds.add(UUID.fromString(post.authorId()));
        if (parent != null) {
            accountIds.add(UUID.fromString(parent.authorId()));
        }
        Map<UUID, AuthorCard> cards = profiles.cards(accountIds);
        Map<String, EngagementSummary> summaries = engagement.summaries(Set.of(post.id()), viewerId);

        Timeline.PostCard card = toCard(post, cards, summaries);
        if (card == null) {
            throw new TimelineErrors.PostNotFound();
        }

        Timeline.ReplyContext context = null;
        if (post.parentId() != null) {
            AuthorCard parentAuthor = parent == null ? null : cards.get(UUID.fromString(parent.authorId()));
            context = new Timeline.ReplyContext(post.parentId(), parentAuthor == null ? null : actor(parentAuthor));
        }
        return new Timeline.PostThread(card, context);
    }

    public Timeline.Page replies(String postId, UUID viewerId, String cursor, int limit) {
        PostView post = findInteractable(postId);
        PostViewPage page = posts.listReplies(post.id(), cursor, limit);

        Set<UUID> authorIds = page.items().stream()
                .map(reply -> UUID.fromString(reply.authorId()))
                .collect(Collectors.toSet());
        Map<UUID, AuthorCard> cards = profiles.cards(authorIds);
        Map<String, EngagementSummary> summaries = engagement.summaries(
                page.items().stream().map(PostView::id).collect(Collectors.toSet()), viewerId);

        List<Timeline.Item> items = page.items().stream()
                .map(reply -> toCard(reply, cards, summaries))
                .filter(Objects::nonNull)
                .map(card -> new Timeline.Item("REPLY", card, null))
                .toList();
        return new Timeline.Page(items, page.nextCursor());
    }

    private PostView findInteractable(String postId) {
        return posts.findVisibleByIds(List.of(postId)).values().stream()
                .filter(post -> post.repostOfId() == null)
                .findFirst()
                .orElseThrow(TimelineErrors.PostNotFound::new);
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
                .map(PostView::repostOfId).filter(Objects::nonNull).collect(Collectors.toSet());
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
                .filter(Objects::nonNull)
                .toList();
        return new Timeline.Page(items, nextCursor);
    }

    private static Timeline.Item toItem(Entry entry, Map<UUID, AuthorCard> cards,
                                        Map<String, EngagementSummary> summaries) {
        Timeline.PostCard post = toCard(entry.shown(), cards, summaries);
        if (post == null) {
            return null;
        }
        if (entry.candidate().repostOfId() == null) {
            return new Timeline.Item("POST", post, null);
        }
        AuthorCard reposter = cards.get(UUID.fromString(entry.candidate().authorId()));
        return reposter == null ? null : new Timeline.Item("REPOST", post, actor(reposter));
    }

    private static Timeline.PostCard toCard(PostView shown, Map<UUID, AuthorCard> cards,
                                            Map<String, EngagementSummary> summaries) {
        AuthorCard author = cards.get(UUID.fromString(shown.authorId()));
        EngagementSummary summary = summaries.get(shown.id());
        if (author == null || summary == null) {
            return null;
        }
        return new Timeline.PostCard(
                shown.id(), actor(author), shown.text(), shown.createdAt(),
                new Timeline.Engagement(summary.likeCount(), summary.repostCount(),
                        summary.likedByMe(), summary.repostedByMe()));
    }

    private static Timeline.Actor actor(AuthorCard card) {
        return new Timeline.Actor(card.accountId().toString(), card.handle(), card.displayName());
    }
}