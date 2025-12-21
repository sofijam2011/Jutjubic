package com.example.jutjubic.repository;

import com.example.jutjubic.model.Like;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // Proveri da li korisnik već lajkovao video
    Optional<Like> findByVideoAndUser(Video video, User user);

    // Izbriši like
    void deleteByVideoAndUser(Video video, User user);

    // Broj lajkova za video
    long countByVideo(Video video);

    // Da li je korisnik lajkovao ovaj video
    boolean existsByVideoAndUser(Video video, User user);
}