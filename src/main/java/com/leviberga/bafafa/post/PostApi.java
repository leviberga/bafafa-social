package com.leviberga.bafafa.post;

import java.util.Optional;

public interface PostApi {

    Optional<String> resolveVisibleId(String rawPostId);
}