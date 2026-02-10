# 🗜️ Periodična kompresija slika - Dokumentacija

## Pregled

Sistem automatski kompresuje thumbnail slike koje su starije od **30 dana**, čime se štedi prostor na disku. Originalne slike se **ne brišu**, već se kompresovane verzije čuvaju u posebnom direktorijumu.

## Karakteristike

✅ **Automatsko pokretanje** - Svaki dan u ponoć (00:00)
✅ **Prag starosti** - Kompresuju se samo slike starije od 30 dana
✅ **Čuvanje originala** - Original ostaje netaknut
✅ **Thumbnailator library** - Visok kvalitet kompresije
✅ **70% kvalitet** - Dobar balans između veličine i kvaliteta
✅ **Tracking** - Praćenje statusa kompresije u bazi
✅ **Manual trigger** - Može se pokrenuti ručno za testiranje

---

## Arhitektura

### Baza podataka

Dodati su novi koloni u `videos` tabelu:

```sql
ALTER TABLE videos ADD COLUMN thumbnail_compressed BOOLEAN DEFAULT FALSE;
ALTER TABLE videos ADD COLUMN thumbnail_compressed_path VARCHAR(255);
ALTER TABLE videos ADD COLUMN thumbnail_compression_date TIMESTAMP;
```

### Struktura direktorijuma

```
uploads/
└── thumbnails/
    ├── thumb_123.png          ← Original (čuva se)
    ├── thumb_456.jpg          ← Original (čuva se)
    └── compressed/            ← Novi direktorijum
        ├── thumb_123_compressed.jpg
        └── thumb_456_compressed.jpg
```

### Proces kompresije

```
┌─────────────────────────────────┐
│   Scheduled Task (Svaki dan)   │
│        Ponoć (00:00)            │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│  Pronađi videe sa thumbnail-ima │
│  starije od 30 dana koje još    │
│  nisu kompresovane              │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│  Za svaki video:                │
│  1. Učitaj originalnu sliku     │
│  2. Kompresuj sa Thumbnailator  │
│  3. Sačuvaj u /compressed/      │
│  4. Ažuriraj bazu podataka      │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│  Log statistiku:                │
│  - Broj kompresovanih           │
│  - Ušteda u prostoru            │
│  - Kompresioni ratio            │
└─────────────────────────────────┘
```

---

## Konfiguracija

### Parametri u `ImageCompressionService`:

```java
private static final int DAYS_THRESHOLD = 30;              // Kompresuj slike starije od 30 dana
private static final double COMPRESSION_QUALITY = 0.7;     // 70% kvalitet (0.0 - 1.0)
private static final String COMPRESSED_SUFFIX = "_compressed";
```

### Scheduled Task Cron:

```java
@Scheduled(cron = "0 0 0 * * ?")  // Sekund Minut Sat Dan Mesec DanNedelje
```

**Primeri cron raspored-a:**
- `0 0 0 * * ?` - Svaki dan u ponoć
- `0 0 2 * * ?` - Svaki dan u 2 ujutru
- `0 0 0 * * SUN` - Svake nedelje u ponoć
- `0 0 0 1 * ?` - Prvog dana u mesecu u ponoć

---

## API Endpoints

### 1. Manual Trigger (Testiranje)

Ručno pokreni kompresiju:

```bash
curl -X POST http://localhost:8081/api/compression/trigger
```

**Response:**
```json
{
  "status": "success",
  "message": "Kompresija je pokrenuta"
}
```

---

### 2. Kompresuj specifičan video thumbnail

```bash
curl -X POST http://localhost:8081/api/compression/video/123
```

**Response:**
```json
{
  "status": "success",
  "message": "Thumbnail za video ID 123 je kompresovan"
}
```

---

### 3. Statistika kompresije

```bash
curl http://localhost:8081/api/compression/stats
```

**Response:**
```json
{
  "totalVideos": 150,
  "compressedCount": 85,
  "uncompressedCount": 65,
  "eligibleForCompression": 12,
  "compressionPercentage": "56.7%"
}
```

---

### 4. Info endpoint

