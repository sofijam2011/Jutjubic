## 🎬 Watch Party - Zajedničko gledanje videa

## Pregled

Watch Party omogućava korisnicima da kreирају sobe i zajednički gledaju videe u realnom vremenu. Kada kreator sobe pokrene video, automatski se otvara stranica sa istim videom kod svih članova sobe.

## Karakteristike

✅ **Kreiranje soba** - Svaki korisnik može kreirati svoju sobu
✅ **Javne i privatne sobe** - Kontrola vidljivosti
✅ **Room code** - Jedinstveni 8-karakterni kod za pristup
✅ **Real-time sync** - WebSocket za instant broadcast
✅ **Broj članova** - Praćenje ko je u sobi
✅ **Kreator kontrole** - Samo kreator može pustiti video

---

## Arhitektura

### Backend Stack

- **Spring WebSocket** - Real-time komunikacija
- **STOMP protokol** - Messaging protocol preko WebSocket-a
- **SockJS** - Fallback za browsere bez WebSocket podrške
- **PostgreSQL** - Čuvanje soba i članova

### Struktura

```
┌─────────────────┐
│   Korisnik 1    │
│   (Kreator)     │
└────────┬────────┘
         │ 1. Kreira sobu
         ▼
┌─────────────────────────┐
│   WatchPartyService     │
│   + PostgreSQL          │
└────────┬────────────────┘
         │ 2. Generiše room code
         ▼
┌─────────────────────────┐
│   Room Code: ABCD1234   │
└────────┬────────────────┘
         │ 3. Share link/code
         ▼
┌─────────────────┐
│   Korisnik 2    │
│   (Član)        │
└────────┬────────┘
         │ 4. Join room
         ▼
┌─────────────────────────┐
│   WebSocket Connection  │
│   /topic/watchparty/    │
│   ABCD1234              │
└────────┬────────────────┘
         │
         ├──────────────────┐
         ▼                  ▼
   Korisnik 1         Korisnik 2
   Pokrene video      Prima poruku
   Broadcast          Otvara isti video
```

---

## REST API Endpoints

### 1. Kreiranje sobe

```http
POST /api/watchparty/create
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer <JWT_TOKEN>

name=Moja Soba&isPublic=true
```

**Response:**
```json
{
  "success": true,
  "room": {
    "id": 1,
    "roomCode": "ABCD1234",
    "name": "Moja Soba",
    "creatorUsername": "user1",
    "creatorId": 1,
    "isPublic": true,
    "isActive": true,
    "memberCount": 1,
    "members": [
      {
        "userId": 1,
        "username": "user1",
        "isOnline": true
      }
    ]
  },
  "message": "Soba kreirana: ABCD1234"
}
```

---

### 2. Lista javnih soba

```http
GET /api/watchparty/public
```

**Response:**
```json
[
  {
    "id": 1,
    "roomCode": "ABCD1234",
    "name": "Moja Soba",
    "creatorUsername": "user1",
    "memberCount": 3,
    "isActive": true
  }
]
```

---

### 3. Informacije o sobi

```http
GET /api/watchparty/room/ABCD1234
```

**Response:**
```json
{
  "id": 1,
  "roomCode": "ABCD1234",
  "name": "Moja Soba",
  "creatorUsername": "user1",
  "memberCount": 3,
  "currentVideoId": 123,
  "currentVideoTitle": "Test Video",
  "members": [
    {
      "userId": 1,
      "username": "user1",
      "isOnline": true
    },
    {
      "userId": 2,
      "username": "user2",
      "isOnline": true
    }
  ]
}
```

---

### 4. Pridruživanje sobi

```http
POST /api/watchparty/join/ABCD1234
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "room": { /* room details */ },
  "message": "Pridružio si se sobi: Moja Soba"
}
```

---

### 5. Pokretanje videa (samo kreator)

```http
POST /api/watchparty/room/ABCD1234/play?videoId=123
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Video pokrenut za sve članove sobe"
}
```

**Ova akcija šalje WebSocket poruku svim članovima!**

---

### 6. Napuštanje sobe

```http
POST /api/watchparty/leave/ABCD1234
Authorization: Bearer <JWT_TOKEN>
```

---

### 7. Zatvaranje sobe (samo kreator)

```http
POST /api/watchparty/room/ABCD1234/close
Authorization: Bearer <JWT_TOKEN>
```

---

## WebSocket Komunikacija

### Povezivanje na WebSocket

**Endpoint:** `ws://localhost:8081/ws`

**Sa SockJS fallback:** `http://localhost:8081/ws`

### STOMP Konfiguracija

