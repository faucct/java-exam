package ru.faucct.server_architecture_benchmarks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class BlockingPoolServer implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final ExecutorService processingPool;
    private final Thread thread;
    private boolean closed;

    public BlockingPoolServer(ServerSocket serverSocket, ExecutorService processingPool) {
        this(serverSocket, processingPool, () -> new ClientMetrics() {
        });
    }

    public BlockingPoolServer(
            ServerSocket serverSocket,
            ExecutorService processingPool,
            Supplier<ClientMetrics> clientMetricsSupplier
    ) {
        this.serverSocket = serverSocket;
        this.processingPool = processingPool;
        thread = new Thread(() -> {
            try {
                while (!closed) {
                    final Socket socket = serverSocket.accept();
                    final DataInputStream input = new DataInputStream(socket.getInputStream());
                    final DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    final ExecutorService executorService = Executors.newSingleThreadExecutor();
                    final ClientMetrics clientMetrics = clientMetricsSupplier.get();
                    new Thread(() -> {
                        try {
                            while (true) {
                                final MessageOuterClass.Message in =
                                        MessageOuterClass.Message.parseFrom(input.readNBytes(input.readInt()));
                                clientMetrics.received();
                                processingPool.submit(() -> {
                                    clientMetrics.processing();
                                    final MessageOuterClass.Message out = MessagesProcessor.process(in);
                                    clientMetrics.processed();
                                    executorService.submit(() -> {
                                        try {
                                            output.writeInt(out.getSerializedSize());
                                            out.writeTo(output);
                                            clientMetrics.responded();
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                });
                            }
                        } catch (EOFException ignored) {
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();
                }
            } catch (IOException e) {
                if (!closed)
                    throw new RuntimeException(e);
            }
        });
        thread.start();
    }

    @Override
    public void close() throws Exception {
        closed = true;
        serverSocket.close();
        thread.join();
    }
}
