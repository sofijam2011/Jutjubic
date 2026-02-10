# Video Transcoding sistem - Uputstvo za pokretanje

## Preduslovi

1. **FFmpeg** mora biti instaliran na sistemu
   ```bash
   # macOS
   brew install ffmpeg

   # Ubuntu/Debian
   sudo apt update
   sudo apt install ffmpeg

   # Provera instalacije
   ffmpeg -version
   ```

2. **Docker** mora biti instaliran za RabbitMQ

## Pokretanje sistema

### 1. Pokrenuti RabbitMQ

```bash
docker-compose -f docker-compose-rabbitmq.yml up -d
```

RabbitMQ Management UI će biti dostupan na: http://localhost:15672
- Username: `guest`
- Password: `guest`

### 2. Pokrenuti Spring Boot aplikaciju

```bash
mvn spring-boot:run
```

Aplikacija će automatski:
- Kreirati **video.transcoding.queue** queue u RabbitMQ
- Pokrenuti **minimalno 2 consumera** (worker-a) koji slušaju queue
- Slati transcoding poslove u queue nakon svakog video upload-a

## Arhitektura sistema

```
┌─────────────┐
│   User      │
│  Upload     │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  VideoService   │──┐
│  (Producer)     │  │ Šalje poruku u queue
└─────────────────┘  │
                     ▼
            ┌────────────────┐
            │   RabbitMQ     │
            │     Queue      │
            └────────┬───────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
   ┌─────────┐  ┌─────────┐  ┌─────────┐
   │Consumer1│  │Consumer2│  │Consumer3│
   │ (Worker)│  │ (Worker)│  │ (Worker)│
   └────┬────┘  └────┬────┘  └────┬────┘
        │            │            │
        ▼            ▼            ▼
   ┌─────────────────────────────────┐
   │      FFmpeg Transcoding         │
   └─────────────────────────────────┘
```

## Kako sistem radi

### 1. Upload videa
Kada korisnik upload-uje video:
1. Video se čuva na disk (npr. `uploads/videos/video_123.mp4`)
2. Kreira se `TranscodingMessage` poruka sa:
   - Video ID
   - Putanja do originalnog videa
   - Putanja za transkodovani video
   - Transcoding parametri (720p, codec, bitrate, itd.)
3. Poruka se šalje u `video.transcoding.queue`

### 2. Procesiranje od strane Consumera
- **Minimum 2 consumera** aktivno slušaju queue (konfigurisano u `application.properties`)
- Svaki consumer uzima **po 1 poruku** odjednom (`prefetch=1`)
- **Exactly-once delivery**: Poruka se ne briše iz queue-a dok consumer ne potvrdi obradu (`MANUAL acknowledgment`)
- Consumer poziva `FFmpegService` koji izvršava transcoding

### 3. FFmpeg Transcoding
FFmpeg komanda koja se izvršava (primer za 720p):
```bash
ffmpeg -i input.mp4 \
       -c:v libx264 \
       -b:v 2000k \
       -vf scale=1280:720 \
       -c:a aac \
       -b:a 128k \
       -preset medium \
       -y output_720p.mp4
```

### 4. Rezultat
- Transkodovani video se čuva u `uploads/videos/transcoded/video_123_720p.mp4`
- Consumer potvrđuje uspešnu obradu (`channel.basicAck()`)
- Poruka se briše iz queue-a

## Sprečavanje duplog procesiranja

Sistem koristi **MANUAL acknowledgment mode**:
- Poruka ostaje u queue-u dok consumer ne potvrdi obradu
- Ako consumer padne tokom procesiranja, poruka se vraća u queue
- RabbitMQ garantuje da samo **jedan consumer** obrađuje jednu poruku
- Ako obrada ne uspe, poruka se šalje u **Dead Letter Queue** (DLQ)

## Konfiguracija

### Broj consumera
U `application.properties`:
```properties
spring.rabbitmq.listener.simple.concurrency=2       # Minimum broj consumera
spring.rabbitmq.listener.simple.max-concurrency=4   # Maksimum broj consumera
```

### Transcoding parametri
Predefinisani presets u `TranscodingMessage.TranscodingParams`:
- `default720p()` - 1280x720, 2000k bitrate
- `default1080p()` - 1920x1080, 4000k bitrate

Za custom parametre, može se kreirati novi preset ili modifikovati postojeće.

### FFmpeg preset
U `FFmpegService.buildFFmpegCommand()`:
```java
command.add("-preset");
command.add("medium"); // ultrafast, fast, medium, slow, veryslow
```

## Testiranje sistema

### 1. Provera RabbitMQ
```bash
# Provera da li je RabbitMQ aktivan
docker ps | grep rabbitmq

# Pristup Management UI
open http://localhost:15672
```

### 2. Upload videa
Koristi frontend ili API endpoint:
```bash
POST http://localhost:8081/api/videos
Content-Type: multipart/form-data

title: Test Video
description: Test Description
video: [video file]
thumbnail: [thumbnail file]
```

### 3. Praćenje logova
```bash
# Logovi Spring Boot aplikacije
tail -f logs/application.log

# Traženje transcoding logova
grep "transcoding" logs/application.log

# Logovi consumera
grep "Consumer" logs/application.log
```

### 4. Provera queue-a u RabbitMQ Management UI
1. Otvori http://localhost:15672
2. Idi na **Queues** tab
3. Vidi `video.transcoding.queue`:
   - **Total** - Broj poruka u queue-u
   - **Ready** - Broj poruka čeka na obradu
   - **Unacked** - Broj poruka koje se trenutno obrađuju

## Rešavanje problema

### FFmpeg nije instaliran
```bash
# Provera da li je FFmpeg dostupan
ffmpeg -version

# Ako nije, instaliraj ga (vidi Preduslovi)
```

### RabbitMQ nije dostupan
```bash
# Restart RabbitMQ
docker-compose -f docker-compose-rabbitmq.yml restart

# Provera logova
docker logs jutjubic-rabbitmq
```

### Consumer ne obrađuje poruke
- Proveri logove aplikacije
- Proveri da li je `spring.rabbitmq.listener.simple.concurrency` podešeno na minimum 2
- Proveri RabbitMQ Management UI da vidiš broj aktivnih consumera

### Transcoding kasni
- Povećaj broj consumera u `application.properties`
- Promeni FFmpeg preset na brži (npr. `fast` umesto `medium`)
- Smanji rezoluciju (npr. 480p umesto 720p)

## Dodatne mogućnosti

### Pokretanje više Spring Boot instanci (za više consumera)
```bash
# Instance 1 (port 8081)
mvn spring-boot:run

# Instance 2 (port 8082)
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082

# Instance 3 (port 8083)
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083
```

Svaka instanca će kreirati svoje consumere, tako da sa 3 instance imaš 6-12 consumera!

### Dodavanje novih transcoding profila
U `VideoService.sendTranscodingJob()`:
```java
// Umesto default720p():
TranscodingMessage.TranscodingParams params = new TranscodingMessage.TranscodingParams(
    "libx264",    // codec
    "1920x1080",  // resolution
    "4000k",      // video bitrate
    "aac",        // audio codec
    "192k",       // audio bitrate
    "mp4"         // format
);
```

### Adaptive Bitrate Streaming (HLS)
Za više transcoding profila (480p, 720p, 1080p), može se modifikovati sistem da šalje više poruka za svaki video, svaka sa različitim parametrima.
