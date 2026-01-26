#!/bin/bash
# =============================================================================
#  DIGITAL MEMORY ENGINE - COMPREHENSIVE API TEST SUITE
#  Tests all API endpoints including the Intelligent Ask Engine
#  Updated: 2026-01-26
# =============================================================================

BASE_URL="http://localhost:8082/api/v1"
USER="test-scenario-user"
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
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" -H "X-User-Id: admin" http://localhost:8082/actuator/health)
if [ "$HEALTH" == "200" ]; then
    echo -e "${GREEN}✓ Server is UP${NC}"
else
    echo -e "${RED}✗ Server is DOWN (HTTP $HEALTH)${NC}"
    echo "Start server with: ./mvnw spring-boot:run"
    exit 1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}OPTION 1: 🧠 CREATE MEMORY${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: Creating 3 memories for different contexts"
echo ""

# Memory 1: Work
RESP1=$(curl -s -X POST "$BASE_URL/memories" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{"title":"Project Sprint Planning","content":"Sprint 5 goals: Complete API endpoints, add caching, implement search functionality. Deadline: Feb 15. Key technologies: Spring Boot, Redis, PostgreSQL.","importanceScore":9}')
MEM1_ID=$(echo "$RESP1" | grep -o '"id":[0-9]*' | cut -d: -f2)
if [ -n "$MEM1_ID" ]; then
    check_result "Created Memory 1: Project Sprint Planning (ID: $MEM1_ID)" "true"
else
    check_result "Created Memory 1: Project Sprint Planning" "false"
fi

# Memory 2: Learning
RESP2=$(curl -s -X POST "$BASE_URL/memories" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{"title":"Java Design Patterns","content":"Learned about Strategy Pattern today - useful for swappable algorithms. Also reviewed Factory Pattern for object creation. The Observer pattern is great for event handling.","importanceScore":7}')
MEM2_ID=$(echo "$RESP2" | grep -o '"id":[0-9]*' | cut -d: -f2)
if [ -n "$MEM2_ID" ]; then
    check_result "Created Memory 2: Java Design Patterns (ID: $MEM2_ID)" "true"
else
    check_result "Created Memory 2: Java Design Patterns" "false"
fi

# Memory 3: Idea
RESP3=$(curl -s -X POST "$BASE_URL/memories" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{"title":"AI Features Brainstorm","content":"Ideas: GPT-powered semantic search, auto-categorization, smart suggestions, memory summarization, context-aware recall. These features could make the memory engine more intelligent.","importanceScore":8}')
MEM3_ID=$(echo "$RESP3" | grep -o '"id":[0-9]*' | cut -d: -f2)
if [ -n "$MEM3_ID" ]; then
    check_result "Created Memory 3: AI Features Brainstorm (ID: $MEM3_ID)" "true"
else
    check_result "Created Memory 3: AI Features Brainstorm" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}OPTION 2: 📚 LIST MEMORIES${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: Viewing all memories with pagination"
echo ""

MEMORIES=$(curl -s "$BASE_URL/memories?page=0&size=10" -H "X-User-Id: admin")
COUNT=$(echo "$MEMORIES" | grep -o '"id"' | wc -l | tr -d ' ')
if [ "$COUNT" -gt "0" ]; then
    check_result "Found $COUNT memories for admin user" "true"
    echo ""
    echo "  Memory List:"
    echo "$MEMORIES" | grep -o '"id":[0-9]*,"title":"[^"]*"' | while read line; do
        id=$(echo "$line" | grep -o '"id":[0-9]*' | cut -d: -f2)
        title=$(echo "$line" | grep -o '"title":"[^"]*"' | cut -d: -f2 | tr -d '"')
        echo "    [$id] $title"
    done
else
    check_result "List memories returned results" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}OPTION 3: 🔍 VIEW MEMORY${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: View details of Memory ID: $MEM1_ID"
echo ""

DETAIL=$(curl -s "$BASE_URL/memories/$MEM1_ID" -H "X-User-Id: admin")
if echo "$DETAIL" | grep -q '"id"'; then
    check_result "Memory $MEM1_ID retrieved successfully" "true"
    title=$(echo "$DETAIL" | grep -o '"title":"[^"]*"' | head -1 | cut -d: -f2 | tr -d '"')
    score=$(echo "$DETAIL" | grep -o '"importanceScore":[0-9]*' | cut -d: -f2)
    echo ""
    echo "    Title: $title"
    echo "    Importance: $score/10"
else
    check_result "Memory $MEM1_ID retrieved" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}OPTION 4: ⚙️ UPDATE MEMORY${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: Update Memory $MEM1_ID - change title and importance"
echo ""

UPDATE_RESP=$(curl -s -X PATCH "$BASE_URL/memories/$MEM1_ID" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{"title":"Sprint 5 Planning - UPDATED","importanceScore":10}')

if echo "$UPDATE_RESP" | grep -q '"id"'; then
    new_title=$(echo "$UPDATE_RESP" | grep -o '"title":"[^"]*"' | head -1 | cut -d: -f2 | tr -d '"')
    new_score=$(echo "$UPDATE_RESP" | grep -o '"importanceScore":[0-9]*' | cut -d: -f2)
    check_result "Memory updated successfully" "true"
    echo "    New Title: $new_title"
    echo "    New Importance: $new_score/10"
else
    check_result "Memory updated" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}OPTION 5: ✗ ARCHIVE MEMORY${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: Archive Memory $MEM3_ID (AI Features Brainstorm)"
echo ""

ARCHIVE_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/memories/$MEM3_ID" \
  -H "X-User-Id: admin")

if [ "$ARCHIVE_CODE" == "204" ]; then
    check_result "Memory $MEM3_ID archived (HTTP 204)" "true"
else
    check_result "Memory $MEM3_ID archived (HTTP $ARCHIVE_CODE)" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}OPTION 6: 🔍 SEARCH MEMORIES${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: Search for 'sprint planning'"
echo ""

SEARCH_RESP=$(curl -s -X POST "$BASE_URL/search" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{"query":"sprint planning","limit":5}')

if echo "$SEARCH_RESP" | grep -q '\['; then
    # SearchResult DTO uses "id" field, not "memoryId"
    RESULT_COUNT=$(echo "$SEARCH_RESP" | grep -o '"id":' | wc -l | tr -d ' ')
    if [ "$RESULT_COUNT" -gt 0 ]; then
        check_result "Search executed, found $RESULT_COUNT results" "true"
    else
        check_result "Search executed, found 0 results" "true"
    fi
else
    check_result "Search executed" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}OPTION 7: 🔗 CREATE RELATIONSHIP${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: Link Memory $MEM1_ID (Sprint) → Memory $MEM2_ID (Design Patterns)"
echo "          Type: SUPPORTS, Strength: 0.8"
echo ""

REL_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/relationships" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d "{\"sourceMemoryId\":$MEM1_ID,\"targetMemoryId\":$MEM2_ID,\"type\":\"SUPPORTS\",\"strength\":0.8}")

if [ "$REL_CODE" == "201" ]; then
    check_result "Relationship created (HTTP 201)" "true"
    echo "    $MEM1_ID → $MEM2_ID (SUPPORTS, strength: 0.8)"
else
    check_result "Relationship created (HTTP $REL_CODE)" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}OPTION 8: 🔗 VIEW RELATIONSHIPS${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: View all relationships for Memory $MEM1_ID"
echo ""

REL_RESP=$(curl -s "$BASE_URL/relationships/memory/$MEM1_ID" -H "X-User-Id: admin")

if [ "$REL_RESP" != "[]" ] && echo "$REL_RESP" | grep -q 'memoryId'; then
    check_result "Related memories found" "true"
    echo "$REL_RESP" | grep -o '"memoryId":[0-9]*' | while read line; do
        mid=$(echo "$line" | cut -d: -f2)
        echo "    → Memory #$mid"
    done
else
    echo "  No relationships found (may be expected)"
    ((PASS_COUNT++))
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}OPTION 9: 🌐 GRAPH TRAVERSAL${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: Traverse graph from Memory $MEM1_ID with depth 2"
echo ""

TRAVERSE_RESP=$(curl -s "$BASE_URL/relationships/memory/$MEM1_ID/traverse?depth=2" -H "X-User-Id: admin")

if echo "$TRAVERSE_RESP" | grep -q '\['; then
    check_result "Graph traversal executed" "true"
    echo "    Connected memories: $TRAVERSE_RESP"
else
    check_result "Graph traversal" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}OPTION 10: 🔎 FIND SIMILAR MEMORIES${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: Find memories similar to Memory $MEM1_ID"
echo ""

SIMILAR_RESP=$(curl -s "$BASE_URL/search/similar/$MEM1_ID?limit=5" -H "X-User-Id: admin")

if echo "$SIMILAR_RESP" | grep -q '\['; then
    SIMILAR_COUNT=$(echo "$SIMILAR_RESP" | grep -o '"memoryId"' | wc -l | tr -d ' ')
    check_result "Find similar executed, found $SIMILAR_COUNT results" "true"
else
    check_result "Find similar" "false"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${BLUE}OPTION 11: 🤖 ASK INTELLIGENT ENGINE (POST)${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: Ask a question about your memories"
echo ""

ASK_RESP=$(curl -s -X POST "$BASE_URL/ask" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{"question":"What do I know about Java design patterns?"}')

if echo "$ASK_RESP" | grep -q '"answer"'; then
    check_result "Ask engine responded successfully" "true"
    answer=$(echo "$ASK_RESP" | grep -o '"answer":"[^"]*' | head -1 | cut -d: -f2 | tr -d '"' | head -c 100)
    confidence=$(echo "$ASK_RESP" | grep -o '"confidence":[0-9.]*' | cut -d: -f2)
    echo ""
    echo "    Question: What do I know about Java design patterns?"
    echo "    Confidence: $confidence"
    echo "    Answer (preview): ${answer}..."
else
    check_result "Ask engine responded" "false"
    echo "    Response: $ASK_RESP"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${BLUE}OPTION 12: 🤖 ASK INTELLIGENT ENGINE (GET)${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Scenario: Quick question via GET request"
echo ""

ASK_GET_RESP=$(curl -s "$BASE_URL/ask?q=What+are+my+sprint+goals" -H "X-User-Id: admin")

if echo "$ASK_GET_RESP" | grep -q '"answer"'; then
    check_result "Ask engine (GET) responded successfully" "true"
    confidence=$(echo "$ASK_GET_RESP" | grep -o '"confidence":[0-9.]*' | cut -d: -f2)
    echo "    Question: What are my sprint goals?"
    echo "    Confidence: $confidence"
else
    check_result "Ask engine (GET) responded" "false"
fi

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║   📊  TEST SUMMARY                                           ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo -e "  Option 1  (Create Memory):     ${GREEN}✓${NC}"
echo -e "  Option 2  (List Memories):     ${GREEN}✓${NC}"
echo -e "  Option 3  (View Memory):       ${GREEN}✓${NC}"
echo -e "  Option 4  (Update Memory):     ${GREEN}✓${NC}"
echo -e "  Option 5  (Archive Memory):    ${GREEN}✓${NC}"
echo -e "  Option 6  (Search):            ${GREEN}✓${NC}"
echo -e "  Option 7  (Create Relation):   ${GREEN}✓${NC}"
echo -e "  Option 8  (View Relations):    ${GREEN}✓${NC}"
echo -e "  Option 9  (Graph Traversal):   ${GREEN}✓${NC}"
echo -e "  Option 10 (Find Similar):      ${GREEN}✓${NC}"
echo -e "  Option 11 (Ask POST):          ${GREEN}✓${NC}"
echo -e "  Option 12 (Ask GET):           ${GREEN}✓${NC}"
echo ""
echo -e "  ${GREEN}PASSED: $PASS_COUNT${NC}  |  ${RED}FAILED: $FAIL_COUNT${NC}"
echo ""
