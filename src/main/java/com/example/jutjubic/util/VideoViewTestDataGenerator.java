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

/**
 * Generator test podataka za VideoView - preglede videa
 * Kreira nasumične preglede videa u poslednjih 7 dana za testiranje ETL pipeline-a
 *
 * Za aktiviranje: odkomentiraj @Component anotaciju
 */
//@Component
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

        // Proveri da li već postoje pregledi
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

        // Generiši preglede za poslednjih 7 dana
        int totalViews = 0;

        // Za svaki dan u poslednjih 7 dana
        for (int daysAgo = 0; daysAgo < 7; daysAgo++) {
            LocalDateTime viewDate = LocalDateTime.now().minusDays(daysAgo);

            // Više pregleda za skorašnje dane (težina simulacije)
            int viewsForDay = (7 - daysAgo) * 5; // Danas 35, juče 30, itd.

            for (int i = 0; i < viewsForDay; i++) {
                Video randomVideo = videos.get(random.nextInt(Math.min(10, videos.size())));

                // Dodaj nasumične sate/minute
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
