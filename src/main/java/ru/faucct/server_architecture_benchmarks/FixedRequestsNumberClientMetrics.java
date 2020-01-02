package ru.faucct.server_architecture_benchmarks;

import java.util.Arrays;
import java.util.stream.LongStream;

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
        assert requestDurations[index] == 0;
        requestDurations[index] = -System.nanoTime();
    }

    @Override
    public void processing() {
        assert processingDurations[index] == 0;
        processingDurations[index] = -System.nanoTime();
    }

    @Override
    public void processed() {
        assert processingDurations[index] < 0;
        processingDurations[index] += System.nanoTime();
    }

    @Override
    public void responded() {
        assert requestDurations[index] < 0;
        requestDurations[index] += System.nanoTime();
        index++;
    }

    public long[] requestDurations() {
        return Arrays.copyOf(requestDurations, requestDurations.length);
    }

    public double averageRequestDuration() {
        return LongStream.of(requestDurations).summaryStatistics().getAverage();
    }

    public long[] processingDurations() {
        return Arrays.copyOf(processingDurations, processingDurations.length);
    }

    public double averageProcessingDuration() {
        return LongStream.of(processingDurations).summaryStatistics().getAverage();
    }

    @Override
    public String toString() {
        return String.valueOf(index);
    }
}
