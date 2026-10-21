package com.leviberga.bafafa.post.application;

import com.leviberga.bafafa.post.PostApi;
import com.leviberga.bafafa.post.domain.Post;
import com.leviberga.bafafa.post.domain.PostType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class DefaultPostApi implements PostApi {

    private final PostService posts;

    @Override
    public Optional<String> resolveVisibleId(String rawPostId) {
        try {
            Post post = posts.get(rawPostId);
            if (post.getType() == PostType.REPOST) {
                return Optional.empty();
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

}