```bash
curl http://localhost:8081/api/compression/info
```

**Response:**
```json
{
  "description": "Periodična kompresija thumbnail slika",
  "schedule": "Svaki dan u ponoć (00:00)",
  "compressionThreshold": "30 dana",
  "compressionQuality": "70%",
  "library": "Thumbnailator 0.4.20",
  "note": "Originalna slika se zadržava..."
}
```

---

## Testiranje

### Test 1: Manuelno pokretanje

```bash
# Pokreni kompresiju ručno
curl -X POST http://localhost:8081/api/compression/trigger

# Proveri statistiku
curl http://localhost:8081/api/compression/stats
```

---

### Test 2: Kompresuj specifičan video

```bash
# Kompresuj thumbnail za video ID 1
curl -X POST http://localhost:8081/api/compression/video/1

# Proveri da li je kreirana kompresovana slika
ls -lh uploads/thumbnails/compressed/
```

---

### Test 3: Provera automatskg pokretanja

Za testiranje možeš privremeno promeniti cron da se pokrene svakog minuta:

```java
// U ImageCompressionService.java
@Scheduled(cron = "0 * * * * ?")  // Svaki minut na 0. sekund
```

**Napomena:** Ne zaboravi da vratiš na originalno!

---

### Test 4: Provera starih slika

Da bi testirao kompresiju starih slika, možeš ručno promeniti `created_at` datum u bazi:

```sql
-- Promeni datum za testiranje
UPDATE videos
SET created_at = NOW() - INTERVAL '31 days'
WHERE id = 1;

-- Pokreni kompresiju
-- curl -X POST http://localhost:8081/api/compression/trigger

-- Vrati na normalno
UPDATE videos
SET created_at = NOW()
WHERE id = 1;
```

---

## Monitoring

### Logovi

Kompresija loguje sledeće informacije:

```
🗜️  [2026-02-10T00:00:00] Pokrećem periodičnu kompresiju slika...
📊 Pronađeno 15 slika za kompresiju (starijih od 30 dana)
  ✅ Kompresovana slika za video ID: 123
    📉 Original: 2.5 MB → Compressed: 850.2 KB (ušteda: 66.0%)
  ✅ Kompresovana slika za video ID: 124
    📉 Original: 1.8 MB → Compressed: 620.5 KB (ušteda: 65.5%)
  ...
🎉 Kompresija završena! Uspešno: 15, Neuspešno: 0
```

---

### Provera rezultata

```bash
# Proveri kompresovane slike
ls -lh uploads/thumbnails/compressed/

# Uporedi veličine
ls -lh uploads/thumbnails/thumb_123.png
ls -lh uploads/thumbnails/compressed/thumb_123_compressed.jpg

# Proveri SQL statistiku
psql -U postgres -d jutjubic_db -c "
  SELECT
    COUNT(*) as total_videos,
    SUM(CASE WHEN thumbnail_compressed = true THEN 1 ELSE 0 END) as compressed,
    SUM(CASE WHEN thumbnail_compressed = false THEN 1 ELSE 0 END) as uncompressed
  FROM videos;
"
```

---

## Kompresija i kvalitet

### Parametri Thumbnailator-a

```java
Thumbnails.of(originalFile)
    .scale(1.0)                          // Zadrži originalnu rezoluciju
    .outputQuality(COMPRESSION_QUALITY)  // 70% kvalitet
    .outputFormat("jpg")                 // JPEG format (bolji za kompresiju)
    .toFile(compressedFile);
```

### Poređenje kvaliteta

| Kvalitet | Veličina fajla | Vizuelni kvalitet | Ušteda prostora |
|----------|----------------|-------------------|-----------------|
| 0.9 (90%)| ~1.5 MB        | Odličan           | ~40%            |
| 0.7 (70%)| ~850 KB        | Vrlo dobar        | ~65%            |
| 0.5 (50%)| ~500 KB        | Dobar             | ~80%            |
| 0.3 (30%)| ~300 KB        | Primetan pad      | ~88%            |

**Preporuka:** 70% je dobar balans između kvaliteta i uštede prostora.

