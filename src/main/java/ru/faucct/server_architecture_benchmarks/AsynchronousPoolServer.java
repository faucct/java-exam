package ru.faucct.server_architecture_benchmarks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class AsynchronousPoolServer implements AutoCloseable {
    private final AsynchronousServerSocketChannel serverSocket;
    private boolean closed = false;

    public AsynchronousPoolServer(AsynchronousServerSocketChannel serverSocket) {
        this(serverSocket, () -> new ClientMetrics() {
        });
    }

    public AsynchronousPoolServer(
            AsynchronousServerSocketChannel serverSocket,
            Supplier<ClientMetrics> clientMetricsSupplier
    ) {
        serverSocket.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel channel, Void ignored) {
                final ClientMetrics clientMetrics = clientMetricsSupplier.get();
                final ByteBuffer header = ByteBuffer.allocate(Integer.BYTES);
                channel.read(header, null, new CompletionHandler<Integer, Void>() {
                    final CompletionHandler<Integer, Void> headerHandler = this;
                    ByteBuffer body, response;

                    @Override
                    public void completed(Integer result, Void attachment) {
                        if (result < 0)
                            return;
                        if (header.hasRemaining()) {
                            channel.read(header, null, this);
                            return;
                        }
                        header.flip();
                        final int length = header.getInt();
                        header.clear();
                        if (body == null || body.capacity() < length)
                            body = ByteBuffer.allocate(length);
                        body.limit(length);
                        channel.read(body, null, bodyHandler);
                    }

                    @Override
                    public void failed(Throwable exc, Void attachment) {
                        throw new RuntimeException(exc);
                    }

                    final CompletionHandler<Integer, Void> bodyHandler = new CompletionHandler<>() {
                        @Override
                        public void completed(Integer result, Void attachment) {
                            if (body.hasRemaining()) {
                                channel.read(body, null, this);
                                return;
                            }
                            clientMetrics.received();
                            body.flip();
                            try {
                                final MessageOuterClass.Message in = MessageOuterClass.Message.parseFrom(
                                        new ByteArrayInputStream(body.array(), 0, body.limit())
                                );
                                body.clear();
                                clientMetrics.processing();
                                final MessageOuterClass.Message out = MessagesProcessor.process(in);
                                clientMetrics.processed();
                                final int length = Integer.BYTES + out.getSerializedSize();
                                if (response == null || response.capacity() < length)
                                    response = ByteBuffer.allocate(length);
                                response.limit(length);
                                response.putInt(out.getSerializedSize());
                                out.writeTo(new OutputStream() {
                                    @Override
                                    public void write(int b) {
                                        response.put((byte) b);
                                    }
                                });
                                response.flip();
                                channel.write(response, null, new CompletionHandler<Integer, Void>() {
                                    @Override
                                    public void completed(Integer result, Void attachment) {
                                        if (response.hasRemaining()) {
                                            channel.write(response, null, this);
                                            return;
                                        }
                                        response.clear();
                                        clientMetrics.responded();
                                        channel.read(header, null, headerHandler);
                                    }

                                    @Override
                                    public void failed(Throwable exc, Void attachment) {
                                        throw new RuntimeException(exc);
                                    }
                                });
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void failed(Throwable exc, Void attachment) {
                            throw new RuntimeException(exc);
                        }
                    };
                });
                serverSocket.accept(null, this);
            }

            @Override
            public void failed(Throwable exc, Void ignored) {
                if (!closed)
                    throw new RuntimeException(exc);
            }
        });
        this.serverSocket = serverSocket;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        serverSocket.close();
    }
}
