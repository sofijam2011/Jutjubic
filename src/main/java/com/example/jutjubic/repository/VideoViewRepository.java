package com.example.jutjubic.repository;

import com.example.jutjubic.model.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VideoViewRepository extends JpaRepository<VideoView, Long> {

    @Query("SELECT vv FROM VideoView vv WHERE vv.viewedAt >= :startDate")
    List<VideoView> findViewsSince(@Param("startDate") LocalDateTime startDate);
}
