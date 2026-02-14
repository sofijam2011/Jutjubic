package com.example.jutjubic.util;

import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.UserRepository;
import com.example.jutjubic.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

public class VideoTestDataGenerator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(VideoTestDataGenerator.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    private static final double MIN_LAT = 35.0;
    private static final double MAX_LAT = 71.0;
    private static final double MIN_LON = -10.0;
    private static final double MAX_LON = 40.0;

    private static final int VIDEO_COUNT = 5000;

    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {

        long existingCount = videoRepository.countVideosWithLocation();
        if (existingCount >= VIDEO_COUNT) {
            return;
        }

        User testUser = userRepository.findByUsername("test_map_user")
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername("test_map_user");
                    user.setEmail("test_map@jutjubic.com");
                    user.setPassword("test123");
                    user.setFirstName("Test");
                    user.setLastName("MapUser");
                    user.setAddress("Test Address 123");
                    return userRepository.save(user);
                });

        logger.info("Test korisnik: {}", testUser.getUsername());

        String[] locations = {
                "Beograd", "Zagreb", "Ljubljana", "Sarajevo", "Podgorica",
                "Paris", "London", "Berlin", "Madrid", "Rome",
                "Vienna", "Prague", "Budapest", "Warsaw", "Athens",
                "Amsterdam", "Brussels", "Copenhagen", "Stockholm", "Oslo",
                "Helsinki", "Lisbon", "Dublin", "Edinburgh", "Barcelona"
        };

        String[] activities = {
                "Walking Tour", "Street Food", "Sunset View", "City Center",
                "Historic Sites", "Local Market", "Night Life", "Architecture",
                "Parks and Gardens", "River Walk", "Museum Visit", "Concert",
                "Festival", "Shopping District", "Cafe Culture", "Street Art"
        };

        logger.info("Generisanje {} test video snimaka...", VIDEO_COUNT);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < VIDEO_COUNT; i++) {
            Video video = new Video();

            String location = locations[random.nextInt(locations.length)];
            String activity = activities[random.nextInt(activities.length)];
            video.setTitle(activity + " in " + location + " #" + (i + 1));

            video.setDescription("Exploring " + location + " - " + activity.toLowerCase());

            video.setVideoPath("./uploads/videos/test_sample.mp4");
            video.setThumbnailPath("./uploads/thumbnails/test_thumbnail.PNG");

            double latitude = MIN_LAT + (MAX_LAT - MIN_LAT) * random.nextDouble();
            double longitude = MIN_LON + (MAX_LON - MIN_LON) * random.nextDouble();
            video.setLatitude(latitude);
            video.setLongitude(longitude);

            long daysAgo = random.nextInt(730);
            video.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));

            video.setViewCount((long) (Math.abs(random.nextGaussian()) * 1000 + 100));

            video.setUser(testUser);

            videoRepository.save(video);

            if ((i + 1) % 500 == 0) {
                logger.info("Generisano {} / {} video snimaka", i + 1, VIDEO_COUNT);
            }
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        logger.info("Kreirano {} video snimaka za {} sekundi", VIDEO_COUNT, duration);
        logger.info("Prosečno vreme po video snimku: {} ms",
                (System.currentTimeMillis() - startTime) / VIDEO_COUNT);
    }
}
