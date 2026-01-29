package com.example.jutjubic.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private ThreadLocal<List<String>> uploadedFiles = ThreadLocal.withInitial(ArrayList::new);

    public String storeThumbnail(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";

        String filename = "thumb_" + UUID.randomUUID() + extension;

        Path thumbnailDir = Paths.get(uploadDir, "thumbnails");
        Files.createDirectories(thumbnailDir);

        Path destinationPath = thumbnailDir.resolve(filename).toAbsolutePath().normalize();
        Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);


        uploadedFiles.get().add(destinationPath.toString());

        return destinationPath.toString();
    }

    public String storeVideo(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".mp4")) {
            throw new IllegalArgumentException("Only MP4 videos are allowed");
        }

        long maxSize = 200 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Video size exceeds 200MB limit");
        }

        String filename = "video_" + UUID.randomUUID() + ".mp4";

        Path videoDir = Paths.get(uploadDir, "videos");
        Files.createDirectories(videoDir);

        Path destinationPath = videoDir.resolve(filename).toAbsolutePath().normalize();
        Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);


        uploadedFiles.get().add(destinationPath.toString());

        return destinationPath.toString();
    }

    public byte[] loadThumbnail(String thumbnailPath) {
        try {
            Path path = Paths.get(thumbnailPath);
            if (!Files.exists(path)) {
                String filename = thumbnailPath.replace('\\', '/');
                filename = filename.substring(filename.lastIndexOf('/') + 1);
                path = Paths.get(uploadDir, "thumbnails", filename).toAbsolutePath().normalize();
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Could not read thumbnail file: " + thumbnailPath);
        }
    }

    public void deleteAllFiles() {
        List<String> files = uploadedFiles.get();
        for (String filePath : files) {
            try {
                Path path = Paths.get(filePath);
                Files.deleteIfExists(path);
                System.out.println("Deleted file during rollback: " + filePath);
            } catch (IOException e) {
                System.err.println("Failed to delete file during rollback: " + filePath);
                e.printStackTrace();
            }
        }
        uploadedFiles.remove();
    }

    public void clearUploadTracking() {
        uploadedFiles.remove();
    }
}