package com.leviberga.bafafa.post;

import java.util.List;

public record PostViewPage(List<PostView> items, String nextCursor) {
}