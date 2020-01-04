package ru.faucct.server_architecture_benchmarks;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static ru.faucct.server_architecture_benchmarks.RequestHelpers.readResponse;
import static ru.faucct.server_architecture_benchmarks.RequestHelpers.writeRequest;

public class AsynchronousPoolServerTest {
    @Test
    public void test() throws Exception {
        try (
                final AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel
                        .open()
                        .bind(new InetSocketAddress(0));
                final AsynchronousPoolServer ignored =
                        new AsynchronousPoolServer(serverChannel, Executors.newFixedThreadPool(3), () -> new FixedRequestsNumberClientMetrics(2));
                final Socket socket = new Socket("localhost", ((InetSocketAddress) serverChannel.getLocalAddress()).getPort())
        ) {
            writeRequest(socket, 3, 2, 4);
            assertArrayEquals(new Integer[]{2, 3, 4}, readResponse(socket));
            writeRequest(socket, 3, 2, 4);
            assertArrayEquals(new Integer[]{2, 3, 4}, readResponse(socket));
        }
    }
}
