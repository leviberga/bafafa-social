package com.leviberga.bafafa.engagement;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface EngagementApi {
    Map<String, EngagementSummary> summaries(Collection<String> postIds, UUID viewerId);
}