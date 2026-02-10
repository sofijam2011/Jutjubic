# 🧪 Test Guide - Video Transcoding System

## Pre-requisites Setup

### 1. Install FFmpeg
```bash
# macOS
brew install ffmpeg

# Verify installation
ffmpeg -version
```

### 2. Start RabbitMQ
```bash
# Start RabbitMQ container
docker run -d --name jutjubic-rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3-management

# Check if running
docker ps | grep rabbitmq

# Access Management UI
open http://localhost:15672
# Login: guest/guest
```

---

## Testing Steps

### Step 1: Start Spring Boot Application

```bash
cd /Users/paun/IdeaProjects/Jutjubic
mvn spring-boot:run
```

**Expected output:**
```
🎬 Starting transcoding consumers...
✅ Consumer 1 ready and listening
✅ Consumer 2 ready and listening
✅ Connected to RabbitMQ at localhost:5672
```

---

### Step 2: Test Health Endpoint

Open a new terminal and run:

```bash
curl http://localhost:8081/api/transcoding/health | jq
```

**Expected response:**
```json
{
  "ffmpeg": {
    "installed": true,
    "status": "OK"
  },
  "rabbitmq": {
    "connected": true,
    "status": "OK",
    "queue": "video.transcoding.queue"
  },
  "status": "HEALTHY"
}
```

---

### Step 3: Test Info Endpoint

```bash
curl http://localhost:8081/api/transcoding/info | jq
```

**Expected response:**
```json
{
  "description": "Video Transcoding System with RabbitMQ",
  "consumers": "Minimum 2 concurrent workers",
  "queue": "video.transcoding.queue",
  "acknowledgment": "MANUAL (exactly-once delivery)",
  "ffmpeg_presets": {
    "720p": "1280x720, 2000k bitrate, libx264, aac",
    "1080p": "1920x1080, 4000k bitrate, libx264, aac"
  },
  "rabbitmq_ui": "http://localhost:15672",
  "setup_guide": "See TRANSCODING_SETUP.md"
}
```

---

### Step 4: Upload a Video (Frontend or API)

**Option A: Using Frontend**
1. Open http://localhost:3000
2. Login to your account
3. Upload a new video with title, description, and files
4. Check console logs for transcoding job message

**Option B: Using curl (API)**
```bash
# First, login to get JWT token
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-email@example.com",
    "password": "your-password"
  }'

# Copy the JWT token from response

# Upload video
curl -X POST http://localhost:8081/api/videos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "title=Test Video" \
  -F "description=Testing transcoding" \
  -F "video=@/path/to/your/video.mp4" \
  -F "thumbnail=@/path/to/your/thumbnail.jpg"
```

---

### Step 5: Monitor Transcoding Process

#### A) Check Spring Boot Logs
Look for these messages in the terminal where Spring Boot is running:

```
📤 Šaljem transcoding job u queue: TranscodingMessage{videoId=1, ...}
✅ Poruka uspešno poslata u queue
🎬 [Consumer-1] Primljena poruka za transcoding: TranscodingMessage{videoId=1, ...}
🎥 Pokrećem FFmpeg komandu: ffmpeg -i input.mp4 ...
[FFmpeg] frame= 1234 fps=30 ...
✅ [Consumer-1] Transcoding uspešno završen za video ID: 1
```

#### B) Check RabbitMQ Management UI
1. Open http://localhost:15672
2. Go to **Queues** tab
3. Click on `video.transcoding.queue`
4. Observe:
   - **Ready**: Number of messages waiting
   - **Unacked**: Number of messages being processed
   - **Total**: Total messages
   - **Message rates**: Messages/sec

#### C) Check Consumers
In RabbitMQ UI, under the queue details:
- Scroll to **Consumers** section
- You should see **at least 2 consumers** listed
- Each consumer will show:
  - Consumer tag
  - Prefetch count: 1
  - Acknowledgement mode: MANUAL

---

### Step 6: Verify Transcoded Video

```bash
# Check if transcoded video was created
ls -lh /Users/paun/IdeaProjects/Jutjubic/uploads/videos/transcoded/

# Expected output:
# video_xxx_720p.mp4
```

