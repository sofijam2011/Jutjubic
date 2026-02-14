package com.example.jutjubic.service;

import com.example.jutjubic.dto.UploadEvent;
import com.example.jutjubic.proto.UploadEventProto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SerializationBenchmarkService {

    private static final int BROJ_PORUKA = 50;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> izvrsiPoredjenje() throws Exception {
        List<UploadEvent> jsonEventovi = generisiTestEventove();

        long ukupnoJsonSerijalizacija = 0;
        long ukupnoJsonDeserijalizacija = 0;
        long ukupnoJsonVelicina = 0;

        List<byte[]> jsonPoruke = new ArrayList<>();

        for (UploadEvent event : jsonEventovi) {
            long start = System.nanoTime();
            byte[] bytes = objectMapper.writeValueAsBytes(event);
            ukupnoJsonSerijalizacija += System.nanoTime() - start;

            ukupnoJsonVelicina += bytes.length;
            jsonPoruke.add(bytes);
        }

        for (byte[] bytes : jsonPoruke) {
            long start = System.nanoTime();
            objectMapper.readValue(bytes, UploadEvent.class);
            ukupnoJsonDeserijalizacija += System.nanoTime() - start;
        }

        long ukupnoProtobufSerijalizacija = 0;
        long ukupnoProtobufDeserijalizacija = 0;
        long ukupnoProtobufVelicina = 0;

        List<byte[]> protobufPoruke = new ArrayList<>();

        for (UploadEvent event : jsonEventovi) {
            UploadEventProto.UploadEvent protoEvent = konvertujUProto(event);

            long start = System.nanoTime();
            byte[] bytes = protoEvent.toByteArray();
            ukupnoProtobufSerijalizacija += System.nanoTime() - start;

            ukupnoProtobufVelicina += bytes.length;
            protobufPoruke.add(bytes);
        }

        for (byte[] bytes : protobufPoruke) {
            long start = System.nanoTime();
            UploadEventProto.UploadEvent.parseFrom(bytes);
            ukupnoProtobufDeserijalizacija += System.nanoTime() - start;
        }

        long jsonAvgSer = ukupnoJsonSerijalizacija / BROJ_PORUKA;
        long jsonAvgDeser = ukupnoJsonDeserijalizacija / BROJ_PORUKA;
        long jsonAvgVelicina = ukupnoJsonVelicina / BROJ_PORUKA;

        long protoAvgSer = ukupnoProtobufSerijalizacija / BROJ_PORUKA;
        long protoAvgDeser = ukupnoProtobufDeserijalizacija / BROJ_PORUKA;
        long protoAvgVelicina = ukupnoProtobufVelicina / BROJ_PORUKA;

        ispisiRezultate(jsonAvgSer, jsonAvgDeser, jsonAvgVelicina,
                        protoAvgSer, protoAvgDeser, protoAvgVelicina);

        Map<String, Object> rezultati = new LinkedHashMap<>();
        rezultati.put("brojPoruka", BROJ_PORUKA);

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("prosecnoVremeSerijalizacijeNs", jsonAvgSer);
        json.put("prosecnoVremeDeserijalizacijeNs", jsonAvgDeser);
        json.put("prosecnaVelicinaB", jsonAvgVelicina);
        rezultati.put("JSON", json);

        Map<String, Object> proto = new LinkedHashMap<>();
        proto.put("prosecnoVremeSerijalizacijeNs", protoAvgSer);
        proto.put("prosecnoVremeDeserijalizacijeNs", protoAvgDeser);
        proto.put("prosecnaVelicinaB", protoAvgVelicina);
        rezultati.put("Protobuf", proto);

        Map<String, Object> razlika = new LinkedHashMap<>();
        razlika.put("serijalizacijaJsonBrziOd", protoAvgSer > jsonAvgSer
                ? String.format("JSON brži za %.1f%%", (protoAvgSer - jsonAvgSer) * 100.0 / protoAvgSer)
                : String.format("Protobuf brži za %.1f%%", (jsonAvgSer - protoAvgSer) * 100.0 / jsonAvgSer));
        razlika.put("deserijalizacijaJsonBrziOd", protoAvgDeser > jsonAvgDeser
                ? String.format("JSON brži za %.1f%%", (protoAvgDeser - jsonAvgDeser) * 100.0 / protoAvgDeser)
                : String.format("Protobuf brži za %.1f%%", (jsonAvgDeser - protoAvgDeser) * 100.0 / jsonAvgDeser));
        razlika.put("velicinaManja", protoAvgVelicina < jsonAvgVelicina
                ? String.format("Protobuf manji za %.1f%%", (jsonAvgVelicina - protoAvgVelicina) * 100.0 / jsonAvgVelicina)
                : String.format("JSON manji za %.1f%%", (protoAvgVelicina - jsonAvgVelicina) * 100.0 / protoAvgVelicina));
        rezultati.put("poredjenje", razlika);

        return rezultati;
    }

    private List<UploadEvent> generisiTestEventove() {
        List<UploadEvent> eventovi = new ArrayList<>();
        String[] autori = {"korisnik1", "ana_jovic", "marko_petrovic", "milica_123", "stefan_dev"};
        String[] naslovi = {
            "Moja prva vožnja biciklom",
            "Recept za domaći hleb",
            "Tutorial: Spring Boot i RabbitMQ",
            "Sunset na Kopaoniku",
            "How to learn programming fast"
        };

        for (int i = 0; i < BROJ_PORUKA; i++) {
            eventovi.add(new UploadEvent(
                (long) (i + 1),
                naslovi[i % naslovi.length] + " " + (i + 1),
                (long) (1024 * 1024 * (10 + i % 500)),
                autori[i % autori.length],
                LocalDateTime.now().minusMinutes(i)
            ));
        }
        return eventovi;
    }

    private UploadEventProto.UploadEvent konvertujUProto(UploadEvent event) {
        return UploadEventProto.UploadEvent.newBuilder()
                .setVideoId(event.getVideoId())
                .setNaziv(event.getNaziv())
                .setVelicina(event.getVelicina())
                .setAutor(event.getAutor())
                .setTimestamp(event.getTimestamp().toInstant(
                        java.time.ZoneOffset.UTC).toEpochMilli())
                .build();
    }

    private void ispisiRezultate(long jsonSer, long jsonDeser, long jsonVel,
                                  long protoSer, long protoDeser, long protoVel) {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║       JSON vs PROTOBUF — Rezultati poređenja (" + BROJ_PORUKA + " poruka)  ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf("║  %-30s  %10s  %10s  ║%n", "Metrika", "JSON", "Protobuf");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf("║  %-30s  %8d ns  %8d ns  ║%n", "Prosečna serijalizacija", jsonSer, protoSer);
        System.out.printf("║  %-30s  %8d ns  %8d ns  ║%n", "Prosečna deserijalizacija", jsonDeser, protoDeser);
        System.out.printf("║  %-30s  %10d B  %10d B  ║%n", "Prosečna veličina poruke", jsonVel, protoVel);
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }
}
