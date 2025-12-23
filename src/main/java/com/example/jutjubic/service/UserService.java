package com.example.jutjubic.service;

import com.example.jutjubic.dto.UserProfileResponse;
import com.example.jutjubic.model.User;
import com.example.jutjubic.repository.UserRepository;
import com.example.jutjubic.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    /**
     * Dobavi javni profil korisnika - dostupno svima
     * Ne prikazuje osetljive informacije kao što su email i adresa
     */
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Korisnik nije pronađen"));

        // Broji koliko videa ima korisnik
        Long videoCount = (long) videoRepository.findByUserIdOrderByCreatedAtDesc(userId).size();

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt(),
                videoCount
        );
    }
}