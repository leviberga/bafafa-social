package com.leviberga.bafafa.engagement.application;

import com.leviberga.bafafa.engagement.PostLiked;
import com.leviberga.bafafa.engagement.PostUnliked;
import com.leviberga.bafafa.engagement.domain.PostStatsRepository;
import com.leviberga.bafafa.engagement.domain.ProcessedEvent;
import com.leviberga.bafafa.engagement.domain.ProcessedEventRepository;
import com.leviberga.bafafa.outbox.MongoTransactions;
import com.leviberga.bafafa.post.PostReposted;
import com.leviberga.bafafa.post.RepostRemoved;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.function.IntConsumer;

/**
 * Aplica os eventos do outbox aos contadores. A entrega é at-least-once, então cada evento só tem efeito uma vez:
 * o registro do evento e o ajuste do contador acontecem na mesma transação. Exceções propagam de propósito,
 * para o relay tentar de novo.
 */
@Component
class CounterProjector {

    private final ProcessedEventRepository processed;
    private final PostStatsRepository stats;
    private final MongoTransactions mongoTx;
    private final Clock clock;

    CounterProjector(ProcessedEventRepository processed, PostStatsRepository stats,
                     MongoTransactions mongoTx, Clock clock) {
        this.processed = processed;
        this.stats = stats;
        this.mongoTx = mongoTx;
        this.clock = clock;
    }

    @EventListener
    void on(PostLiked event) {
        applyOnce("liked:" + event.likeId(), delta -> stats.adjustLikes(event.postId(), delta), 1);
    }

    @EventListener
    void on(PostUnliked event) {
        applyOnce("unliked:" + event.likeId(), delta -> stats.adjustLikes(event.postId(), delta), -1);
    }

    @EventListener
    void on(PostReposted event) {
        applyOnce("reposted:" + event.repostId(), delta -> stats.adjustReposts(event.originalPostId(), delta), 1);
    }

    @EventListener
    void on(RepostRemoved event) {
        applyOnce("unreposted:" + event.repostId(), delta -> stats.adjustReposts(event.originalPostId(), delta), -1);
    }

    private void applyOnce(String eventKey, IntConsumer adjustment, int delta) {
        if (processed.existsById(eventKey)) {
            return;
        }
        try {
            mongoTx.run(() -> {
                processed.insert(ProcessedEvent.of(eventKey, clock.instant()));
                adjustment.accept(delta);
            });
        } catch (DuplicateKeyException e) {
            // Outra entrega do mesmo evento já foi aplicada.
        }
    }
}