```javascript
// Primer sa JavaScript (SockJS + STOMP)
const socket = new SockJS('http://localhost:8081/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  console.log('Connected: ' + frame);

  // Subscribe na sobu
  stompClient.subscribe('/topic/watchparty/ABCD1234', function(message) {
    const videoChange = JSON.parse(message.body);
    console.log('Primljena poruka:', videoChange);

    // Otvori video
    if (videoChange.action === 'PLAY') {
      window.location.href = `/video/${videoChange.videoId}`;
    }
  });
});
```

---

### Struktura WebSocket poruke

```json
{
  "roomCode": "ABCD1234",
  "videoId": 123,
  "videoTitle": "Test Video",
  "creatorUsername": "user1",
  "action": "PLAY"
}
```

**Akcije:**
- `PLAY` - Pokreni video
- `STOP` - Soba zatvorena
- `CHANGE` - Promeni video

---

## Frontend Integracija

### 1. Kreiranje sobe

```javascript
async function createWatchParty(name, isPublic) {
  const response = await fetch('/api/watchparty/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': 'Bearer ' + getJwtToken()
    },
    body: `name=${encodeURIComponent(name)}&isPublic=${isPublic}`
  });

  const data = await response.json();
  console.log('Room code:', data.room.roomCode);
  return data.room;
}
```

---

### 2. Povezivanje na WebSocket

```javascript
function connectToWatchParty(roomCode) {
  const socket = new SockJS('/ws');
  const stompClient = Stomp.over(socket);

  stompClient.connect({}, function(frame) {
    // Subscribe na topic za sobu
    stompClient.subscribe(`/topic/watchparty/${roomCode}`, function(message) {
      const videoChange = JSON.parse(message.body);

      if (videoChange.action === 'PLAY') {
        // Otvori stranicu videa
        window.location.href = `/video.html?id=${videoChange.videoId}`;
        // ILI ako koristiš React Router
        // navigate(`/video/${videoChange.videoId}`);
      } else if (videoChange.action === 'STOP') {
        alert('Soba je zatvorena');
        window.location.href = '/';
      }
    });
  });

  return stompClient;
}
```

---

### 3. Pokretanje videa (kreator)

```javascript
async function playVideoInRoom(roomCode, videoId) {
  const response = await fetch(`/api/watchparty/room/${roomCode}/play?videoId=${videoId}`, {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + getJwtToken()
    }
  });

  const data = await response.json();
  console.log(data.message); // "Video pokrenut za sve članove sobe"
}
```

---

## Testiranje na 2 računara

### Setup

**Računar 1 (Server + Kreator):**
- IP adresa: npr. `192.168.1.100`
- Port: `8081`

**Računar 2 (Član):**
- Pristupa serveru preko IP-a računara 1

---

### Korak po korak testiranje

#### **Računar 1 (Server):**

1. **Pokreni aplikaciju**
   ```bash
   mvn spring-boot:run
   ```

2. **Proveri IP adresu**
   ```bash
   ipconfig getifaddr en0  # macOS
   # ili
   ifconfig  # Linux
   # ili
   ipconfig  # Windows
   ```
   Primer: `192.168.1.100`

3. **Proveri da firewall dozvoljava port 8081**
   ```bash
   # macOS
   sudo pfctl -d  # privremeno disable

   # Linux
   sudo ufw allow 8081

   # Windows
   # Dodaj rule u Windows Firewall za port 8081
   ```

---

#### **Računar 2 (Klijent):**

1. **Proveri konekciju**
   ```bash
   ping 192.168.1.100
   curl http://192.168.1.100:8081/actuator/health
   ```

2. **Otvori browser**
   - URL: `http://192.168.1.100:8081`

---

### Test Scenario

#### **Računar 1:**

1. Login kao `user1`
2. Kreiraj Watch Party sobu:
   ```bash
   curl -X POST "http://localhost:8081/api/watchparty/create" \
     -H "Authorization: Bearer YOUR_JWT" \
     -d "name=Test Room&isPublic=true"
   ```
   Rezultat: `{"roomCode": "ABCD1234"}`

3. Podeli room code sa računarom 2: **ABCD1234**

---

#### **Računar 2:**

1. Login kao `user2` (ili bilo koji drugi korisnik)
2. Join sobu:
   ```bash
   curl -X POST "http://192.168.1.100:8081/api/watchparty/join/ABCD1234" \
     -H "Authorization: Bearer YOUR_JWT"
   ```

