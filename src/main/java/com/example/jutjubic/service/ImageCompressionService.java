package com.example.jutjubic.service;

import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.VideoRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servis za periodičnu kompresiju thumbnail slika
 */
@Service
public class ImageCompressionService {

    @Autowired
    private VideoRepository videoRepository;

    private static final int DAYS_THRESHOLD = 30; // Kompresuj slike starije od 30 dana
    private static final double COMPRESSION_QUALITY = 0.7; // 70% kvalitet (0.0 - 1.0)
    private static final String COMPRESSED_SUFFIX = "_compressed";

    /**
     * Scheduled task koji se pokreće svaki dan u ponoć (00:00)
     * Cron format: "sekund minut sat dan mesec dan_nedelje"
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void compressOldThumbnails() {
        System.out.println("🗜️  [" + LocalDateTime.now() + "] Pokrećem periodičnu kompresiju slika...");

        // Pronađi sve videe sa nekompresovanim thumbnail-ima starijim od 30 dana
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(DAYS_THRESHOLD);
        List<Video> videosToCompress = videoRepository.findByThumbnailCompressedFalseAndCreatedAtBefore(thresholdDate);

        if (videosToCompress.isEmpty()) {
            System.out.println("✅ Nema slika za kompresiju.");
            return;
        }

        System.out.println("📊 Pronađeno " + videosToCompress.size() + " slika za kompresiju (starijih od " + DAYS_THRESHOLD + " dana)");

        int successCount = 0;
        int failCount = 0;

        for (Video video : videosToCompress) {
            try {
                compressThumbnail(video);
                successCount++;
                System.out.println("  ✅ Kompresovana slika za video ID: " + video.getId());
            } catch (Exception e) {
                failCount++;
                System.err.println("  ❌ Greška pri kompresiji slike za video ID: " + video.getId() + " - " + e.getMessage());
            }
        }

        System.out.println("🎉 Kompresija završena! Uspešno: " + successCount + ", Neuspešno: " + failCount);
    }

    /**
     * Kompresuje thumbnail sliku za određeni video
     */
    @Transactional
    public void compressThumbnail(Video video) throws IOException {
        String originalPath = video.getThumbnailPath();
        File originalFile = new File(originalPath);

        if (!originalFile.exists()) {
            throw new IOException("Original thumbnail ne postoji: " + originalPath);
        }

        // Kreiraj putanju za kompresovanu sliku
        String compressedPath = generateCompressedPath(originalPath);
        File compressedFile = new File(compressedPath);

        // Kreiraj direktorijum ako ne postoji
        File parentDir = compressedFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Kompresuj sliku koristeći Thumbnailator
        // Održava originalnu rezoluciju, ali smanjuje kvalitet (JPEG kompresija)
        Thumbnails.of(originalFile)
                .scale(1.0)  // Zadrži originalnu veličinu
                .outputQuality(COMPRESSION_QUALITY)  // 70% kvalitet
                .outputFormat("jpg")
                .toFile(compressedFile);

        // Izračunaj kompresioni ratio
        long originalSize = originalFile.length();
        long compressedSize = compressedFile.length();
        double compressionRatio = (1.0 - ((double) compressedSize / originalSize)) * 100;

        System.out.println("    📉 Original: " + formatBytes(originalSize) +
                " → Compressed: " + formatBytes(compressedSize) +
                " (ušteda: " + String.format("%.1f", compressionRatio) + "%)");

        // Ažuriraj video entitet
        video.setThumbnailCompressed(true);
        video.setThumbnailCompressedPath(compressedPath);
        video.setThumbnailCompressionDate(LocalDateTime.now());
        videoRepository.save(video);
    }

    /**
     * Generiše putanju za kompresovanu sliku
     * Primer: uploads/thumbnails/thumb_123.png -> uploads/thumbnails/compressed/thumb_123_compressed.jpg
     */
    private String generateCompressedPath(String originalPath) {
        File originalFile = new File(originalPath);
        String parentPath = originalFile.getParent();
        String fileName = originalFile.getName();
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));

        // Kreiraj compressed direktorijum
        String compressedDir = parentPath + File.separator + "compressed";

        return compressedDir + File.separator + nameWithoutExt + COMPRESSED_SUFFIX + ".jpg";
    }

    /**
     * Formatira bajtove u čitljiv format (KB, MB)
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Manuelni trigger za testiranje (može se pozvati iz kontrolera)
     */
    @Transactional
    public void compressAllOldThumbnails() {
        compressOldThumbnails();
    }

    /**
     * Kompresuje specifičan video thumbnail (za testiranje)
     */
    @Transactional
    public void compressThumbnailById(Long videoId) throws IOException {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoId));

        if (video.getThumbnailCompressed() != null && video.getThumbnailCompressed()) {
            throw new IllegalStateException("Thumbnail je već kompresovan");
        }

        compressThumbnail(video);
    }

    /**
     * Vraća statistiku kompresije
     */
    public CompressionStats getCompressionStats() {
        long totalVideos = videoRepository.count();
        long compressedCount = videoRepository.countByThumbnailCompressed(true);
        long uncompressedCount = videoRepository.countByThumbnailCompressed(false);

        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(DAYS_THRESHOLD);
        long eligibleForCompression = videoRepository.countByThumbnailCompressedFalseAndCreatedAtBefore(thresholdDate);

        return new CompressionStats(totalVideos, compressedCount, uncompressedCount, eligibleForCompression);
    }

    /**
     * DTO za statistiku kompresije
     */
    public static class CompressionStats {
        private long totalVideos;
        private long compressedCount;
        private long uncompressedCount;
        private long eligibleForCompression;

        public CompressionStats(long totalVideos, long compressedCount, long uncompressedCount, long eligibleForCompression) {
            this.totalVideos = totalVideos;
            this.compressedCount = compressedCount;
            this.uncompressedCount = uncompressedCount;
            this.eligibleForCompression = eligibleForCompression;
        }

        // Getters
        public long getTotalVideos() {
            return totalVideos;
        }

        public long getCompressedCount() {
            return compressedCount;
        }

        public long getUncompressedCount() {
            return uncompressedCount;
        }

        public long getEligibleForCompression() {
            return eligibleForCompression;
        }

        public double getCompressionPercentage() {
            if (totalVideos == 0) return 0;
            return (compressedCount * 100.0) / totalVideos;
        }
    }
}
