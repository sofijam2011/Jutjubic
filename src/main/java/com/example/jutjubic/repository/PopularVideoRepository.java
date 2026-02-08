package com.example.jutjubic.repository;

import com.example.jutjubic.model.PopularVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PopularVideoRepository extends JpaRepository<PopularVideo, Long> {

    @Query("SELECT pv FROM PopularVideo pv WHERE pv.pipelineRunAt = " +
           "(SELECT MAX(pv2.pipelineRunAt) FROM PopularVideo pv2) " +
           "ORDER BY pv.rankPosition ASC")
    List<PopularVideo> findLatestPopularVideos();
}
