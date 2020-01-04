package ru.faucct.server_architecture_benchmarks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class ServersApplication {
    private static class ArrayListClientMetrics implements ClientMetrics {
        final ArrayList<Long> requestDurations = new ArrayList<>(), processingDurations = new ArrayList<>();

        @Override
        public void received() {
            requestDurations.add(-System.nanoTime());
        }

        @Override
        public void processing() {
            processingDurations.add(-System.nanoTime());
        }

        @Override
        public void processed() {
            final int index = processingDurations.size() - 1;
            processingDurations.set(index, processingDurations.get(index) + System.nanoTime());
        }

        @Override
        public void responded() {
            final int index = requestDurations.size() - 1;
            requestDurations.set(index, requestDurations.get(index) + System.nanoTime());
        }

        public double averageRequestDuration() {
            return requestDurations.stream().mapToLong(x -> x).summaryStatistics().getAverage();
        }

        public double averageProcessingDuration() {
            return processingDurations.stream().mapToLong(x -> x).summaryStatistics().getAverage();
        }
    }

    public static void main(String[] args) throws IOException {
        try (final ServerSocket serverSocket = new ServerSocket(1600)) {
            while (true) {
                final Socket socket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        while (true) {
                            final DataInputStream input = new DataInputStream(socket.getInputStream());
                            final DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                            final ArrayList<ArrayListClientMetrics> clientsMetrics = new ArrayList<>();
                            try (final Server server = buildServer(
                                    Benchmark.Config.Architecture.values()[input.readInt()],
                                    () -> {
                                        final ArrayListClientMetrics clientMetrics = new ArrayListClientMetrics();
                                        clientsMetrics.add(clientMetrics);
                                        return clientMetrics;
                                    }
                            )) {
                                output.writeInt(server.port());
                                input.readByte();
                            }
                            output.writeDouble(
                                    clientsMetrics.stream()
                                            .mapToDouble(ArrayListClientMetrics::averageRequestDuration)
                                            .summaryStatistics().getAverage()
                            );
                            output.writeDouble(
                                    clientsMetrics.stream()
                                            .mapToDouble(ArrayListClientMetrics::averageProcessingDuration)
                                            .summaryStatistics().getAverage()
                            );
                        }
                    } catch (EOFException ignored) {
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        }
    }

    private static Server buildServer(
            Benchmark.Config.Architecture architecture, Supplier<ClientMetrics> clientMetricsSupplier
    ) throws IOException {
        final int threadPoolSize = Runtime.getRuntime().availableProcessors();
        final int backlog = 1000;
        switch (architecture) {
            case ThreadPerClient:
                return new ThreadPerClientServer(new ServerSocket(0, backlog), clientMetricsSupplier);
            case BlockingPool:
                return new BlockingPoolServer(
                        new ServerSocket(0, backlog),
                        Executors.newFixedThreadPool(threadPoolSize),
                        clientMetricsSupplier
                );
            case NonBlockingPool:
                return new NonBlockingPoolServer(
                        ServerSocketChannel.open().bind(new InetSocketAddress(0), backlog),
                        Executors.newFixedThreadPool(threadPoolSize),
                        clientMetricsSupplier
                );
            case Asynchronous:
                return new AsynchronousPoolServer(
                        AsynchronousServerSocketChannel.open(AsynchronousChannelGroup.withFixedThreadPool(
                                threadPoolSize,
                                Executors.defaultThreadFactory()
                        )).bind(new InetSocketAddress(0), backlog),
                        clientMetricsSupplier
                );
            default:
                throw new RuntimeException(architecture.toString());
        }
    }
}