**Verify video properties:**
```bash
ffprobe uploads/videos/transcoded/video_xxx_720p.mp4
```

You should see:
- Resolution: 1280x720
- Video codec: h264
- Audio codec: aac

---

## Advanced Testing

### Test 1: Multiple Concurrent Uploads

Upload multiple videos simultaneously to test if consumers work in parallel:

```bash
# Terminal 1
curl -X POST http://localhost:8081/api/videos ... (video 1)

# Terminal 2 (at the same time)
curl -X POST http://localhost:8081/api/videos ... (video 2)

# Terminal 3 (at the same time)
curl -X POST http://localhost:8081/api/videos ... (video 3)
```

**Expected behavior:**
- Multiple consumers will pick up different messages
- Each message is processed by exactly ONE consumer
- Logs will show different consumer IDs processing different videos

---

### Test 2: Consumer Failure Recovery

1. Upload a video
2. While transcoding is in progress, kill the Spring Boot app (Ctrl+C)
3. Restart the app
4. The message should be redelivered and processed again

**This tests:**
- Message persistence in RabbitMQ
- Exactly-once delivery (NACK when consumer fails)
- Automatic redelivery

---

### Test 3: Dead Letter Queue

To test DLQ, you can modify FFmpegService to throw an exception:

```java
// In FFmpegService.transcodeVideo() - temporarily add:
if (true) {
    throw new RuntimeException("Test DLQ");
}
```

**Expected:**
- Consumer receives message
- Processing fails
- Message is sent to `video.transcoding.dlq`
- Check DLQ in RabbitMQ UI

---

### Test 4: Load Testing

Upload 10 videos rapidly and monitor:
- Queue depth in RabbitMQ
- Consumer processing rates
- System resources (CPU, memory)

```bash
# Create a simple load test script
for i in {1..10}; do
  curl -X POST http://localhost:8081/api/videos \
    -H "Authorization: Bearer YOUR_TOKEN" \
    -F "title=Test Video $i" \
    -F "video=@test-video.mp4" \
    -F "thumbnail=@test-thumb.jpg" &
done
wait
```

---

## Troubleshooting

### Issue: FFmpeg not found
```bash
brew install ffmpeg
# or
sudo apt install ffmpeg
```

### Issue: RabbitMQ connection refused
```bash
# Check if RabbitMQ is running
docker ps | grep rabbitmq

# Check logs
docker logs jutjubic-rabbitmq

# Restart RabbitMQ
docker restart jutjubic-rabbitmq
```

### Issue: No consumers visible
- Check `application.properties`:
  ```properties
  spring.rabbitmq.listener.simple.concurrency=2
  ```
- Restart Spring Boot app
- Check logs for consumer initialization

### Issue: Transcoding fails
- Check FFmpeg installation: `ffmpeg -version`
- Check input video path exists
- Check output directory permissions
- Check FFmpeg logs in Spring Boot console

---

## Success Criteria ✅

Your transcoding system is working correctly if:

1. ✅ Health endpoint shows all systems "OK"
2. ✅ At least 2 consumers are visible in RabbitMQ UI
3. ✅ Video upload triggers a message in the queue
4. ✅ Consumer picks up the message and starts FFmpeg
5. ✅ Transcoded video appears in `uploads/videos/transcoded/`
6. ✅ Original video resolution is converted to 720p
7. ✅ Each message is processed exactly once
8. ✅ Multiple videos can be processed concurrently

---

## Monitoring Commands

```bash
# Watch RabbitMQ queue in real-time
watch -n 1 'curl -s http://localhost:8081/api/transcoding/health | jq'

# Monitor transcoded files
watch -n 2 'ls -lh uploads/videos/transcoded/'

# Watch Spring Boot logs for transcoding
tail -f logs/spring.log | grep -i transcoding

# Monitor system resources
htop  # or 'top' on macOS
```

---

## Next Steps

After successful testing:
1. Configure multiple Spring Boot instances for more workers
2. Add monitoring with Prometheus/Grafana
3. Implement progress tracking for transcoding jobs
4. Add webhooks to notify when transcoding is complete
5. Implement multiple quality presets (480p, 720p, 1080p, 4K)
