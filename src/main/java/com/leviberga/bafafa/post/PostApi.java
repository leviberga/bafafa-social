package com.leviberga.bafafa.post;

import java.util.*;

public interface PostApi {

    Optional<String> resolveVisibleId(String rawPostId);

    boolean hasReposted(String canonicalPostId, String userId);

    List<PostView> findFeedCandidates(Collection<String> authorIds, String beforeCursor, int limit);

    Map<String, PostView> findVisibleByIds(Collection<String> postIds);

    Set<String> repostedAmong(String userId, Collection<String> originalPostIds);
}