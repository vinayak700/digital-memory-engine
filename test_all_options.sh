#!/bin/bash
# =============================================================================
#  DIGITAL MEMORY ENGINE - COMPREHENSIVE API TEST SUITE
#  Tests all API endpoints including the Intelligent Ask Engine
#  Updated: 2026-01-31
# =============================================================================

BASE_URL="http://localhost:8082/api/v1"
AUTH="-u admin:admin123"
GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Store created IDs
MEM1_ID=""
MEM2_ID=""
MEM3_ID=""
TOPIC_ID=""
PASS_COUNT=0
FAIL_COUNT=0

# Helper function to check result
check_result() {
    local name=$1
    local condition=$2
    if [ "$condition" == "true" ]; then
        echo -e "  ${GREEN}✓${NC} $name"
        ((PASS_COUNT++))
    else
        echo -e "  ${RED}✗${NC} $name"
        ((FAIL_COUNT++))
    fi
}

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║   🧠  DIGITAL MEMORY ENGINE - API TEST SUITE                 ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Check server
echo -e "${CYAN}Checking server status...${NC}"
HEALTH=$(curl $AUTH -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/health)
if [ "$HEALTH" == "200" ]; then
    echo -e "${GREEN}✓ Server is UP${NC}"
else
    echo -e "${RED}✗ Server is DOWN (HTTP $HEALTH)${NC}"
    echo "Start server with: ./mvnw spring-boot:run"
    exit 1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}STEP 1: 🧠 CREATE MEMORIES${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Memory 1: Work
RESP1=$(curl $AUTH -s -X POST "$BASE_URL/memories" \
  -H "Content-Type: application/json" \
  -d '{"title":"Project Sprint Planning","content":"Sprint 5 goals: Complete API endpoints, add caching, implement search functionality. Deadline: Feb 15. Key technologies: Spring Boot, Redis, PostgreSQL.","importanceScore":9}')
MEM1_ID=$(echo "$RESP1" | grep -o '"id":[0-9]*' | cut -d: -f2)
if [ -n "$MEM1_ID" ]; then
    check_result "Created Memory 1: Project Sprint Planning (ID: $MEM1_ID)" "true"
else
    check_result "Created Memory 1: Project Sprint Planning" "false"
fi

# Memory 2: Learning
RESP2=$(curl $AUTH -s -X POST "$BASE_URL/memories" \
  -H "Content-Type: application/json" \
  -d '{"title":"Java Design Patterns","content":"Learned about Strategy Pattern today - useful for swappable algorithms. Also reviewed Factory Pattern for object creation. The Observer pattern is great for event handling.","importanceScore":7}')
MEM2_ID=$(echo "$RESP2" | grep -o '"id":[0-9]*' | cut -d: -f2)
if [ -n "$MEM2_ID" ]; then
    check_result "Created Memory 2: Java Design Patterns (ID: $MEM2_ID)" "true"
else
    check_result "Created Memory 2: Java Design Patterns" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}STEP 2: 📚 TOPICS & TAGGING${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Create Topic
TOPIC_RESP=$(curl $AUTH -s -X POST "${BASE_URL}/topics?name=Software+Architecture&description=High+level+design+patterns")
TOPIC_ID=$(echo "$TOPIC_RESP" | grep -o '"id":[0-9]*' | cut -d: -f2)
if [ -n "$TOPIC_ID" ]; then
    check_result "Created Topic: Software Architecture (ID: $TOPIC_ID)" "true"
else
    check_result "Created Topic" "false"
fi

# Tag Memory
TAG_CODE=$(curl $AUTH -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/topics/memory/${MEM2_ID}/tag" \
  -H "Content-Type: application/json" \
  -d '["Software Architecture", "Java"]')

if [ "$TAG_CODE" == "200" ]; then
    check_result "Tagged Memory $MEM2_ID with topics" "true"
else
    check_result "Tagging Memory (HTTP $TAG_CODE)" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}STEP 3: 🔍 SEARCH & RELATIONS${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Search
SEARCH_RESP=$(curl $AUTH -s -X POST "$BASE_URL/search" \
  -H "Content-Type: application/json" \
  -d '{"query":"sprint planning","limit":5}')
if echo "$SEARCH_RESP" | grep -q '"id"'; then
    check_result "Search executed successfully" "true"
else
    check_result "Search execution" "false"
fi

# Create Relation
REL_CODE=$(curl $AUTH -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/relationships" \
  -H "Content-Type: application/json" \
  -d "{\"sourceMemoryId\":$MEM1_ID,\"targetMemoryId\":$MEM2_ID,\"type\":\"SUPPORTS\",\"strength\":0.8}")

if [ "$REL_CODE" == "201" ]; then
    check_result "Relationship created (HTTP 201)" "true"
else
    check_result "Relationship creation (HTTP $REL_CODE)" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}STEP 4: 🤖 INTELLIGENT ASK${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

ASK_RESP=$(curl $AUTH -s -X POST "$BASE_URL/ask" \
  -H "Content-Type: application/json" \
  -d '{"question":"What do I know about Java design patterns?"}')

if echo "$ASK_RESP" | grep -q '"answer"'; then
    check_result "Ask engine responded successfully" "true"
    conf=$(echo "$ASK_RESP" | grep -o '"confidence":[0-9.]*' | cut -d: -f2)
    echo "    Confidence: $conf"
else
    check_result "Ask engine response" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}STEP 5: ⭐ FEEDBACK${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

FEEDBACK_CODE=$(curl $AUTH -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/memories/${MEM2_ID}/feedback" \
  -H "Content-Type: application/json" \
  -d '{"score":5, "summary":"Very accurate pattern description"}')

if [ "$FEEDBACK_CODE" == "200" ]; then
    check_result "Feedback submitted successfully (HTTP 200)" "true"
else
    check_result "Feedback submission (HTTP $FEEDBACK_CODE)" "false"
fi

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║   📊  FINAL TEST SUMMARY                                    ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo -e "  Passed: ${GREEN}$PASS_COUNT${NC}  |  Failed: ${RED}$FAIL_COUNT${NC}"
echo ""

if [ "$FAIL_COUNT" -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL SYSTEMS NOMINAL - READY FOR PRODUCTION${NC}"
else
    echo -e "${RED}⚠️ SYSTEM HAS FAILURES - AUDIT REQUIRED${NC}"
fi
echo ""
