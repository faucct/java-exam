package ru.faucct.server_architecture_benchmarks;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class BenchmarkTest {

    @Test
    void test() throws IOException, InterruptedException {
        new Benchmark(
                new Benchmark.Config(Benchmark.Config.Architecture.Asynchronous, 10, 50, 0, 10)
        ).local();
    }

}
