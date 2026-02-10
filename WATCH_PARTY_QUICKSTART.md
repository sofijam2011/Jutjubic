# 🎬 Watch Party - Quick Start Guide

## 🚀 Brzo pokretanje

### 1. Pokreni aplikaciju

```bash
mvn spring-boot:run
```

### 2. Otvori test stranicu

**Browser:** http://localhost:8081/watchparty-test.html

---

## 📋 Test scenario na 2 računara

### **RAČUNAR 1 (Server + Kreator)**

#### Korak 1: Pokreni server
```bash
cd /Users/paun/IdeaProjects/Jutjubic
mvn spring-boot:run
```

#### Korak 2: Proveri IP adresu
```bash
# macOS/Linux
ifconfig | grep "inet "
# Primer: 192.168.1.100
```

#### Korak 3: Otvori test stranicu
```
http://localhost:8081/watchparty-test.html
```

#### Korak 4: Login
- Email: `test@example.com` (ili bilo koji postojeći korisnik)
- Password: `password`
- Klikni **Login**

#### Korak 5: Kreiraj sobu
- Room Name: `Test Room`
- Čekiraj **Public**
- Klikni **Create Room**
- **Zapamti Room Code** (npr. `ABCD1234`)

#### Korak 6: Konektuj WebSocket
- Room code će automatski biti popunjen
- Klikni **Connect**
- Trebao bi da vidiš: `✅ Connected to WebSocket`

---

### **RAČUNAR 2 (Član)**

#### Korak 1: Proveri konekciju
```bash
ping 192.168.1.100
curl http://192.168.1.100:8081/actuator/health
```

#### Korak 2: Otvori test stranicu
```
http://192.168.1.100:8081/watchparty-test.html
```

#### Korak 3: Login
- Koristi **DRUGOG korisnika** (ne istog!)
- Email: `user2@example.com`
- Password: `password`
- Klikni **Login**

#### Korak 4: Join Room
- Unesi Room Code od Računara 1: `ABCD1234`
- Klikni **Join Room**
- Trebao bi da vidiš: `✅ Joined room: ABCD1234`

#### Korak 5: Konektuj WebSocket
- Room code će automatski biti popunjen
- Klikni **Connect**
- Čekaj poruku...

---

### **RAČUNAR 1 (Kreator pokreće video)**

#### Korak 7: Play Video
- Video ID: `1` (ili bilo koji postojeći video ID)
- Klikni **Play Video**

---

### **RAČUNAR 2 (Prima poruku)**

#### Rezultat: 🎉
- **Automatski se pojavi alert** sa informacijama o videu!
- U logu vidiš: `🎬 Opening video: 1 - Video Title`
- U realnoj aplikaciji, browser bi automatski otvorio stranicu videa!

---

## 🧪 Test sa curl (Alternativa)

### Računar 1:

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}' \
  | jq -r '.token')

# Kreiraj sobu
ROOM=$(curl -s -X POST "http://localhost:8081/api/watchparty/create" \
  -H "Authorization: Bearer $TOKEN" \
  -d "name=Test&isPublic=true" \
  | jq -r '.room.roomCode')

echo "Room code: $ROOM"
```

### Računar 2:

```bash
# Login kao drugi korisnik
TOKEN2=$(curl -s -X POST http://192.168.1.100:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user2@example.com","password":"password"}' \
  | jq -r '.token')

# Join room
curl -X POST "http://192.168.1.100:8081/api/watchparty/join/ABCD1234" \
  -H "Authorization: Bearer $TOKEN2"

# Konektuj WebSocket preko test stranice!
```

### Računar 1:

```bash
# Play video
curl -X POST "http://localhost:8081/api/watchparty/room/$ROOM/play?videoId=1" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📡 WebSocket Flow

