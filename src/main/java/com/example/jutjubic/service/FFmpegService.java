package com.example.jutjubic.service;

import com.example.jutjubic.dto.TranscodingMessage.TranscodingParams;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class FFmpegService {

    public void transcodeVideo(String inputPath, String outputPath, TranscodingParams params) throws Exception {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input video ne postoji: " + inputPath);
        }

        File outputFile = new File(outputPath);
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }

        List<String> command = buildFFmpegCommand(inputPath, outputPath, params);

        System.out.println("🎥 Pokrećem FFmpeg komandu: " + String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[FFmpeg] " + line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg proces nije uspeo. Exit code: " + exitCode);
        }

        if (!outputFile.exists()) {
            throw new RuntimeException("Output video nije kreiran: " + outputPath);
        }

        System.out.println("✅ FFmpeg transcoding uspešno završen: " + outputPath);
    }

    private List<String> buildFFmpegCommand(String inputPath, String outputPath, TranscodingParams params) {
        List<String> command = new ArrayList<>();

        command.add("ffmpeg");
        command.add("-i");
        command.add(inputPath);

        if (params.getCodec() != null) {
            command.add("-c:v");
            command.add(params.getCodec());
        }

        if (params.getBitrate() != null) {
            command.add("-b:v");
            command.add(params.getBitrate());
        }

        if (params.getResolution() != null) {
            command.add("-vf");
            command.add("scale=" + params.getResolution().replace("x", ":"));
        }

        if (params.getAudioCodec() != null) {
            command.add("-c:a");
            command.add(params.getAudioCodec());
        }

        if (params.getAudioBitrate() != null) {
            command.add("-b:a");
            command.add(params.getAudioBitrate());
        }

        if (params.getFormat() != null) {
            command.add("-f");
            command.add(params.getFormat());
        }

        command.add("-preset");
        command.add("medium");

        command.add("-y");

        command.add(outputPath);

        return command;
    }

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
