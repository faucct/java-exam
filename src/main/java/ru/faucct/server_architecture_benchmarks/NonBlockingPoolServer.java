package ru.faucct.server_architecture_benchmarks;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class NonBlockingPoolServer implements AutoCloseable {
    private final ServerSocketChannel serverSocket;
    private final ExecutorService processingPool;
    private final Selector readSelector;
    private final Selector writeSelector;
    private final Thread reader;
    private final Thread writer;
    private boolean closed = false;

    static class Attachment {
        final ByteBuffer inputHeader = ByteBuffer.allocate(Integer.BYTES);
        ByteBuffer inputBody, output;
        final ClientMetrics clientMetrics;

        Attachment(ClientMetrics clientMetrics) {
            this.clientMetrics = clientMetrics;
        }
    }

    public NonBlockingPoolServer(ServerSocketChannel serverSocket, ExecutorService processingPool) throws IOException {
        this(serverSocket, processingPool, () -> new ClientMetrics() {
        });
    }

    public NonBlockingPoolServer(
            ServerSocketChannel serverSocket,
            ExecutorService processingPool,
            Supplier<ClientMetrics> clientMetricsSupplier
    ) throws IOException {
        this.serverSocket = serverSocket;
        serverSocket.configureBlocking(false);
        this.processingPool = processingPool;
        readSelector = Selector.open();
        writeSelector = Selector.open();
        writer = new Thread(() -> {
            while (!closed) {
                try {
                    writeSelector.select();
                    for (SelectionKey selectedKey : writeSelector.selectedKeys()) {
                        final SocketChannel channel = (SocketChannel) selectedKey.channel();
                        final Attachment attachment = (Attachment) selectedKey.attachment();
                        channel.write(attachment.output);
                        if (!attachment.output.hasRemaining()) {
                            selectedKey.cancel();
                            attachment.output.clear();
                            attachment.clientMetrics.responded();
                        }
                    }
                    writeSelector.selectedKeys().clear();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        writer.start();
        reader = new Thread(() -> {
            try {
                serverSocket.register(readSelector, serverSocket.validOps());
                while (!closed) {
                    readSelector.select();
                    for (SelectionKey selectedKey : readSelector.selectedKeys()) {
                        if (selectedKey.isAcceptable()) {
                            serverSocket.accept().configureBlocking(false).register(
                                    readSelector,
                                    SelectionKey.OP_READ,
                                    new Attachment(clientMetricsSupplier.get())
                            );
                            readSelector.wakeup();
                        }
                        if (selectedKey.isReadable()) {
                            final SocketChannel channel = (SocketChannel) selectedKey.channel();
                            final Attachment attachment = (Attachment) selectedKey.attachment();
                            if (attachment.inputHeader.hasRemaining()) {
                                channel.read(attachment.inputHeader);
                                if (attachment.inputHeader.hasRemaining())
                                    continue;
                                attachment.inputHeader.flip();
                                final int bodyLength = attachment.inputHeader.getInt();
                                if (attachment.inputBody == null || attachment.inputBody.capacity() < bodyLength)
                                    attachment.inputBody = ByteBuffer.allocate(bodyLength);
                                attachment.inputBody.limit(bodyLength);
                            }
                            channel.read(attachment.inputBody);
                            if (attachment.inputBody.hasRemaining())
                                continue;
                            attachment.inputBody.flip();
                            final MessageOuterClass.Message in = MessageOuterClass.Message.parseFrom(
                                    new ByteArrayInputStream(attachment.inputBody.array())
                            );
                            attachment.inputBody.clear();
                            attachment.inputHeader.clear();
                            attachment.clientMetrics.received();
                            processingPool.submit(() -> {
                                attachment.clientMetrics.processing();
                                final MessageOuterClass.Message out = MessagesProcessor.process(in);
                                attachment.clientMetrics.processed();
                                final int outputLength = Integer.BYTES + out.getSerializedSize();
                                if (attachment.output == null || attachment.output.capacity() < outputLength)
                                    attachment.output = ByteBuffer.allocate(outputLength);
                                attachment.output.limit(outputLength);
                                try {
                                    attachment.output.putInt(out.getSerializedSize());
                                    out.writeTo(new OutputStream() {
                                        @Override
                                        public void write(int b) {
                                            attachment.output.put((byte) b);
                                        }
                                    });
                                    attachment.output.flip();
                                    channel.register(writeSelector, SelectionKey.OP_WRITE, attachment);
                                    writeSelector.wakeup();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        }
                    }
                    readSelector.selectedKeys().clear();
                }
                serverSocket.close();
                for (SelectionKey key : readSelector.keys()) {
                    key.channel().close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        reader.start();
    }

    @Override
    public void close() throws InterruptedException {
        closed = true;
        writeSelector.wakeup();
        readSelector.wakeup();
        writer.join();
        reader.join();
    }
}
