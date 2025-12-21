package com.example.jutjubic.service;

import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class CacheService {

    @CachePut(value = "thumbnails", key = "#filePath")
    public byte[] cacheThumbnail(String filePath, MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to cache thumbnail", e);
        }
    }
}