package com.leviberga.bafafa.engagement.web;

import com.leviberga.bafafa.engagement.application.EngagementService;

final class EngagementDtos {

    private EngagementDtos() {
    }

    record EngagementResponse(long likeCount, long repostCount, boolean likedByMe, boolean repostedByMe) {

        static EngagementResponse from(EngagementService.PostEngagement engagement) {
            return new EngagementResponse(engagement.likeCount(), engagement.repostCount(),
                    engagement.likedByMe(), engagement.repostedByMe());
        }
    }
}