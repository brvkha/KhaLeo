package com.khaleo.flashcard.service.deploy;

import java.util.List;

public final class DeploymentResultAggregator {

    private DeploymentResultAggregator() {
    }

    public static Aggregation aggregate(List<String> statuses) {
        int total = statuses.size();
        int failed = (int) statuses.stream()
                .map(status -> status == null ? "" : status.toUpperCase())
                .filter(status -> status.equals("FAILED") || status.equals("TIMED_OUT") || status.equals("CANCELLED"))
                .count();
        int succeeded = (int) statuses.stream()
                .map(status -> status == null ? "" : status.toUpperCase())
                .filter("SUCCESS"::equals)
                .count();
        return new Aggregation(total, succeeded, failed, failed == 0 && total > 0);
    }

    public record Aggregation(int totalTargets, int succeededTargets, int failedTargets, boolean successful) {
    }
}
