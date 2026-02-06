package com.example.jutjubic.controller;

import com.example.jutjubic.dto.VideoResponse;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideo(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "tags", required = false) String tagsString,
            @RequestParam("thumbnail") MultipartFile thumbnail,
            @RequestParam("video") MultipartFile video,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "scheduledDateTime", required = false) String scheduledDateTimeString,
            @RequestParam(value = "durationSeconds", required = false) Long durationSeconds,
            Authentication authentication) {

        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<String> tags = tagsString != null ?
                    Arrays.asList(tagsString.split(",")) :
                    List.of();

            LocalDateTime scheduledDateTime = null;
            if (scheduledDateTimeString != null && !scheduledDateTimeString.isEmpty()) {
                scheduledDateTime = LocalDateTime.parse(scheduledDateTimeString);
            }

            Video uploadedVideo = videoService.uploadVideo(
                    title, description, tags, thumbnail, video, location, latitude, longitude, user, scheduledDateTime, durationSeconds
            );

            return ResponseEntity.ok(uploadedVideo);

        } catch (org.springframework.transaction.TransactionTimedOutException e) {
            System.err.println("Transaction timeout during video upload");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body("Upload timeout: Video upload took too long. Please try again with a smaller file.");
        } catch (Exception e) {
            System.err.println("Error during video upload: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<VideoResponse>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getVideoById(@PathVariable Long id) {
        return ResponseEntity.ok(videoService.getVideoById(id));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long id) {
        videoService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id) {
        try {
            byte[] thumbnail = videoService.getThumbnail(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(thumbnail);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long id) {
        try {
            VideoResponse videoResponse = videoService.getVideoById(id);
            String videoPath = videoResponse.getVideoPath();

            System.out.println("Streaming video from path: " + videoPath);

            File videoFile = new File(videoPath);
            if (!videoFile.exists()) {
                String filename = videoPath.replace('\\', '/');
                filename = filename.substring(filename.lastIndexOf('/') + 1);
                videoFile = new File("./uploads/videos/" + filename);
                if (!videoFile.exists()) {
                    System.err.println("Video file not found at: " + videoPath);
                    return ResponseEntity.notFound().build();
                }
            }

            Path path = videoFile.toPath();
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("video/mp4"))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                System.err.println("Video file exists but not readable: " + videoPath);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/streaming-info")
    public ResponseEntity<Map<String, Object>> getStreamingInfo(@PathVariable Long id) {
        try {
            VideoResponse video = videoService.getVideoById(id);
            Map<String, Object> info = new HashMap<>();

            if (video.getIsScheduled() != null && video.getIsScheduled() && video.getScheduledDateTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime scheduledTime = video.getScheduledDateTime();

                long offsetSeconds = Duration.between(scheduledTime, now).getSeconds();

                if (offsetSeconds < 0) {
                    info.put("available", false);
                    info.put("message", "Video nije još uvek dostupan");
                    info.put("scheduledDateTime", scheduledTime.toString());
                    return ResponseEntity.ok(info);
                }

                if (video.getDurationSeconds() != null && offsetSeconds > video.getDurationSeconds()) {
                    info.put("available", true);
                    info.put("isScheduled", false);
                    info.put("canSeek", true);
                    info.put("offsetSeconds", 0);
                } else {
                    info.put("available", true);
                    info.put("isScheduled", true);
                    info.put("canSeek", false);
                    info.put("offsetSeconds", offsetSeconds);
                    info.put("scheduledDateTime", scheduledTime.toString());
                    if (video.getDurationSeconds() != null) {
                        info.put("durationSeconds", video.getDurationSeconds());
                    }
                }
            } else {
                info.put("available", true);
                info.put("isScheduled", false);
                info.put("canSeek", true);
                info.put("offsetSeconds", 0);
            }

            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("available", false);
            errorInfo.put("message", e.getMessage());
            return ResponseEntity.ok(errorInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}