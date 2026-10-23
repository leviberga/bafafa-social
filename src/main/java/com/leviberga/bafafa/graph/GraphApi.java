package com.leviberga.bafafa.graph;

import java.util.Set;

public interface GraphApi {
    Set<String> followingIds(String accountId);
}