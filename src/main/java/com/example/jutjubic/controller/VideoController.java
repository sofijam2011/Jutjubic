package com.example.jutjubic.controller;

import com.example.jutjubic.dto.VideoResponse;
import com.example.jutjubic.model.User;
import com.example.jutjubic.repository.UserRepository;
import com.example.jutjubic.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoResponse> uploadVideo(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam("thumbnail") MultipartFile thumbnail,
            @RequestParam("video") MultipartFile video,
            @RequestParam(value = "location", required = false) String location,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        var uploadedVideo = videoService.uploadVideo(title, description, tags,
                thumbnail, video, location, user);
        var response = videoService.getVideoById(uploadedVideo.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<VideoResponse>> getAllVideos() {
        List<VideoResponse> videos = videoService.getAllVideos();
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long id) {
        VideoResponse video = videoService.getVideoById(id);
        return ResponseEntity.ok(video);
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long id) {
        videoService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id) {
        byte[] thumbnail = videoService.getThumbnail(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(thumbnail);
    }

    /**
     * STREAMING ENDPOINT - Sa CORS headers!
     * Omogućava gledanje videa direktno u browseru
     */
    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long id) {
        try {
            // Dobavi informacije o videu
            VideoResponse videoResponse = videoService.getVideoById(id);

            // Učitaj video fajl
            Path videoPath = Paths.get("./uploads").resolve(videoResponse.getVideoPath());
            Resource resource = new UrlResource(videoPath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Vrati video sa odgovarajućim headerima za streaming + CORS
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}