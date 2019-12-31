package ru.faucct.server_architecture_benchmarks;

public interface ClientMetrics {
    default void received() {
    }

    default void processing() {
    }

    default void processed() {
    }

    default void responded() {
    }
}
