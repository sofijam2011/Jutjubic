package com.example.jutjubic.repository;

import com.example.jutjubic.model.Comment;
import com.example.jutjubic.model.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Dobavi sve komentare za određeni video, sortirane od najnovijeg
    List<Comment> findByVideoOrderByCreatedAtDesc(Video video);

    // Dobavi komentare sa paginacijom
    Page<Comment> findByVideoOrderByCreatedAtDesc(Video video, Pageable pageable);

    // Dobavi sve komentare za određeni video po ID-u
    List<Comment> findByVideoIdOrderByCreatedAtDesc(Long videoId);

    // Prebroj komentare za određeni video
    long countByVideo(Video video);
}