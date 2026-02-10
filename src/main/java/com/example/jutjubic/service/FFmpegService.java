package com.example.jutjubic.service;

import com.example.jutjubic.dto.TranscodingMessage.TranscodingParams;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Servis koji izvršava FFmpeg komande za transcoding videa
 */
@Service
public class FFmpegService {

    /**
     * Izvršava transcoding videa koristeći FFmpeg
     *
     * @param inputPath Putanja do originalnog videa
     * @param outputPath Putanja gde će biti sačuvan transkodovani video
     * @param params Parametri za transcoding
     */
    public void transcodeVideo(String inputPath, String outputPath, TranscodingParams params) throws Exception {
        // Provera da li input fajl postoji
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input video ne postoji: " + inputPath);
        }

        // Kreiranje output direktorijuma ako ne postoji
        File outputFile = new File(outputPath);
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Kreiranje FFmpeg komande
        List<String> command = buildFFmpegCommand(inputPath, outputPath, params);

        System.out.println("🎥 Pokrećem FFmpeg komandu: " + String.join(" ", command));

        // Izvršavanje FFmpeg komande
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // Čitanje output-a procesa
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[FFmpeg] " + line);
            }
        }

        // Čekanje da proces završi
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg proces nije uspeo. Exit code: " + exitCode);
        }

        // Provera da li je output fajl kreiran
        if (!outputFile.exists()) {
            throw new RuntimeException("Output video nije kreiran: " + outputPath);
        }

        System.out.println("✅ FFmpeg transcoding uspešno završen: " + outputPath);
    }

    /**
     * Kreira FFmpeg komandu na osnovu parametara
     */
    private List<String> buildFFmpegCommand(String inputPath, String outputPath, TranscodingParams params) {
        List<String> command = new ArrayList<>();

        command.add("ffmpeg");
        command.add("-i");
        command.add(inputPath);

        // Video codec
        if (params.getCodec() != null) {
            command.add("-c:v");
            command.add(params.getCodec());
        }

        // Video bitrate
        if (params.getBitrate() != null) {
            command.add("-b:v");
            command.add(params.getBitrate());
        }

        // Resolution (scaling)
        if (params.getResolution() != null) {
            command.add("-vf");
            command.add("scale=" + params.getResolution().replace("x", ":"));
        }

        // Audio codec
        if (params.getAudioCodec() != null) {
            command.add("-c:a");
            command.add(params.getAudioCodec());
        }

        // Audio bitrate
        if (params.getAudioBitrate() != null) {
            command.add("-b:a");
            command.add(params.getAudioBitrate());
        }

        // Output format
        if (params.getFormat() != null) {
            command.add("-f");
            command.add(params.getFormat());
        }

        // Preset za brži transcoding (opciono)
        command.add("-preset");
        command.add("medium"); // može biti: ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow

        // Overwrite output file bez pitanja
        command.add("-y");

        // Output path
        command.add(outputPath);

        return command;
    }

    /**
     * Proverava da li je FFmpeg instaliran na sistemu
     */
    public boolean isFFmpegInstalled() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-version");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
