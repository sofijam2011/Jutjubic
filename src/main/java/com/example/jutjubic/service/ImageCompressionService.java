package com.example.jutjubic.service;

import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.VideoRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ImageCompressionService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private ApplicationContext applicationContext;

    private ImageCompressionService self() {
        return applicationContext.getBean(ImageCompressionService.class);
    }

    private static final int DAYS_THRESHOLD = 30;
    private static final double COMPRESSION_QUALITY = 0.7;
    private static final String COMPRESSED_SUFFIX = "_compressed";

    // Scheduled metoda NE smije biti @Transactional — drži DB konekciju tokom cijelog file I/O
    @Scheduled(cron = "0 0 0 * * ?")
    public void compressOldThumbnails() {
        System.out.println("[" + LocalDateTime.now() + "] Pokrećem periodičnu kompresiju slika...");

        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(DAYS_THRESHOLD);

        // Učitaj listu van transakcije — kratka DB operacija
        List<Video> videosToCompress = videoRepository.findByThumbnailCompressedFalseAndCreatedAtBefore(thresholdDate);

        if (videosToCompress.isEmpty()) {
            System.out.println("Nema slika za kompresiju.");
            return;
        }

        System.out.println("Pronađeno " + videosToCompress.size() + " slika za kompresiju (starijih od " + DAYS_THRESHOLD + " dana)");

        int successCount = 0;
        int failCount = 0;

        for (Video video : videosToCompress) {
            try {
                // Poziv kroz Spring proxy (self) — svaki video dobija svoju transakciju
                // DB konekcija se drži samo tokom save(), ne tokom file I/O
                self().compressThumbnailTransactional(video);
                successCount++;
                System.out.println("  Kompresovana slika za video ID: " + video.getId());
            } catch (Exception e) {
                failCount++;
                System.err.println("  Greška pri kompresiji slike za video ID: " + video.getId() + " - " + e.getMessage());
            }
        }

        System.out.println("Kompresija završena! Uspešno: " + successCount + ", Neuspešno: " + failCount);
    }

    // Svaki thumbnail dobija svoju transakciju — file I/O se obavlja VAN transakcije,
    // a DB save se radi na kraju (kratko drži konekciju)
    public void compressThumbnailTransactional(Video video) throws IOException {
        String originalPath = video.getThumbnailPath();
        File originalFile = new File(originalPath);

        if (!originalFile.exists()) {
            throw new IOException("Original thumbnail ne postoji: " + originalPath);
        }

        long originalSize = originalFile.length();
        String compressedPath = generateCompressedPath(originalPath);
        File compressedFile = new File(compressedPath);

        File parentDir = compressedFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // File I/O — obavlja se van transakcije
        Thumbnails.of(originalFile)
                .scale(1.0)
                .outputQuality(COMPRESSION_QUALITY)
                .outputFormat("jpg")
                .toFile(compressedFile);

        long compressedSize = compressedFile.length();
        double compressionRatio = (1.0 - ((double) compressedSize / originalSize)) * 100;

        // Ako kompresovana verzija nije manja, brišemo je i koristimo original
        if (compressedSize >= originalSize) {
            compressedFile.delete();
            System.out.println("    Video ID " + video.getId() + ": kompresija ne smanjuje velicinu ("
                    + formatBytes(originalSize) + "), koristimo original.");
            self().saveCompressionResult(video.getId(), originalPath);
        } else {
            System.out.println("    Video ID " + video.getId() + ": " + formatBytes(originalSize)
                    + " -> " + formatBytes(compressedSize)
                    + " (usteda: " + String.format("%.1f", compressionRatio) + "%)");
            self().saveCompressionResult(video.getId(), compressedPath);
        }
    }

    @Transactional
    public void saveCompressionResult(Long videoId, String compressedPath) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoId));
        video.setThumbnailCompressed(true);
        video.setThumbnailCompressedPath(compressedPath);
        video.setThumbnailCompressionDate(LocalDateTime.now());
        videoRepository.save(video);
    }

    private String generateCompressedPath(String originalPath) {
        File originalFile = new File(originalPath);
        String parentPath = originalFile.getParent();
        String fileName = originalFile.getName();
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));

        String compressedDir = parentPath + File.separator + "compressed";

        return compressedDir + File.separator + nameWithoutExt + COMPRESSED_SUFFIX + ".jpg";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }

    public void compressAllOldThumbnails() {
        compressOldThumbnails();
    }

    @Transactional
    public void compressThumbnailById(Long videoId) throws IOException {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoId));

        if (video.getThumbnailCompressed() != null && video.getThumbnailCompressed()) {
            throw new IllegalStateException("Thumbnail je već kompresovan");
        }

        self().compressThumbnailTransactional(video);
    }

    public CompressionStats getCompressionStats() {
        long totalVideos = videoRepository.count();
        long compressedCount = videoRepository.countByThumbnailCompressed(true);
        long uncompressedCount = videoRepository.countByThumbnailCompressed(false);

        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(DAYS_THRESHOLD);
        long eligibleForCompression = videoRepository.countByThumbnailCompressedFalseAndCreatedAtBefore(thresholdDate);

        return new CompressionStats(totalVideos, compressedCount, uncompressedCount, eligibleForCompression);
    }

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
