package com.leviberga.bafafa.post.web;

import com.leviberga.bafafa.post.application.PostService;
import com.leviberga.bafafa.post.domain.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

final class PostDtos {

    private PostDtos() {
    }

    record CreatePostRequest(@NotBlank @Size(max = 280) String text) {
    }

    record PostResponse(String id, String authorId, String type, String parentId,
                        String rootId, String text, Instant createdAt) {

        static PostResponse from(Post post) {
            return new PostResponse(
                    post.getId().toHexString(),
                    post.getAuthorId(),
                    post.getType().name(),
                    post.getParentId() == null ? null : post.getParentId().toHexString(),
                    post.getRootId() == null ? null : post.getRootId().toHexString(),
                    post.getText(),
                    post.getCreatedAt());
        }
    }

    record PostPageResponse(List<PostResponse> items, String nextCursor) {

        static PostPageResponse from(PostService.PostPage page) {
            return new PostPageResponse(
                    page.items().stream().map(PostResponse::from).toList(),
                    page.nextCursor());
        }
    }
}