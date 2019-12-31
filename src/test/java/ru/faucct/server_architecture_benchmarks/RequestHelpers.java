package ru.faucct.server_architecture_benchmarks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class RequestHelpers {
    public static void writeRequest(Socket socket, Integer... unsorted) throws IOException {
        final DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        final MessageOuterClass.Message message =
                MessageOuterClass.Message.newBuilder().addAllArray(Arrays.asList(unsorted)).build();
        output.writeInt(message.getSerializedSize());
        message.writeTo(output);
    }

    public static Integer[] readResponse(Socket socket) throws IOException {
        final DataInputStream input = new DataInputStream(socket.getInputStream());
        return MessageOuterClass.Message.parseFrom(input.readNBytes(input.readInt())).getArrayList()
                .toArray(Integer[]::new);
    }
}
