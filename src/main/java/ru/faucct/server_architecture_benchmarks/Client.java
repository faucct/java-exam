package ru.faucct.server_architecture_benchmarks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class Client {
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;

    public Client(Socket socket) throws IOException {
        this.socket = socket;
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(this.socket.getOutputStream());
    }

    public Integer[] sort(Integer[] unsorted) throws IOException {
        final MessageOuterClass.Message message =
                MessageOuterClass.Message.newBuilder().addAllArray(Arrays.asList(unsorted)).build();
        output.writeInt(message.getSerializedSize());
        message.writeTo(output);
        return MessageOuterClass.Message.parseFrom(input.readNBytes(input.readInt())).getArrayList()
                .toArray(Integer[]::new);
    }
}