---

## Optimizacija

### 1. Promena schedule-a

Za veliki broj videa, možda želiš da pokrećeš kompresiju noću kada je manje aktivnosti:

```java
@Scheduled(cron = "0 0 3 * * ?")  // 3 AM umesto ponoć
```

---

### 2. Batch processing

Ako imaš hiljade slika, možeš ograničiti broj kompresija po batch-u:

```java
@Scheduled(cron = "0 0 0 * * ?")
public void compressOldThumbnails() {
    List<Video> videosToCompress = videoRepository
        .findByThumbnailCompressedFalseAndCreatedAtBefore(thresholdDate)
        .stream()
        .limit(100)  // Kompresuj maksimum 100 po danu
        .collect(Collectors.toList());

    // ... rest of the code
}
```

---

### 3. Konkurentno procesiranje

Za brže izvršavanje, možeš koristiti parallel stream:

```java
videosToCompress.parallelStream().forEach(video -> {
    try {
        compressThumbnail(video);
    } catch (Exception e) {
        // error handling
    }
});
```

---

## Održavanje

### Brisanje kompresovanih slika (opciono)

Ako želiš da brišeš originalne slike nakon kompresije (NE PREPORUČUJE SE za projekat):

```java
// SAMO ZA PRODUKCIJU, NE ZA PROJEKAT
File originalFile = new File(originalPath);
if (originalFile.delete()) {
    video.setThumbnailPath(compressedPath);  // Koristi kompresovanu kao glavnu
}
```

---

### Periodično čišćenje starih kompresija

Možeš dodati novi scheduled task koji briše kompresovane verzije starije od npr. 1 godine:

```java
@Scheduled(cron = "0 0 1 1 * ?")  // Prvog dana u mesecu u 1 AM
public void cleanOldCompressedImages() {
    LocalDateTime threshold = LocalDateTime.now().minusYears(1);
    // Delete compressed images older than 1 year
}
```

---

## Troubleshooting

### Problem: Kompresija se ne pokreće

**Rešenje:**
- Proveri da li je `@EnableScheduling` anotacija na glavnoj klasi
- Proveri logove: `grep "kompresiju" logs/application.log`
- Ručno pokreni: `curl -X POST http://localhost:8081/api/compression/trigger`

---

### Problem: Greška "Image file not found"

**Rešenje:**
- Proveri putanje u bazi: `SELECT id, thumbnail_path FROM videos;`
- Proveri da li fajlovi postoje: `ls -la uploads/thumbnails/`
- Proveri permisije: `chmod 755 uploads/thumbnails/`

---

### Problem: Kompresovane slike su prevelike

**Rešenje:**
- Smanji `COMPRESSION_QUALITY` na 0.6 ili 0.5
- Opciono, smanji rezoluciju: `.size(800, 600)`

---

### Problem: OutOfMemoryError

**Rešenje:**
- Povećaj heap memory: `mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g"`
- Implementiraj batch processing (vidi Optimizacija sekciju)
- Ne koristi parallel streams za veliku količinu slika

---

## Proširenja

### 1. Notifikacije

Dodaj email notifikacije nakon završetka kompresije:

```java
@Autowired
private EmailService emailService;

public void compressOldThumbnails() {
    // ... compression logic

    emailService.sendEmail(
        "admin@example.com",
        "Kompresija završena",
        "Kompresovano: " + successCount + " slika"
    );
}
```

---

### 2. Progress tracking

Čuvaj progress u bazi za real-time monitoring:

```java
@Entity
class CompressionJob {
    private Long id;
    private LocalDateTime startTime;
    private Integer totalImages;
    private Integer processedImages;
    private String status; // RUNNING, COMPLETED, FAILED
}
```

---

### 3. Web UI

Dodaj frontend dashboard za monitoring kompresije.

---

## Zaključak

✅ Sistem je potpuno automatizovan
✅ Štedi prostor na disku bez gubitka originalnih slika
✅ Lako se testira i monitoruje
✅ Konfigurabilan i proširiv

Za pitanja ili probleme, proveri logove ili pokreni manual test.
