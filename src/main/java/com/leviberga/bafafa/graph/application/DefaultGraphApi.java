package com.leviberga.bafafa.graph.application;

import com.leviberga.bafafa.graph.GraphApi;
import com.leviberga.bafafa.graph.domain.Follow;
import com.leviberga.bafafa.graph.domain.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class DefaultGraphApi implements GraphApi {

    private final FollowRepository follows;

    @Override
    public Set<String> followingIds(String accountId) {
        return follows.findByFollowerId(accountId).stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toSet());
    }
}