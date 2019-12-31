package ru.faucct.server_architecture_benchmarks;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.faucct.server_architecture_benchmarks.RequestHelpers.readResponse;
import static ru.faucct.server_architecture_benchmarks.RequestHelpers.writeRequest;

public class BlockingPoolServerTest {
    @Test
    public void test() throws Exception {
        try (
                final ServerSocket serverSocket = new ServerSocket(0);
                final BlockingPoolServer ignored = new BlockingPoolServer(
                        serverSocket,
                        Executors.newFixedThreadPool(3),
                        () -> new FixedRequestsNumberClientMetrics(2)
                );
                final Socket socket = new Socket("localhost", serverSocket.getLocalPort());
        ) {
            writeRequest(socket, 3, 2, 4);
            assertArrayEquals(new Integer[]{2, 3, 4}, readResponse(socket));
            writeRequest(socket, 3, 2, 4);
            assertArrayEquals(new Integer[]{2, 3, 4}, readResponse(socket));
        }
    }
}
