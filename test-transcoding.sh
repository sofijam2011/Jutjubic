#!/bin/bash


set -e

echo "🚀 Starting Video Transcoding System Tests..."
echo ""

GREEN='\33[0;32m'
RED='\33[0;31m'
YELLOW='\33[1;33m'
NC='\33[0m'

TESTS_PASSED=0
TESTS_FAILED=0

print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ PASS${NC}: $2"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ FAIL${NC}: $2"
        ((TESTS_FAILED++))
    fi
}

echo "============================================"
echo "1️⃣  Testing FFmpeg Installation"
echo "============================================"
if command -v ffmpeg &> /dev/null; then
    FFMPEG_VERSION=$(ffmpeg -version 2>&1 | head -1)
    print_result 0 "FFmpeg installed: $FFMPEG_VERSION"
else
    print_result 1 "FFmpeg not installed. Run: brew install ffmpeg"
fi
echo ""

echo "============================================"
echo "2️⃣  Testing Docker Installation"
echo "============================================"
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    print_result 0 "Docker installed: $DOCKER_VERSION"
else
    print_result 1 "Docker not installed"
fi
echo ""

echo "============================================"
echo "3️⃣  Testing RabbitMQ Container"
echo "============================================"
if docker ps | grep -q jutjubic-rabbitmq; then
    print_result 0 "RabbitMQ container is running"

    if curl -s -u guest:guest http://localhost:15672/api/overview > /dev/null 2>&1; then
        print_result 0 "RabbitMQ Management API is accessible"
    else
        print_result 1 "RabbitMQ Management API is not accessible"
    fi
else
    print_result 1 "RabbitMQ container is not running"
    echo -e "${YELLOW}💡 Tip: Start RabbitMQ with:${NC}"
    echo "   docker run -d --name jutjubic-rabbitmq -p 5672:5672 -p 15672:15672 \\"
    echo "     -e RABBITMQ_DEFAULT_USER=guest -e RABBITMQ_DEFAULT_PASS=guest \\"
    echo "     rabbitmq:3-management"
fi
echo ""

echo "============================================"
echo "4️⃣  Testing Spring Boot Application"
echo "============================================"
if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    print_result 0 "Spring Boot application is running"

    HEALTH_RESPONSE=$(curl -s http://localhost:8081/api/transcoding/health)

    if echo "$HEALTH_RESPONSE" | grep -q '"status":"HEALTHY"'; then
        print_result 0 "Transcoding system is HEALTHY"
    else
        print_result 1 "Transcoding system is not healthy"
        echo -e "${YELLOW}Response: $HEALTH_RESPONSE${NC}"
    fi

    if echo "$HEALTH_RESPONSE" | grep -q '"installed":true'; then
        print_result 0 "Application detected FFmpeg"
    else
        print_result 1 "Application cannot detect FFmpeg"
    fi

    if echo "$HEALTH_RESPONSE" | grep -q '"connected":true'; then
        print_result 0 "Application connected to RabbitMQ"
    else
        print_result 1 "Application not connected to RabbitMQ"
    fi
else
    print_result 1 "Spring Boot application is not running"
    echo -e "${YELLOW}💡 Tip: Start the application with: mvn spring-boot:run${NC}"
fi
echo ""

echo "============================================"
echo "5️⃣  Testing RabbitMQ Queues"
echo "============================================"
if docker ps | grep -q jutjubic-rabbitmq; then
    QUEUE_INFO=$(curl -s -u guest:guest http://localhost:15672/api/queues/%2F/video.transcoding.queue 2>/dev/null)

    if echo "$QUEUE_INFO" | grep -q "video.transcoding.queue"; then
        print_result 0 "Queue 'video.transcoding.queue' exists"

        CONSUMER_COUNT=$(echo "$QUEUE_INFO" | grep -o '"consumers":[0-9]*' | grep -o '[0-9]*')
        if [ "$CONSUMER_COUNT" -ge 2 ]; then
            print_result 0 "At least 2 consumers connected (found: $CONSUMER_COUNT)"
        else
            print_result 1 "Less than 2 consumers connected (found: $CONSUMER_COUNT)"
        fi
    else
        print_result 1 "Queue 'video.transcoding.queue' does not exist"
    fi

    DLQ_INFO=$(curl -s -u guest:guest http://localhost:15672/api/queues/%2F/video.transcoding.dlq 2>/dev/null)
    if echo "$DLQ_INFO" | grep -q "video.transcoding.dlq"; then
        print_result 0 "Dead Letter Queue exists"
    else
        print_result 1 "Dead Letter Queue does not exist"
    fi
else
    echo -e "${YELLOW}⏭️  Skipping queue tests (RabbitMQ not running)${NC}"
fi
echo ""

echo "============================================"
echo "6️⃣  Testing File Directories"
echo "============================================"
if [ -d "uploads/videos" ]; then
    print_result 0 "Uploads directory exists"
else
    print_result 1 "Uploads directory does not exist"
fi

if [ -d "uploads/videos/transcoded" ]; then
    print_result 0 "Transcoded directory exists"
else
    mkdir -p uploads/videos/transcoded 2>/dev/null && \
        print_result 0 "Created transcoded directory" || \
        print_result 1 "Cannot create transcoded directory"
fi
echo ""

echo "============================================"
echo "📊 Test Summary"
echo "============================================"
TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
echo -e "Total Tests: ${TOTAL_TESTS}"
echo -e "${GREEN}Passed: ${TESTS_PASSED}${NC}"
echo -e "${RED}Failed: ${TESTS_FAILED}${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 All tests passed! System is ready.${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Upload a video through the frontend or API"
    echo "  2. Monitor logs: tail -f logs/spring.log | grep transcoding"
    echo "  3. Check RabbitMQ UI: http://localhost:15672"
    echo "  4. Verify transcoded files: ls -lh uploads/videos/transcoded/"
    exit 0
else
    echo -e "${RED}⚠️  Some tests failed. Please fix the issues above.${NC}"
    echo ""
    echo "Common fixes:"
    echo "  - Install FFmpeg: brew install ffmpeg"
    echo "  - Start RabbitMQ: docker run -d --name jutjubic-rabbitmq ..."
    echo "  - Start Spring Boot: mvn spring-boot:run"
    echo ""
    echo "For detailed instructions, see TEST_GUIDE.md"
    exit 1
fi