3. Konektuj se na WebSocket:
   ```javascript
   const socket = new SockJS('http://192.168.1.100:8081/ws');
   const stompClient = Stomp.over(socket);

   stompClient.connect({}, function() {
     stompClient.subscribe('/topic/watchparty/ABCD1234', function(msg) {
       const data = JSON.parse(msg.body);
       console.log('Video ID:', data.videoId);
       // Otvori video automatski
       window.location.href = `http://192.168.1.100:8081/video.html?id=${data.videoId}`;
     });
   });
   ```

---

#### **Računar 1 (Kreator):**

4. Pokreni video ID 123:
   ```bash
   curl -X POST "http://localhost:8081/api/watchparty/room/ABCD1234/play?videoId=123" \
     -H "Authorization: Bearer YOUR_JWT"
   ```

---

#### **Računar 2:**

5. **Automatski se otvara video stranica!** 🎉

---

## Baza podataka

### Tabele

```sql
-- Watch Party sobe
CREATE TABLE watch_parties (
    id BIGSERIAL PRIMARY KEY,
    room_code VARCHAR(8) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    creator_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_public BOOLEAN DEFAULT TRUE,
    current_video_id BIGINT REFERENCES videos(id),
    is_active BOOLEAN DEFAULT TRUE
);

-- Članovi soba
CREATE TABLE watch_party_members (
    id BIGSERIAL PRIMARY KEY,
    watch_party_id BIGINT NOT NULL REFERENCES watch_parties(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_online BOOLEAN DEFAULT TRUE,
    last_seen TIMESTAMP DEFAULT NOW(),
    UNIQUE(watch_party_id, user_id)
);
```

---

## Testiranje sa curl

### Kompletan test flow:

```bash
# 1. Login kao user1 (kreator)
TOKEN1=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user1@example.com","password":"password"}' \
  | jq -r '.token')

# 2. Kreiraj sobu
ROOM=$(curl -s -X POST "http://localhost:8081/api/watchparty/create" \
  -H "Authorization: Bearer $TOKEN1" \
  -d "name=Test Room&isPublic=true" \
  | jq -r '.room.roomCode')

echo "Room code: $ROOM"

# 3. Login kao user2 (član)
TOKEN2=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user2@example.com","password":"password"}' \
  | jq -r '.token')

# 4. User2 join
curl -X POST "http://localhost:8081/api/watchparty/join/$ROOM" \
  -H "Authorization: Bearer $TOKEN2"

# 5. Proveri članove
curl -s "http://localhost:8081/api/watchparty/room/$ROOM" | jq '.members'

# 6. User1 (kreator) pokrene video
curl -X POST "http://localhost:8081/api/watchparty/room/$ROOM/play?videoId=1" \
  -H "Authorization: Bearer $TOKEN1"

# User2 prima WebSocket poruku i automatski otvara video!
```

---

## Troubleshooting

### WebSocket ne povezuje

**Problem:** `Failed to connect to WebSocket`

**Rešenje:**
```bash
# Proveri da li WebSocket endpoint radi
curl http://localhost:8081/ws/info

# Proveri CORS settings
# U WebSocketConfig.java:
.setAllowedOriginPatterns("*")
```

---

### Poruke ne stižu

**Problem:** Subscribe ne prima poruke

**Rešenje:**
- Proveri topic name: `/topic/watchparty/{roomCode}`
- Proveri da li je korisnik autentifikovan
- Proveri browser console za greške

---

### "Samo kreator može pustiti video"

**Problem:** Član pokušava da pusti video

**Rešenje:**
- Samo kreator sobe može koristiti `/play` endpoint
- Članovi samo primaju WebSocket poruke

---

## Performance & Skalabilnost

### Ograničenja

- **Max članovi po sobi:** Neograničeno (ali preporučeno 10-20)
- **Max sobe:** Neograničeno
- **WebSocket connections:** Zavisi od servera (default: 8192)

### Optimizacija

Za veliku količinu korisnika, razmotriti:
- Redis za session storage
- RabbitMQ ili Kafka umesto simple broker
- Load balancing sa sticky sessions

---

## Security Considerations

✅ **JWT autentifikacija** - Samo autentifikovani korisnici
✅ **Kreator validation** - Samo kreator kontroliše sobu
✅ **Room code** - Jedinstveni kodovi sprečavaju nesrećan pristup
✅ **CORS konfiguracija** - Kontrola pristupa

---

## Proširenja

Moguća poboljšanja:
1. **Chat** - Dodaj chat funkcionalnost u sobi
2. **Emoji reactions** - Real-time emoji reakcije
3. **Kick/Ban** - Kreator može izbaciti članove
4. **Scheduled rooms** - Zakaži sobu za određeno vreme
5. **Room history** - Čuvaj istoriju pustenih videa

---

## Zaključak

Watch Party sistem je potpuno funkcionalan i spreman za testiranje na više računara. Korisnici mogu kreirati sobe, pridružiti se sobama, i automatski otvarati videe kada kreator pokrene video.

**🎬 Enjoy watching together!**
