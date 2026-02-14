package com.example.jutjubic.benchmark;

import com.example.jutjubic.dto.UploadEvent;
import com.example.jutjubic.proto.UploadEventProto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class ProtobufVsJsonBenchmarkTest {

    private static final int NUM_MESSAGES = 100;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    public void benchmarkJsonVsProtobuf() throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("BENCHMARK: JSON vs PROTOBUF - " + NUM_MESSAGES + " poruka");
        System.out.println("=".repeat(80));

        List<UploadEvent> jsonEvents = new ArrayList<>();
        List<UploadEventProto.UploadEvent> protoEvents = new ArrayList<>();

        for (int i = 1; i <= NUM_MESSAGES; i++) {
            UploadEvent jsonEvent = new UploadEvent(
                (long) i,
                "Test Video " + i,
                1024L * 1024 * 50,
                "user" + i
            );
            jsonEvents.add(jsonEvent);

            UploadEventProto.UploadEvent protoEvent = UploadEventProto.UploadEvent.newBuilder()
                .setVideoId(i)
                .setNaziv("Test Video " + i)
                .setVelicina(1024L * 1024 * 50)
                .setAutor("user" + i)
                .setTimestamp(System.currentTimeMillis())
                .build();
            protoEvents.add(protoEvent);
        }

        System.out.println("\n--- JSON FORMAT ---");

        long jsonSerStartTime = System.nanoTime();
        List<byte[]> jsonSerializedList = new ArrayList<>();
        for (UploadEvent event : jsonEvents) {
            byte[] serialized = objectMapper.writeValueAsBytes(event);
            jsonSerializedList.add(serialized);
        }
        long jsonSerEndTime = System.nanoTime();
        long jsonSerTime = (jsonSerEndTime - jsonSerStartTime) / 1_000_000;

        long jsonDeserStartTime = System.nanoTime();
        for (byte[] serialized : jsonSerializedList) {
            objectMapper.readValue(serialized, UploadEvent.class);
        }
        long jsonDeserEndTime = System.nanoTime();
        long jsonDeserTime = (jsonDeserEndTime - jsonDeserStartTime) / 1_000_000;

        long jsonTotalSize = jsonSerializedList.stream()
            .mapToLong(bytes -> bytes.length)
            .sum();
        double jsonAvgSize = jsonTotalSize / (double) NUM_MESSAGES;

        System.out.printf("Serijalizacija:   %d ms (%.2f ms/poruka)\n",
            jsonSerTime, jsonSerTime / (double) NUM_MESSAGES);
        System.out.printf("Deserijalizacija: %d ms (%.2f ms/poruka)\n",
            jsonDeserTime, jsonDeserTime / (double) NUM_MESSAGES);
        System.out.printf("Prosečna veličina: %.2f bytes\n", jsonAvgSize);
        System.out.printf("Ukupna veličina:   %d bytes (%.2f KB)\n",
            jsonTotalSize, jsonTotalSize / 1024.0);

        System.out.println("\n--- PROTOBUF FORMAT ---");

        long protoSerStartTime = System.nanoTime();
        List<byte[]> protoSerializedList = new ArrayList<>();
        for (UploadEventProto.UploadEvent event : protoEvents) {
            byte[] serialized = event.toByteArray();
            protoSerializedList.add(serialized);
        }
        long protoSerEndTime = System.nanoTime();
        long protoSerTime = (protoSerEndTime - protoSerStartTime) / 1_000_000;

        long protoDeserStartTime = System.nanoTime();
        for (byte[] serialized : protoSerializedList) {
            UploadEventProto.UploadEvent.parseFrom(serialized);
        }
        long protoDeserEndTime = System.nanoTime();
        long protoDeserTime = (protoDeserEndTime - protoDeserStartTime) / 1_000_000;

        long protoTotalSize = protoSerializedList.stream()
            .mapToLong(bytes -> bytes.length)
            .sum();
        double protoAvgSize = protoTotalSize / (double) NUM_MESSAGES;

        System.out.printf("Serijalizacija:   %d ms (%.2f ms/poruka)\n",
            protoSerTime, protoSerTime / (double) NUM_MESSAGES);
        System.out.printf("Deserijalizacija: %d ms (%.2f ms/poruka)\n",
            protoDeserTime, protoDeserTime / (double) NUM_MESSAGES);
        System.out.printf("Prosečna veličina: %.2f bytes\n", protoAvgSize);
        System.out.printf("Ukupna veličina:   %d bytes (%.2f KB)\n",
            protoTotalSize, protoTotalSize / 1024.0);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("POREĐENJE:");
        System.out.println("=".repeat(80));

        double serSpeedup = jsonSerTime / (double) protoSerTime;
        double deserSpeedup = jsonDeserTime / (double) protoDeserTime;
        double sizeReduction = (1 - protoTotalSize / (double) jsonTotalSize) * 100;

        System.out.printf("Serijalizacija:   Protobuf je %.2fx brži\n", serSpeedup);
        System.out.printf("Deserijalizacija: Protobuf je %.2fx brži\n", deserSpeedup);
        System.out.printf("Veličina:         Protobuf je %.2f%% manji\n", sizeReduction);

        System.out.println("=".repeat(80));
    }
}
