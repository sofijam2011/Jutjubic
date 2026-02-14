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

    List<Comment> findByVideoOrderByCreatedAtDesc(Video video);

    Page<Comment> findByVideoOrderByCreatedAtDesc(Video video, Pageable pageable);

    List<Comment> findByVideoIdOrderByCreatedAtDesc(Long videoId);

    long countByVideo(Video video);
}
