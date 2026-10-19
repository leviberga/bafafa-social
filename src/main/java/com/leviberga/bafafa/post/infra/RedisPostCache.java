package com.leviberga.bafafa.post.infra;

import com.leviberga.bafafa.post.application.PostCache;
import com.leviberga.bafafa.post.domain.Post;
import com.leviberga.bafafa.post.domain.PostType;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@EnableConfigurationProperties(PostCacheProperties.class)
class RedisPostCache implements PostCache {

    private static final String KEY_PREFIX = "post:v1:";

    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final PostCacheProperties props;
    private final MeterRegistry meters;

    RedisPostCache(StringRedisTemplate redis, JsonMapper json, PostCacheProperties props, MeterRegistry meters) {
        this.redis = redis;
        this.json = json;
        this.props = props;
        this.meters = meters;
    }

    @Override
    public Optional<Post> find(ObjectId id) {
        try {
            String value = redis.opsForValue().get(key(id));
            if (value == null) {
                count("miss");
                return Optional.empty();
            }
            Post post = json.readValue(value, CachedPost.class).toPost();
            count("hit");
            return Optional.of(post);
        } catch (RuntimeException e) {
            // Redis fora do ar ou valor ilegível: segue para o banco.
            log.warn("Falha ao ler o cache do post {}", id, e);
            count("error");
            return Optional.empty();
        }
    }

    @Override
    public void put(Post post) {
        try {
            String value = json.writeValueAsString(CachedPost.from(post));
            redis.opsForValue().set(key(post.getId()), value, ttlWithJitter());
        } catch (RuntimeException e) {
            log.warn("Falha ao gravar o post {} no cache", post.getId(), e);
        }
    }

    @Override
    public void evict(ObjectId id) {
        try {
            redis.delete(key(id));
        } catch (RuntimeException e) {
            log.warn("Falha ao remover o post {} do cache", id, e);
        }
    }

    private static String key(ObjectId id) {
        return KEY_PREFIX + id.toHexString();
    }

    private Duration ttlWithJitter() {
        double factor = 0.9 + ThreadLocalRandom.current().nextDouble() * 0.2; // ±10%
        return Duration.ofMillis((long) (props.ttl().toMillis() * factor));
    }

    private void count(String result) {
        meters.counter("bafafa.post.cache", "result", result).increment();
    }

    public record CachedPost(String id, String authorId, String type, String parentId,
                             String rootId, String text, Instant createdAt) {

        static CachedPost from(Post post) {
            return new CachedPost(
                    post.getId().toHexString(),
                    post.getAuthorId(),
                    post.getType().name(),
                    post.getParentId() == null ? null : post.getParentId().toHexString(),
                    post.getRootId() == null ? null : post.getRootId().toHexString(),
                    post.getText(),
                    post.getCreatedAt());
        }

        Post toPost() {
            return Post.restore(
                    new ObjectId(id),
                    authorId,
                    PostType.valueOf(type),
                    parentId == null ? null : new ObjectId(parentId),
                    rootId == null ? null : new ObjectId(rootId),
                    text,
                    createdAt);
        }
    }
}