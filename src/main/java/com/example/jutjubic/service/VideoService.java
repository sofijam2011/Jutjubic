package com.example.jutjubic.service;

import com.example.jutjubic.dto.VideoResponse;
import com.example.jutjubic.model.Tag;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.TagRepository;
import com.example.jutjubic.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private CacheService cacheService;

    @Transactional
    public Video uploadVideo(String title, String description, List<String> tagNames,
                             MultipartFile thumbnail, MultipartFile video,
                             String location, User user) {
        try {
            // Save files
            String thumbnailPath = fileStorageService.storeThumbnail(thumbnail);
            String videoPath = fileStorageService.storeVideo(video);

            // Create video entity
            Video videoEntity = new Video();
            videoEntity.setTitle(title);
            videoEntity.setDescription(description);
            videoEntity.setThumbnailPath(thumbnailPath);
            videoEntity.setVideoPath(videoPath);
            videoEntity.setLocation(location);
            videoEntity.setUser(user);
            videoEntity.setViewCount(0L);

            // Handle tags
            Set<Tag> tags = new HashSet<>();
            if (tagNames != null) {
                for (String tagName : tagNames) {
                    Tag tag = tagRepository.findByName(tagName)
                            .orElseGet(() -> {
                                Tag newTag = new Tag(tagName);
                                return tagRepository.save(newTag);
                            });
                    tags.add(tag);
                }
            }
            videoEntity.setTags(tags);

            // Save video
            Video savedVideo = videoRepository.save(videoEntity);

            // Cache thumbnail
            cacheService.cacheThumbnail(thumbnailPath, thumbnail);

            return savedVideo;

        } catch (Exception e) {
            // Rollback: delete uploaded files
            fileStorageService.deleteAllFiles();
            throw new RuntimeException("Video upload failed: " + e.getMessage(), e);
        }
    }

    public List<VideoResponse> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toVideoResponse)
                .collect(Collectors.toList());
    }

    public VideoResponse getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        return toVideoResponse(video);
    }

    /**
     * ATOMIČNI INCREMENT VIEW COUNT - BEZ TRANSAKCIJA!
     * Koristi direktan UPDATE u bazi koji je atomičan po defaultu.
     * Garantuje konzistentnost čak i sa 100+ konkurentnih zahteva.
     */
    public void incrementViewCount(Long videoId) {
        int rowsAffected = videoRepository.incrementViewCount(videoId);

        if (rowsAffected == 0) {
            throw new IllegalArgumentException("Video not found with id: " + videoId);
        }
    }

    @Cacheable(value = "thumbnails", key = "#videoId")
    public byte[] getThumbnail(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        return fileStorageService.loadThumbnail(video.getThumbnailPath());
    }

    private VideoResponse toVideoResponse(Video video) {
        VideoResponse response = new VideoResponse();
        response.setId(video.getId());
        response.setTitle(video.getTitle());
        response.setDescription(video.getDescription());
        response.setThumbnailPath(video.getThumbnailPath());
        response.setVideoPath(video.getVideoPath());
        response.setUsername(video.getUser().getUsername());
        response.setCreatedAt(video.getCreatedAt());
        response.setViewCount(video.getViewCount());
        response.setLocation(video.getLocation());
        response.setTags(video.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toList()));
        return response;
    }
}