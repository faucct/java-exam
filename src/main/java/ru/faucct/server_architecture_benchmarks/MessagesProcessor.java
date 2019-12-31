package ru.faucct.server_architecture_benchmarks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MessagesProcessor {
    static MessageOuterClass.Message process(MessageOuterClass.Message message) {
        final ArrayList<Integer> list = new ArrayList<>(message.getArrayList());
        for (int i = 0; i < list.size(); i++) {
            final List<Integer> tail = list.subList(i, list.size());
            final Integer min = tail.stream().min(Comparator.naturalOrder()).get();
            final int minIndex = tail.indexOf(min);
            tail.set(minIndex, tail.get(0));
            tail.set(0, min);
        }
        return MessageOuterClass.Message.newBuilder().addAllArray(list).build();
    }
}
