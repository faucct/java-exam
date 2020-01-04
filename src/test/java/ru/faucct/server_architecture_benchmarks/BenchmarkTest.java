package ru.faucct.server_architecture_benchmarks;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class BenchmarkTest {

    @Test
    void test() throws IOException, InterruptedException {
        new Benchmark(
                new Benchmark.Config(Benchmark.Config.Architecture.Asynchronous, 10000, 50, 1_000_000_000, 10)
        ).local();
    }

}
