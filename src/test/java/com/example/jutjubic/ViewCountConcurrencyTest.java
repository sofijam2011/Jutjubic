package com.example.jutjubic;

import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.UserRepository;
import com.example.jutjubic.repository.VideoRepository;
import com.example.jutjubic.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ViewCountConcurrencyTest {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    private Long testVideoId;

    @BeforeEach
    public void setup() {
        // Očisti bazu pre svakog testa
        videoRepository.deleteAll();
        userRepository.deleteAll();

        // Kreiraj test korisnika
        User testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setAddress("Test Address");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Kreiraj test video
        Video testVideo = new Video();
        testVideo.setTitle("Test Video for Concurrency");
        testVideo.setDescription("Testing concurrent view count WITHOUT optimistic locking");
        testVideo.setThumbnailPath("test/thumbnail.jpg");
        testVideo.setVideoPath("test/video.mp4");
        testVideo.setUser(testUser);
        testVideo.setViewCount(0L);
        testVideo.setCreatedAt(LocalDateTime.now());
        testVideo = videoRepository.save(testVideo);

        testVideoId = testVideo.getId();

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  TEST SETUP COMPLETED                                    ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Created Test Video ID: " + String.format("%-33d", testVideoId) + "║");
        System.out.println("║  Initial View Count: 0                                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    /**
     * TEST 1: 100 Konkurentnih Threadova
     * Simulira 100 korisnika koji istovremeno gledaju isti video
     */
    @Test
    public void testConcurrentViewCount_100Threads() throws InterruptedException {
        int numberOfThreads = 100;
        runConcurrencyTest(numberOfThreads, "100 THREADOVA");
    }

    /**
     * TEST 2: 500 Konkurentnih Threadova (Stress Test)
     * Ekstremno opterećenje - proverava da li sistem puca
     */
    @Test
    public void testConcurrentViewCount_500Threads_StressTest() throws InterruptedException {
        int numberOfThreads = 500;
        runConcurrencyTest(numberOfThreads, "500 THREADOVA (STRESS TEST)");
    }

    /**
     * TEST 3: 10 Threadova sa Delays (Real-world scenario)
     * Simulira realniju situaciju sa malim delay-ima između zahteva
     */
    @Test
    public void testConcurrentViewCount_WithDelays() throws InterruptedException {
        int numberOfThreads = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Video initialVideo = videoRepository.findById(testVideoId).orElseThrow();
        Long initialViewCount = initialVideo.getViewCount();

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  REAL-WORLD SCENARIO TEST                                ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Threads: 50                                             ║");
        System.out.println("║  Delays: Random 0-100ms                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i + 1;
            executorService.submit(() -> {
                try {
                    // Random delay (simulira realno korištenje)
                    Thread.sleep((long) (Math.random() * 100));

                    videoService.incrementViewCount(testVideoId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("  ✗ Thread " + threadNum + " FAILED: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        videoRepository.flush();

        Video finalVideo = videoRepository.findById(testVideoId).orElseThrow();
        Long expectedViewCount = initialViewCount + numberOfThreads;

        printResults(numberOfThreads, successCount.get(), failureCount.get(),
                initialViewCount, finalVideo.getViewCount(), expectedViewCount, duration);

        assertEquals(expectedViewCount, finalVideo.getViewCount(),
                "Real-world scenario failed! Expected: " + expectedViewCount + ", Got: " + finalVideo.getViewCount());
    }

    /**
     * Zajednička metoda za pokretanje konkurentnih testova
     */
    private void runConcurrencyTest(int numberOfThreads, String testName) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Video initialVideo = videoRepository.findById(testVideoId).orElseThrow();
        Long initialViewCount = initialVideo.getViewCount();

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  STARTING CONCURRENCY TEST: " + String.format("%-28s", testName) + "║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Method: ATOMIC UPDATE (Direct DB Query)                ║");
        System.out.println("║  Video ID: " + String.format("%-47d", testVideoId) + "║");
        System.out.println("║  Initial View Count: " + String.format("%-37d", initialViewCount) + "║");
        System.out.println("║  Concurrent Threads: " + String.format("%-37d", numberOfThreads) + "║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        long startTime = System.currentTimeMillis();

        // Pokreni sve threadove
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i + 1;
            executorService.submit(() -> {
                try {
                    videoService.incrementViewCount(testVideoId);
                    successCount.incrementAndGet();

                    if (threadNum % 100 == 0) {
                        System.out.println("  ✓ Thread " + threadNum + " completed successfully");
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("  ✗ Thread " + threadNum + " FAILED: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        if (!completed) {
            System.err.println("\n⚠ WARNING: Not all threads completed within 60 seconds!");
        }

        videoRepository.flush();

        Video finalVideo = videoRepository.findById(testVideoId).orElseThrow();
        Long expectedViewCount = initialViewCount + numberOfThreads;

        printResults(numberOfThreads, successCount.get(), failureCount.get(),
                initialViewCount, finalVideo.getViewCount(), expectedViewCount, duration);

        assertEquals(expectedViewCount, finalVideo.getViewCount(),
                "\n❌ ATOMIC UPDATE FAILED!\n" +
                        "   Expected all " + numberOfThreads + " threads to succeed.\n" +
                        "   View count should be: " + expectedViewCount + "\n" +
                        "   But got: " + finalVideo.getViewCount());

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  ✅ TEST PASSED: " + String.format("%-43s", testName) + "║");
        System.out.println("║  Atomic update guarantees consistency!                   ║");
        System.out.println("║  All " + String.format("%-3d", numberOfThreads) + " concurrent requests succeeded!                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    /**
     * Štampanje rezultata testa
     */
    private void printResults(int totalThreads, int successCount, int failureCount,
                              Long initialCount, Long finalCount, Long expectedCount, long duration) {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  TEST RESULTS                                            ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Execution Time: " + String.format("%-41s", duration + " ms") + "║");
        System.out.println("║  Threads/sec: " + String.format("%-44s",
                String.format("%.2f", (double) totalThreads / duration * 1000)) + "║");
        System.out.println("║  Successful Increments: " + String.format("%-34d", successCount) + "║");
        System.out.println("║  Failed Increments: " + String.format("%-38d", failureCount) + "║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Initial View Count: " + String.format("%-37d", initialCount) + "║");
        System.out.println("║  Final View Count: " + String.format("%-39d", finalCount) + "║");
        System.out.println("║  Expected View Count: " + String.format("%-36d", expectedCount) + "║");
        System.out.println("║  Difference: " + String.format("%-45d", (finalCount - initialCount)) + "║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}