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

    // dobavi sve komentare za odredjeni video, sortirane od najnovijeg
    List<Comment> findByVideoOrderByCreatedAtDesc(Video video);

    // dobavi komentare sa paginacijom
    Page<Comment> findByVideoOrderByCreatedAtDesc(Video video, Pageable pageable);

    // dobavi sve komentare za odredjeni video po id-u
    List<Comment> findByVideoIdOrderByCreatedAtDesc(Long videoId);

    // prebroj komentare za odredjeni video
    long countByVideo(Video video);
}