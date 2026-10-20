package com.leviberga.bafafa.engagement.web;

import com.leviberga.bafafa.engagement.application.LikeService;

final class EngagementDtos {

    private EngagementDtos() {
    }

    record EngagementResponse(long likeCount, boolean likedByMe) {

        static EngagementResponse from(LikeService.PostEngagement engagement) {
            return new EngagementResponse(engagement.likeCount(), engagement.likedByMe());
        }
    }
}