package com.example.jutjubic.util;

import com.example.jutjubic.model.Video;
import com.example.jutjubic.model.VideoView;
import com.example.jutjubic.repository.VideoRepository;
import com.example.jutjubic.repository.VideoViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

public class VideoViewTestDataGenerator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(VideoViewTestDataGenerator.class);

    @Autowired
    private VideoViewRepository videoViewRepository;

    @Autowired
    private VideoRepository videoRepository;

    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        logger.info("🎬 Pokretanje generatora test podataka za video preglede...");

        long existingViews = videoViewRepository.count();
        if (existingViews > 100) {
            logger.info("Već postoji {} pregleda, preskačem generisanje", existingViews);
            return;
        }

        List<Video> videos = videoRepository.findAll();

        if (videos.isEmpty()) {
            logger.warn("⚠️ Nema videa u bazi! Preskačem generisanje.");
            return;
        }

        logger.info("📊 Pronađeno {} videa", videos.size());

        int totalViews = 0;

        for (int daysAgo = 0; daysAgo < 7; daysAgo++) {
            LocalDateTime viewDate = LocalDateTime.now().minusDays(daysAgo);

            int viewsForDay = (7 - daysAgo) * 5;

            for (int i = 0; i < viewsForDay; i++) {
                Video randomVideo = videos.get(random.nextInt(Math.min(10, videos.size())));

                LocalDateTime exactTime = viewDate
                    .withHour(random.nextInt(24))
                    .withMinute(random.nextInt(60))
                    .withSecond(random.nextInt(60));

                VideoView view = new VideoView(randomVideo, exactTime);

                videoViewRepository.save(view);
                totalViews++;
            }

            logger.info("✅ Kreirano {} pregleda za dan {} (pre {} dana)",
                viewsForDay, viewDate.toLocalDate(), daysAgo);
        }

        logger.info("🎉 Ukupno kreirano {} test pregleda za ETL pipeline testiranje!", totalViews);
        logger.info("💡 Sada možeš pokrenuti ETL pipeline i testirati popularity score kalkulaciju");
    }
}
