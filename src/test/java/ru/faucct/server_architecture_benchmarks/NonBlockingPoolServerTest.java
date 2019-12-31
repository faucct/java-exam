package ru.faucct.server_architecture_benchmarks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static ru.faucct.server_architecture_benchmarks.RequestHelpers.readResponse;
import static ru.faucct.server_architecture_benchmarks.RequestHelpers.writeRequest;

public class NonBlockingPoolServerTest {
    @Test
    public void test() throws IOException, InterruptedException {
        try (
                final ServerSocketChannel serverChannel = ServerSocketChannel.open().bind(new InetSocketAddress(0));
                final NonBlockingPoolServer ignored = new NonBlockingPoolServer(
                        serverChannel,
                        Executors.newFixedThreadPool(3),
                        () -> new FixedRequestsNumberClientMetrics(2)
                );
                final Socket socket = new Socket("localhost", ((InetSocketAddress) serverChannel.getLocalAddress()).getPort())
        ) {
            writeRequest(socket, 3, 2, 4);
            assertArrayEquals(new Integer[]{2, 3, 4}, readResponse(socket));
            writeRequest(socket, 3, 2, 4);
            assertArrayEquals(new Integer[]{2, 3, 4}, readResponse(socket));
        }
    }
}
