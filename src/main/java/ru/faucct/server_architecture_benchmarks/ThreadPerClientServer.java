package ru.faucct.server_architecture_benchmarks;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public class ThreadPerClientServer implements Server {
    private final ServerSocket serverSocket;
    private final Thread thread;
    private boolean closed;

    public ThreadPerClientServer(ServerSocket serverSocket) {
        this(serverSocket, () -> new ClientMetrics() {
        });
    }

    public ThreadPerClientServer(ServerSocket serverSocket, Supplier<ClientMetrics> clientMetricsSupplier) {
        this.serverSocket = serverSocket;
        thread = new Thread(() -> {
            while (!closed) {
                try {
                    final Socket socket = serverSocket.accept();
                    final DataInputStream input = new DataInputStream(socket.getInputStream());
                    final DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    final ClientMetrics clientMetrics = clientMetricsSupplier.get();
                    new Thread(() -> {
                        try (Socket ignored = socket) {
                            while (true) {
                                final MessageOuterClass.Message in =
                                        MessageOuterClass.Message.parseFrom(input.readNBytes(input.readInt()));
                                clientMetrics.received();
                                clientMetrics.processing();
                                final MessageOuterClass.Message message = MessagesProcessor.process(in);
                                clientMetrics.processed();
                                output.writeInt(message.getSerializedSize());
                                message.writeTo(output);
                                clientMetrics.responded();
                            }
                        } catch (EOFException ignored) {
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();
                } catch (IOException e) {
                    if (!closed)
                        throw new RuntimeException(e);
                }
            }
        });
        thread.start();
    }

    @Override
    public void close() throws IOException, InterruptedException {
        closed = true;
        serverSocket.close();
        thread.join();
    }

    @Override
    public int port() {
        return serverSocket.getLocalPort();
    }
}
