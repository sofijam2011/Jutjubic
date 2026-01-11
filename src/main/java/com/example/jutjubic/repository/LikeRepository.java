package com.example.jutjubic.repository;

import com.example.jutjubic.model.Like;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    //proveri da li je korisnik vec lajkovao video
    Optional<Like> findByVideoAndUser(Video video, User user);

    //obrisi like
    void deleteByVideoAndUser(Video video, User user);

    //broj lajkova za video
    long countByVideo(Video video);

    //da li je korisnik lajkovao ovaj video
    boolean existsByVideoAndUser(Video video, User user);
}