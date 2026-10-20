package com.leviberga.bafafa.post.application;

import com.leviberga.bafafa.post.PostApi;
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
            return Optional.of(posts.get(rawPostId).getId().toHexString());
        } catch (PostErrors.PostNotFound e) {
            return Optional.empty();
        }
    }
}