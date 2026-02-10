package com.example.jutjubic.repository;

import com.example.jutjubic.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findAllByOrderByCreatedAtDesc();

    List<Video> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Query("SELECT v FROM Video v WHERE v.latitude IS NOT NULL " +
            "AND v.latitude BETWEEN :minLat AND :maxLat " +
            "AND v.longitude BETWEEN :minLon AND :maxLon")
    List<Video> findByLocationWithinBounds(
            @Param("minLat") double minLat,
            @Param("minLon") double minLon,
            @Param("maxLat") double maxLat,
            @Param("maxLon") double maxLon
    );

    @Query("SELECT v FROM Video v WHERE v.latitude IS NOT NULL AND v.longitude IS NOT NULL")
    List<Video> findAllWithLocation();

    @Query("SELECT COUNT(v) FROM Video v WHERE v.latitude IS NOT NULL AND v.longitude IS NOT NULL")
    long countVideosWithLocation();

    // Metode za kompresiju slika
    List<Video> findByThumbnailCompressedFalseAndCreatedAtBefore(LocalDateTime date);

    long countByThumbnailCompressed(Boolean compressed);

    long countByThumbnailCompressedFalseAndCreatedAtBefore(LocalDateTime date);
}