package com.example.jutjubic;

import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.UserRepository;
import com.example.jutjubic.repository.VideoRepository;
import com.example.jutjubic.service.JwtService;
import com.example.jutjubic.service.CommentRateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CommentRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CommentRateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Video testVideo;
    private String jwtToken;

    @BeforeEach
    public void setUp() {
        rateLimitService.resetCounters();


        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        testUser = new User();
        testUser.setEmail("testuser" + uniqueId + "@test.com");
        testUser.setUsername("testuser" + uniqueId);
        testUser.setPassword("hashedpassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setAddress("Test Address 123");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        testVideo = new Video();
        testVideo.setTitle("Test Video " + uniqueId);
        testVideo.setDescription("Test description");
        testVideo.setThumbnailPath("test/thumbnail.jpg");
        testVideo.setVideoPath("test/video.mp4");
        testVideo.setUser(testUser);
        testVideo.setCreatedAt(LocalDateTime.now());
        testVideo = videoRepository.save(testVideo);

        jwtToken = jwtService.generateToken(testUser.getEmail());
    }

    @Test
    public void testRateLimitEnforcement() throws Exception {
        System.out.println("\n=== RATE LIMIT TEST: Započinjem slanje 61 komentara ===\n");

        int successfulComments = 0;
        int failedComments = 0;

        for (int i = 1; i <= 61; i++) {
            Map<String, String> commentRequest = new HashMap<>();
            commentRequest.put("text", "Test komentar broj " + i);

            MvcResult result = mockMvc.perform(post("/api/videos/" + testVideo.getId() + "/comments")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andReturn();

            int status = result.getResponse().getStatus();

            if (status == 200) {
                successfulComments++;
                System.out.println("✅ Komentar " + i + "/61: USPEŠNO (200 OK)");
            } else if (status == 400) {
                failedComments++;
                String responseBody = result.getResponse().getContentAsString();
                System.out.println("❌ Komentar " + i + "/61: ODBIJEN (400 Bad Request)");
                System.out.println("   Poruka: " + responseBody);
            } else {
                fail("Neočekivan status kod: " + status + " za komentar " + i);
            }
        }

        System.out.println("\n=== REZULTATI ===");
        System.out.println("Uspešni komentari: " + successfulComments);
        System.out.println("Odbijeni komentari: " + failedComments);
        System.out.println("==================\n");

        assertEquals(60, successfulComments,
                "Tačno 60 komentara bi trebalo da uspe pre rate limiting-a");
        assertEquals(1, failedComments,
                "61. komentar bi trebalo da bude odbijen (rate limited)");
    }

    @Test
    public void testRateLimitStatusEndpoint() throws Exception {
        System.out.println("\n=== TEST: Rate Limit Status Endpoint ===\n");

        // posaljem 10 komentara
        for (int i = 1; i <= 10; i++) {
            Map<String, String> commentRequest = new HashMap<>();
            commentRequest.put("text", "Test komentar " + i);

            mockMvc.perform(post("/api/videos/" + testVideo.getId() + "/comments")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isOk());
        }

        System.out.println("✅ Poslato 10 komentara");

        // proverim status
        MvcResult afterResult = mockMvc.perform(
                        get("/api/videos/" + testVideo.getId() + "/comments/rate-limit")
                                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = afterResult.getResponse().getContentAsString();
        System.out.println("Status posle 10 komentara: " + responseBody);

        Map<String, Integer> response = objectMapper.readValue(responseBody, Map.class);
        assertEquals(50, response.get("remainingComments"),
                "Trebalo bi da ostane 50 komentara (60 - 10)");

        System.out.println("==================\n");
    }

    @Test
    public void testRateLimitPerUser() throws Exception {
        System.out.println("\n=== TEST: Nezavisni Rate Limit Po Korisniku ===\n");

        // kreiram drugog korisnika sa unique podacima
        String uniqueId2 = UUID.randomUUID().toString().substring(0, 8);
        User secondUser = new User();
        secondUser.setEmail("seconduser" + uniqueId2 + "@test.com");
        secondUser.setUsername("seconduser" + uniqueId2);
        secondUser.setPassword("hashedpassword");
        secondUser.setFirstName("Second");
        secondUser.setLastName("User");
        secondUser.setAddress("Second Address 456");
        secondUser.setCreatedAt(LocalDateTime.now());
        secondUser = userRepository.save(secondUser);

        String secondUserToken = jwtService.generateToken(secondUser.getEmail());

        // prvi korisnik salje 60 komentara
        for (int i = 1; i <= 60; i++) {
            Map<String, String> commentRequest = new HashMap<>();
            commentRequest.put("text", "Komentar prvog korisnika " + i);

            mockMvc.perform(post("/api/videos/" + testVideo.getId() + "/comments")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isOk());
        }

        System.out.println("✅ Prvi korisnik: Poslao 60 komentara (dostigao limit)");

        // prvi korisnik pokusava 61. komentar
        Map<String, String> firstUserExtraComment = new HashMap<>();
        firstUserExtraComment.put("text", "61. komentar prvog korisnika");

        mockMvc.perform(post("/api/videos/" + testVideo.getId() + "/comments")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUserExtraComment)))
                .andExpect(status().isBadRequest());

        System.out.println("❌ Prvi korisnik: 61. komentar odbijen (rate limited)");

        // drugi korisnik salje komentar (trebalo bi da uspe)
        Map<String, String> secondUserComment = new HashMap<>();
        secondUserComment.put("text", "Komentar drugog korisnika");

        mockMvc.perform(post("/api/videos/" + testVideo.getId() + "/comments")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUserComment)))
                .andExpect(status().isOk());

        System.out.println("✅ Drugi korisnik: Komentar prihvaćen (ima svoj nezavisan limit)");
        System.out.println("\n==================\n");
    }
}