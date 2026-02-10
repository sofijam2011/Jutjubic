#!/bin/bash

# 🧪 Test Script za Image Compression System
# Description: Testira kompresiju thumbnail slika

set -e

echo "🗜️  Testing Image Compression System..."
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

BASE_URL="http://localhost:8081"

# Function to print colored output
print_test() {
    echo -e "${BLUE}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

echo "============================================"
echo "1️⃣  Testing Spring Boot Application"
echo "============================================"
print_test "Checking if Spring Boot is running..."
if curl -s "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    print_success "Spring Boot is running"
else
    print_error "Spring Boot is not running"
    print_info "Start with: mvn spring-boot:run"
    exit 1
fi
echo ""

echo "============================================"
echo "2️⃣  Testing Compression Info Endpoint"
echo "============================================"
print_test "Fetching compression configuration..."
RESPONSE=$(curl -s "${BASE_URL}/api/compression/info")
echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
echo ""

echo "============================================"
echo "3️⃣  Testing Compression Statistics"
echo "============================================"
print_test "Fetching compression statistics..."
STATS=$(curl -s "${BASE_URL}/api/compression/stats")
echo "$STATS" | jq . 2>/dev/null || echo "$STATS"

TOTAL=$(echo "$STATS" | jq -r '.totalVideos // 0' 2>/dev/null || echo "0")
COMPRESSED=$(echo "$STATS" | jq -r '.compressedCount // 0' 2>/dev/null || echo "0")
UNCOMPRESSED=$(echo "$STATS" | jq -r '.uncompressedCount // 0' 2>/dev/null || echo "0")
ELIGIBLE=$(echo "$STATS" | jq -r '.eligibleForCompression // 0' 2>/dev/null || echo "0")

echo ""
print_info "Summary:"
echo "  Total videos: $TOTAL"
echo "  Compressed: $COMPRESSED"
echo "  Uncompressed: $UNCOMPRESSED"
echo "  Eligible for compression (>30 days old): $ELIGIBLE"
echo ""

echo "============================================"
echo "4️⃣  Manual Compression Trigger Test"
echo "============================================"

if [ "$ELIGIBLE" -gt 0 ]; then
    print_test "Found $ELIGIBLE images eligible for compression"
    read -p "Do you want to trigger compression now? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_test "Triggering compression..."
        TRIGGER_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/compression/trigger")
        echo "$TRIGGER_RESPONSE" | jq . 2>/dev/null || echo "$TRIGGER_RESPONSE"

        STATUS=$(echo "$TRIGGER_RESPONSE" | jq -r '.status // "unknown"' 2>/dev/null || echo "unknown")
        if [ "$STATUS" == "success" ]; then
            print_success "Compression triggered successfully!"
        else
            print_error "Compression failed"
        fi
    else
        print_info "Skipping compression trigger"
    fi
else
    print_info "No images eligible for compression (must be >30 days old)"
    print_info "To test, you can:"
    echo "  1. Upload a video and wait 30 days (not practical)"
    echo "  2. Manually update created_at in database:"
    echo "     UPDATE videos SET created_at = NOW() - INTERVAL '31 days' WHERE id = 1;"
    echo "  3. Compress a specific video by ID (ignoring age threshold):"
    echo "     curl -X POST ${BASE_URL}/api/compression/video/1"
fi
echo ""

echo "============================================"
echo "5️⃣  Testing Specific Video Compression"
echo "============================================"
print_test "Testing compression of specific video..."

if [ "$TOTAL" -gt 0 ]; then
    read -p "Enter video ID to compress (or press Enter to skip): " VIDEO_ID
    if [ -n "$VIDEO_ID" ]; then
        print_test "Compressing video ID: $VIDEO_ID"
        VIDEO_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/compression/video/${VIDEO_ID}")
        echo "$VIDEO_RESPONSE" | jq . 2>/dev/null || echo "$VIDEO_RESPONSE"

        VIDEO_STATUS=$(echo "$VIDEO_RESPONSE" | jq -r '.status // "unknown"' 2>/dev/null || echo "unknown")
        if [ "$VIDEO_STATUS" == "success" ]; then
            print_success "Video thumbnail compressed successfully!"
        else
            print_error "Video compression failed"
            echo "$VIDEO_RESPONSE" | jq -r '.message // ""' 2>/dev/null
        fi
    else
        print_info "Skipping specific video compression"
    fi
else
    print_info "No videos in database"
fi
echo ""

echo "============================================"
echo "6️⃣  Checking Compressed Files Directory"
echo "============================================"
print_test "Checking for compressed images..."

COMPRESSED_DIR="uploads/thumbnails/compressed"
if [ -d "$COMPRESSED_DIR" ]; then
    COMPRESSED_COUNT=$(find "$COMPRESSED_DIR" -type f -name "*.jpg" 2>/dev/null | wc -l | tr -d ' ')
    if [ "$COMPRESSED_COUNT" -gt 0 ]; then
        print_success "Found $COMPRESSED_COUNT compressed images in $COMPRESSED_DIR"
        echo ""
        print_info "Listing compressed images:"
        ls -lh "$COMPRESSED_DIR" | tail -5
    else
        print_info "Compressed directory exists but is empty"
    fi
else
    print_info "Compressed directory does not exist yet (will be created on first compression)"
    mkdir -p "$COMPRESSED_DIR" && print_success "Created directory: $COMPRESSED_DIR"
fi
echo ""

echo "============================================"
echo "7️⃣  Size Comparison Test"
echo "============================================"
print_test "Comparing original vs compressed sizes..."

if [ -d "uploads/thumbnails" ]; then
    ORIGINALS=$(find uploads/thumbnails -maxdepth 1 -type f \( -name "*.jpg" -o -name "*.png" \) 2>/dev/null)

    if [ -n "$ORIGINALS" ]; then
        print_info "Sample comparison (first 3 images):"
        echo ""
        COUNT=0
        for ORIGINAL in $ORIGINALS; do
            if [ $COUNT -ge 3 ]; then break; fi
            FILENAME=$(basename "$ORIGINAL")
            NAMEONLY="${FILENAME%.*}"
            COMPRESSED="uploads/thumbnails/compressed/${NAMEONLY}_compressed.jpg"

            if [ -f "$COMPRESSED" ]; then
                ORIG_SIZE=$(stat -f%z "$ORIGINAL" 2>/dev/null || stat -c%s "$ORIGINAL" 2>/dev/null || echo "0")
                COMP_SIZE=$(stat -f%z "$COMPRESSED" 2>/dev/null || stat -c%s "$COMPRESSED" 2>/dev/null || echo "0")

                if [ "$ORIG_SIZE" -gt 0 ] && [ "$COMP_SIZE" -gt 0 ]; then
                    ORIG_MB=$(echo "scale=2; $ORIG_SIZE/1024/1024" | bc 2>/dev/null || echo "0")
                    COMP_MB=$(echo "scale=2; $COMP_SIZE/1024/1024" | bc 2>/dev/null || echo "0")
                    SAVINGS=$(echo "scale=1; 100*(1-$COMP_SIZE/$ORIG_SIZE)" | bc 2>/dev/null || echo "0")

                    echo "  📄 $FILENAME"
                    echo "     Original:    ${ORIG_MB} MB"
                    echo "     Compressed:  ${COMP_MB} MB"
                    echo "     Savings:     ${SAVINGS}%"
                    echo ""
                fi
            fi
            COUNT=$((COUNT + 1))
        done
    else
        print_info "No original thumbnails found"
    fi
else
    print_error "Thumbnails directory does not exist"
fi

echo "============================================"
echo "📊 Test Summary"
echo "============================================"
echo -e "${GREEN}✅ All tests completed!${NC}"
echo ""
echo "Next steps:"
echo "  1. Upload videos to create thumbnails"
echo "  2. Wait 30 days OR manually update created_at in database"
echo "  3. Run compression: curl -X POST ${BASE_URL}/api/compression/trigger"
echo "  4. Monitor logs: tail -f logs/spring.log | grep kompresiju"
echo "  5. Check compressed directory: ls -lh uploads/thumbnails/compressed/"
echo ""
echo "For scheduled execution, the task will run automatically every day at midnight (00:00)"
echo ""
