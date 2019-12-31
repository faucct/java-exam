package ru.faucct.server_architecture_benchmarks;

import java.util.Arrays;

public class FixedRequestsNumberClientMetrics implements ClientMetrics {
    private final long[] requestDurations;
    private final long[] processingDurations;
    private int index;

    public FixedRequestsNumberClientMetrics(int requestsNumber) {
        requestDurations = new long[requestsNumber];
        processingDurations = new long[requestsNumber];
    }

    @Override
    public void received() {
        assert processingDurations[index] == 0;
        processingDurations[index] = -System.nanoTime();
    }

    @Override
    public void processing() {
        assert requestDurations[index] == 0;
        requestDurations[index] = -System.nanoTime();
    }

    @Override
    public void processed() {
        assert requestDurations[index] < 0;
        requestDurations[index] += System.nanoTime();
    }

    @Override
    public void responded() {
        assert processingDurations[index] < 0;
        processingDurations[index] += System.nanoTime();
        index++;
    }

    public long[] requestDurations() {
        return Arrays.copyOf(requestDurations, requestDurations.length);
    }

    public long[] processingDurations() {
        return Arrays.copyOf(processingDurations, processingDurations.length);
    }
}
