package ru.faucct.server_architecture_benchmarks;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface Server extends AutoCloseable {
    int port() throws IOException;

    @Override
    void close() throws IOException, InterruptedException;
}
