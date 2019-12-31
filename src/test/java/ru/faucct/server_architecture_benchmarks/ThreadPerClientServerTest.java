package ru.faucct.server_architecture_benchmarks;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static ru.faucct.server_architecture_benchmarks.RequestHelpers.readResponse;
import static ru.faucct.server_architecture_benchmarks.RequestHelpers.writeRequest;

public class ThreadPerClientServerTest {
    @Test
    public void test() throws Exception {
        try (
                final ServerSocket serverSocket = new ServerSocket(0);
                final ThreadPerClientServer ignored =
                        new ThreadPerClientServer(serverSocket, () -> new FixedRequestsNumberClientMetrics(2));
                final Socket socket = new Socket("localhost", serverSocket.getLocalPort());
        ) {
            writeRequest(socket, 3, 2, 4);
            assertArrayEquals(new Integer[]{2, 3, 4}, readResponse(socket));
            writeRequest(socket, 3, 2, 4);
            assertArrayEquals(new Integer[]{2, 3, 4}, readResponse(socket));
        }
    }
}
