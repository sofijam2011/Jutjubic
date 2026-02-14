package com.example.jutjubic.repository;

import com.example.jutjubic.model.Like;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByVideoAndUser(Video video, User user);

    void deleteByVideoAndUser(Video video, User user);

    long countByVideo(Video video);

    boolean existsByVideoAndUser(Video video, User user);
}