```
Računar 1 (Kreator)
   │
   │ POST /api/watchparty/room/ABCD1234/play?videoId=1
   ▼
Spring Boot Server
   │
   │ SimpMessagingTemplate.convertAndSend()
   ▼
WebSocket Topic: /topic/watchparty/ABCD1234
   │
   ├──────────────┬──────────────┐
   ▼              ▼              ▼
Računar 1    Računar 2    Računar 3
(Kreator)     (Član)       (Član)
   │              │              │
   ▼              ▼              ▼
Otvara video  Otvara video  Otvara video
```

---

## 🐛 Troubleshooting

### Problem: "Connection refused"

**Rešenje:**
```bash
# Proveri da li server radi
curl http://localhost:8081/actuator/health

# Proveri firewall
sudo pfctl -d  # macOS
sudo ufw allow 8081  # Linux
```

---

### Problem: "WebSocket connection failed"

**Rešenje:**
- Proveri da li je `/ws` endpoint dostupan
- Proveri browser console za greške
- Pokušaj sa `http://` umesto `https://`

---

### Problem: "Login failed"

**Rešenje:**
- Proveri da li korisnik postoji u bazi
- Kreiraj novog korisnika preko `/api/auth/register`
- Proveri email i password

---

### Problem: Računar 2 ne prima poruku

**Rešenje:**
1. Proveri da li je WebSocket connected (zeleni status)
2. Proveri da li je room code ispravan
3. Proveri browser console (F12) za greške
4. Proveri da li Računar 1 i 2 koriste ISTI room code

---

## 📊 Provera da li radi

### Checkpoints:

- [ ] Server radi na Računaru 1
- [ ] Računar 2 može pristupiti serveru (ping uspešan)
- [ ] Login radi na oba računara
- [ ] Room je kreiran na Računaru 1
- [ ] Room code je podeljen sa Računarom 2
- [ ] Računar 2 joined room
- [ ] WebSocket connected na oba računara (zeleni status)
- [ ] Play video na Računaru 1
- [ ] **Alert se pojavio na Računaru 2!** ✅

---

## 🎯 Očekivani rezultati

Kada Računar 1 pokrene video:

**Računar 1 (Log):**
```
[14:30:15] ✅ Video broadcast to all members
```

**Računar 2 (Log):**
```
[14:30:15] 📡 Received message: {"roomCode":"ABCD1234","videoId":1,"videoTitle":"Test Video","creatorUsername":"user1","action":"PLAY"}
[14:30:15] 🎬 Opening video: 1 - Test Video
```

**Računar 2 (Alert):**
```
Video playing: Test Video (ID: 1)

In real app, this would open: /video/1
```

---

## 🔄 Kako bi radilo u realnoj aplikaciji

U `watchparty-test.html`, ova linija bi stvarno otvorila video:

```javascript
// Trenutno je zakomentarisano za testiranje
// window.location.href = `/video.html?id=${data.videoId}`;

// U produkciji bi bilo aktivno:
window.location.href = `/video.html?id=${data.videoId}`;
// ILI sa React Router:
navigate(`/video/${data.videoId}`);
```

---

## 📝 API Endpoints Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/watchparty/create` | POST | Kreiraj sobu |
| `/api/watchparty/public` | GET | Lista javnih soba |
| `/api/watchparty/room/{code}` | GET | Info o sobi |
| `/api/watchparty/join/{code}` | POST | Pridruži se sobi |
| `/api/watchparty/leave/{code}` | POST | Napusti sobu |
| `/api/watchparty/room/{code}/play` | POST | Pokreni video (kreator) |
| `/api/watchparty/room/{code}/close` | POST | Zatvori sobu (kreator) |
| `/ws` | WebSocket | WebSocket endpoint |
| `/topic/watchparty/{code}` | Subscribe | Topic za sobu |

---

## 🎉 Gotovo!

Nakon što uspešno testiraš na 2 računara, možeš integrisati Watch Party u svoj frontend!

Za više detalja, pogledaj: **WATCH_PARTY_GUIDE.md**
