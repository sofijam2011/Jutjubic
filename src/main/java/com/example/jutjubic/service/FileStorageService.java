package com.example.jutjubic.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }

    public String storeThumbnail(MultipartFile file) {
        return storeFile(file, "thumbnails");
    }

    public String storeVideo(MultipartFile file) {
        // Check file size (max 200MB)
        if (file.getSize() > 200 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds maximum limit of 200MB");
        }

        // Check format
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("video/mp4")) {
            throw new RuntimeException("Only MP4 format is supported");
        }

        return storeFile(file, "videos");
    }

    private String storeFile(MultipartFile file, String subfolder) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            Path targetLocation = this.fileStorageLocation.resolve(subfolder).resolve(fileName);
            Files.createDirectories(targetLocation.getParent());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return subfolder + "/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName, ex);
        }
    }

    public byte[] loadThumbnail(String filePath) {
        try {
            Path file = fileStorageLocation.resolve(filePath);
            return Files.readAllBytes(file);
        } catch (IOException ex) {
            throw new RuntimeException("Could not read file " + filePath, ex);
        }
    }

    public void deleteAllFiles() {
        // Implementation for rollback
        // You can implement this later if needed
    }
}