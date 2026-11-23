package com.fairfold.ai;

import java.util.List;

/** STAR-structured coaching feedback. */
public record StarFeedback(
        Component situation,
        Component task,
        Component action,
        Component result,
        String summary
) {
    public record Component(int rating, List<Point> points) {
    }

    public record Point(String comment, String quote) {
    }
}
