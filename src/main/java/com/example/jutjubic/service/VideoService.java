package com.example.jutjubic.service;

import com.example.jutjubic.dto.TranscodingMessage;
import com.example.jutjubic.dto.UploadEvent;
import com.example.jutjubic.dto.VideoResponse;
import com.example.jutjubic.proto.UploadEventProto;
import com.example.jutjubic.model.Tag;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.model.VideoView;
import com.example.jutjubic.repository.TagRepository;
import com.example.jutjubic.repository.VideoRepository;
import com.example.jutjubic.repository.VideoViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
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

    @Autowired
    private MapTileService mapTileService;

    @Autowired
    private VideoViewRepository videoViewRepository;

    @Autowired
    private TranscodingProducer transcodingProducer;

    @Autowired
    private UploadEventProducer uploadEventProducer;

    @Autowired
    private ApplicationContext applicationContext;

    private VideoService self() {
        return applicationContext.getBean(VideoService.class);
    }

    public Video uploadVideo(String title, String description, List<String> tagNames,
                             MultipartFile thumbnail, MultipartFile video,
                             String location, Double latitude, Double longitude, User user, LocalDateTime scheduledDateTime, Long durationSeconds) {
        long videoSize = video.getSize();
        Video savedVideo = self().saveVideoTransactional(
                title, description, tagNames, thumbnail, video,
                location, latitude, longitude, user, scheduledDateTime, durationSeconds
        );

        sendTranscodingJob(savedVideo);
        sendUploadEvents(savedVideo, videoSize);

        return savedVideo;
    }

    @Transactional(timeout = 300)
    public Video saveVideoTransactional(String title, String description, List<String> tagNames,
                                        MultipartFile thumbnail, MultipartFile video,
                                        String location, Double latitude, Double longitude, User user,
                                        LocalDateTime scheduledDateTime, Long durationSeconds) {
        try {
            String thumbnailPath = fileStorageService.storeThumbnail(thumbnail);
            String videoPath = fileStorageService.storeVideo(video);

            Video videoEntity = new Video();
            videoEntity.setTitle(title);
            videoEntity.setDescription(description);
            videoEntity.setThumbnailPath(thumbnailPath);
            videoEntity.setVideoPath(videoPath);
            videoEntity.setLocation(location);
            videoEntity.setLatitude(latitude);
            videoEntity.setLongitude(longitude);
            videoEntity.setUser(user);
            videoEntity.setViewCount(0L);
            videoEntity.setTranscodingStatus("PENDING");

            if (scheduledDateTime != null) {
                videoEntity.setScheduledDateTime(scheduledDateTime);
                videoEntity.setIsScheduled(true);
            } else {
                videoEntity.setIsScheduled(false);
            }

            if (durationSeconds != null) {
                videoEntity.setDurationSeconds(durationSeconds);
            }

            Set<Tag> tags = new HashSet<>();
            if (tagNames != null) {
                for (String tagName : tagNames) {
                    String trimmedTag = tagName.trim();
                    if (!trimmedTag.isEmpty()) {
                        Tag tag = tagRepository.findByName(trimmedTag)
                                .orElseGet(() -> {
                                    Tag newTag = new Tag(trimmedTag);
                                    return tagRepository.save(newTag);
                                });
                        tags.add(tag);
                    }
                }
            }
            videoEntity.setTags(tags);

            Video savedVideo = videoRepository.save(videoEntity);

            cacheService.cacheThumbnail(savedVideo.getId(), thumbnail);
            mapTileService.updateTileForNewVideo(savedVideo);
            fileStorageService.clearUploadTracking();

            return savedVideo;

        } catch (Exception e) {
            fileStorageService.deleteAllFiles();
            throw new RuntimeException("Video upload failed: " + e.getMessage(), e);
        }
    }

    public List<VideoResponse> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(video -> {
                    if (video.getIsScheduled() != null && video.getIsScheduled()) {
                        return video.getScheduledDateTime() != null &&
                               LocalDateTime.now().isAfter(video.getScheduledDateTime());
                    }
                    return true;
                })
                .map(this::toVideoResponse)
                .collect(Collectors.toList());
    }

    public VideoResponse getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        if (video.getIsScheduled() != null && video.getIsScheduled()) {
            if (video.getScheduledDateTime() != null &&
                LocalDateTime.now().isBefore(video.getScheduledDateTime())) {
                throw new IllegalArgumentException("Video is not available yet");
            }
        }

        return toVideoResponse(video);
    }

    public void incrementViewCount(Long videoId) {
        int rowsAffected = videoRepository.incrementViewCount(videoId);

        if (rowsAffected == 0) {
            throw new IllegalArgumentException("Video not found with id: " + videoId);
        }

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found with id: " + videoId));
        VideoView videoView = new VideoView(video, LocalDateTime.now());
        videoViewRepository.save(videoView);
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
        response.setUserId(video.getUser().getId());
        response.setScheduledDateTime(video.getScheduledDateTime());
        response.setIsScheduled(video.getIsScheduled());
        response.setDurationSeconds(video.getDurationSeconds());
        response.setTranscodedVideoPath(video.getTranscodedVideoPath());
        response.setTranscodingStatus(video.getTranscodingStatus());
        return response;
    }

    public List<VideoResponse> getVideosByUserId(Long userId) {
        return videoRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(video -> {
                    if (video.getIsScheduled() != null && video.getIsScheduled()) {
                        return video.getScheduledDateTime() != null &&
                               LocalDateTime.now().isAfter(video.getScheduledDateTime());
                    }
                    return true;
                })
                .map(this::toVideoResponse)
                .collect(Collectors.toList());
    }

    private void sendTranscodingJob(Video video) {
        try {
            String originalPath = video.getVideoPath();
            String outputPath = generateTranscodedVideoPath(originalPath);

            TranscodingMessage.TranscodingParams params = TranscodingMessage.TranscodingParams.default720p();

            TranscodingMessage message = new TranscodingMessage(
                    video.getId(),
                    originalPath,
                    outputPath,
                    params
            );

            transcodingProducer.sendTranscodingJob(message);

            System.out.println("✅ Transcoding job poslat za video ID: " + video.getId());
        } catch (Exception e) {
            System.err.println("⚠️ Greška prilikom slanja transcoding job-a: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendUploadEvents(Video video, long fileSize) {
        try {
            String username = video.getUser().getUsername();

            UploadEvent jsonEvent = new UploadEvent(
                video.getId(),
                video.getTitle(),
                fileSize,
                username
            );
            uploadEventProducer.sendJsonEvent(jsonEvent);

            UploadEventProto.UploadEvent protoEvent = UploadEventProto.UploadEvent.newBuilder()
                .setVideoId(video.getId())
                .setNaziv(video.getTitle())
                .setVelicina(fileSize)
                .setAutor(username)
                .setTimestamp(System.currentTimeMillis())
                .build();
            uploadEventProducer.sendProtobufEvent(protoEvent);

            System.out.println("✅ Upload eventi poslati (JSON + Protobuf)");

        } catch (Exception e) {
            System.err.println("⚠️ Greška pri slanju upload event-a: " + e.getMessage());
        }
    }

    private String generateTranscodedVideoPath(String originalPath) {
        File originalFile = new File(originalPath);
        String parentPath = originalFile.getParent();
        String fileName = originalFile.getName();
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = fileName.substring(fileName.lastIndexOf('.'));

        String transcodedDir = parentPath + File.separator + "transcoded";
        new File(transcodedDir).mkdirs();

        return transcodedDir + File.separator + nameWithoutExt + "_720p" + extension;
    }
}
