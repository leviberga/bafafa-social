package com.leviberga.bafafa.engagement.application;

import com.leviberga.bafafa.engagement.domain.PostStatsRepository;
import com.leviberga.bafafa.post.PostReposted;
import com.leviberga.bafafa.post.RepostRemoved;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class RepostCounterListener {

    private final PostStatsRepository stats;

    RepostCounterListener(PostStatsRepository stats) {
        this.stats = stats;
    }

    @EventListener
    void on(PostReposted event) {
        try {
            stats.incrementReposts(event.originalPostId());
        } catch (RuntimeException e) {
            log.error("Contador de reposts defasado (post {})", event.originalPostId(), e);
        }
    }

    @EventListener
    void on(RepostRemoved event) {
        try {
            stats.decrementReposts(event.originalPostId());
        } catch (RuntimeException e) {
            log.error("Contador de reposts defasado (post {})", event.originalPostId(), e);
        }
    }
}