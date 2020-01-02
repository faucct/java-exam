package ru.faucct.server_architecture_benchmarks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.concurrent.locks.LockSupport.parkNanos;

public class Benchmark {
    public static class Config {
        enum Architecture {
            ThreadPerClient,
            BlockingPool,
            NonBlockingPool,
            Asynchronous
        }

        public final Architecture architecture;
        public final int arraySize;
        public final int clientsNumber;
        public final long delayNanoseconds;
        public final int requestsNumber;

        public Config(Architecture architecture, int arraySize, int clientsNumber, long delayNanoseconds, int requestsNumber) {
            this.architecture = architecture;
            this.arraySize = arraySize;
            this.clientsNumber = clientsNumber;
            this.delayNanoseconds = delayNanoseconds;
            this.requestsNumber = requestsNumber;
        }
    }

    public static class Result {
        public final Config config;
        public final double averageServerRequestDuration, averageProcessingDuration, averageClientRequestDuration;

        public Result(Config config, double averageServerRequestDuration, double averageProcessingDuration, double averageClientRequestDuration) {
            this.config = config;
            this.averageServerRequestDuration = averageServerRequestDuration;
            this.averageProcessingDuration = averageProcessingDuration;
            this.averageClientRequestDuration = averageClientRequestDuration;
        }
    }

    private final Config config;

    public Benchmark(Config config) {
        this.config = config;
    }

    public Result run() throws InterruptedException, IOException {
        final List<FixedRequestsNumberClientMetrics> clientsMetrics = Stream.generate(() ->
                new FixedRequestsNumberClientMetrics(config.requestsNumber)
        ).limit(config.clientsNumber).collect(Collectors.toList());
        try (final Server server = buildServer(clientsMetrics)) {
            final long[] durations = new long[config.clientsNumber];
            final List<Thread> threads = IntStream.range(0, config.clientsNumber).mapToObj(clientId -> new Thread(() -> {
                try (final Socket socket = new Socket("localhost", server.port())) {
                    final Client client = new Client(socket);
                    final Random random = new Random();
                    long start = System.nanoTime();
                    for (int i = 0; i < config.requestsNumber; i++) {
                        final Integer[] sorted = client.sort(
                                random.ints().limit(config.arraySize).boxed().toArray(Integer[]::new)
                        );
                        assert config.arraySize == sorted.length;
                        parkNanos(config.delayNanoseconds);
                    }
                    durations[clientId] = System.nanoTime() - start;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, "client" + clientId)).peek(Thread::start).collect(Collectors.toList());
            for (Thread thread : threads) {
                thread.join();
            }
            return new Result(
                    config,
                    clientsMetrics.stream().mapToDouble(FixedRequestsNumberClientMetrics::averageRequestDuration).summaryStatistics().getAverage(),
                    clientsMetrics.stream().mapToDouble(FixedRequestsNumberClientMetrics::averageProcessingDuration).summaryStatistics().getAverage(),
                    LongStream.of(durations).summaryStatistics().getAverage() / config.requestsNumber
            );
        }
    }

    private Server buildServer(List<FixedRequestsNumberClientMetrics> clientsMetrics) throws IOException {
        final int threadPoolSize = Runtime.getRuntime().availableProcessors();
        final int backlog = 1000;
        switch (config.architecture) {
            case ThreadPerClient:
                return new ThreadPerClientServer(new ServerSocket(0, backlog), clientsMetrics.iterator()::next);
            case BlockingPool:
                return new BlockingPoolServer(
                        new ServerSocket(0, backlog),
                        Executors.newFixedThreadPool(threadPoolSize),
                        clientsMetrics.iterator()::next
                );
            case NonBlockingPool:
                return new NonBlockingPoolServer(
                        ServerSocketChannel.open().bind(new InetSocketAddress(0), backlog),
                        Executors.newFixedThreadPool(threadPoolSize),
                        clientsMetrics.iterator()::next
                );
            case Asynchronous:
                return new AsynchronousPoolServer(
                        AsynchronousServerSocketChannel.open(AsynchronousChannelGroup.withFixedThreadPool(
                                threadPoolSize,
                                Executors.defaultThreadFactory()
                        )).bind(new InetSocketAddress(0), backlog),
                        clientsMetrics.iterator()::next
                );
            default:
                throw new RuntimeException(config.architecture.toString());
        }
    }
}
