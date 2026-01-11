package com.example.jutjubic.controller;

import com.example.jutjubic.dto.UserProfileResponse;
import com.example.jutjubic.dto.VideoResponse;
import com.example.jutjubic.service.UserService;
import com.example.jutjubic.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private VideoService videoService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        try {
            UserProfileResponse profile = userService.getUserProfile(id);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/videos")
    public ResponseEntity<List<VideoResponse>> getUserVideos(@PathVariable Long id) {
        List<VideoResponse> videos = videoService.getVideosByUserId(id);
        return ResponseEntity.ok(videos);
    }
}