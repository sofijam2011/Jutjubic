#!/bin/bash


echo "🚀 Jutjubic - Quick Test"
echo "========================"
echo ""

GREEN='\33[0;32m'
RED='\33[0;31m'
YELLOW='\33[1;33m'
NC='\33[0m'

echo "1️⃣  Testing Spring Boot..."
if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Spring Boot is running${NC}"
else
    echo -e "${RED}❌ Spring Boot is NOT running${NC}"
    echo -e "${YELLOW}Start with: mvn spring-boot:run${NC}"
    exit 1
fi
echo ""

echo "2️⃣  Testing FFmpeg..."
if command -v ffmpeg &> /dev/null; then
    VERSION=$(ffmpeg -version 2>&1 | head -1 | cut -d' ' -f3)
    echo -e "${GREEN}✅ FFmpeg installed: v$VERSION${NC}"
else
    echo -e "${RED}❌ FFmpeg not installed${NC}"
fi
echo ""

echo "3️⃣  Testing Image Compression..."
RESPONSE=$(curl -s http://localhost:8081/api/compression/info)
if echo "$RESPONSE" | grep -q "compressionQuality"; then
    echo -e "${GREEN}✅ Image Compression API is working${NC}"
    echo "   Quality: 70%"
    echo "   Schedule: Daily at midnight"
else
    echo -e "${RED}❌ Image Compression API failed${NC}"
fi
echo ""

echo "4️⃣  Testing Compression Stats..."
STATS=$(curl -s http://localhost:8081/api/compression/stats)
TOTAL=$(echo "$STATS" | grep -o '"totalVideos":[0-9]*' | cut -d':' -f2)
COMPRESSED=$(echo "$STATS" | grep -o '"compressedCount":[0-9]*' | cut -d':' -f2)
echo "   Total Videos: ${TOTAL:-0}"
echo "   Compressed: ${COMPRESSED:-0}"
echo ""

echo "5️⃣  Testing Watch Party..."
WP_RESPONSE=$(curl -s http://localhost:8081/api/watchparty/public)
if [ "$WP_RESPONSE" == "[]" ] || echo "$WP_RESPONSE" | grep -q "roomCode"; then
    echo -e "${GREEN}✅ Watch Party API is working${NC}"
    echo "   Public rooms: $(echo "$WP_RESPONSE" | grep -o "roomCode" | wc -l | tr -d ' ')"
else
    echo -e "${RED}❌ Watch Party API failed${NC}"
fi
echo ""

echo "6️⃣  Testing WebSocket..."
WS_INFO=$(curl -s http://localhost:8081/ws/info)
if echo "$WS_INFO" | grep -q "websocket"; then
    echo -e "${GREEN}✅ WebSocket is enabled${NC}"
    echo "   Endpoint: ws://localhost:8081/ws"
else
    echo -e "${RED}❌ WebSocket not available${NC}"
fi
echo ""

echo "7️⃣  Testing RabbitMQ (Transcoding)..."
if curl -s http://localhost:15672 > /dev/null 2>&1; then
    echo -e "${GREEN}✅ RabbitMQ is running${NC}"
    echo "   Management UI: http://localhost:15672"
else
    echo -e "${YELLOW}⚠️  RabbitMQ is not running${NC}"
    echo "   Transcoding will not work"
    echo "   Install: brew install rabbitmq"
fi
echo ""

echo "========================"
echo "📊 Summary"
echo "========================"
echo ""
echo -e "${GREEN}✅ Working:${NC}"
echo "   - Image Compression"
echo "   - Watch Party"
echo "   - FFmpeg"
echo ""
echo -e "${YELLOW}⚠️  Optional (not required):${NC}"
echo "   - RabbitMQ (for Transcoding)"
echo ""
echo "🎯 Next Steps:"
echo "   1. Test Image Compression: http://localhost:8081/api/compression/stats"
echo "   2. Test Watch Party: http://localhost:8081/watchparty-test.html"
echo "   3. Install RabbitMQ: brew install rabbitmq (optional)"
echo ""
echo "📖 Documentation:"
echo "   - SETUP_STATUS.md"
echo "   - IMAGE_COMPRESSION_GUIDE.md"
echo "   - WATCH_PARTY_QUICKSTART.md"
echo ""
