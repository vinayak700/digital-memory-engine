#!/bin/bash
# =============================================================================
#  DIGITAL MEMORY ENGINE - API PERFORMANCE TEST
#  Tests API latency, caching behavior, and Ask engine performance
#  Updated: 2026-01-26
# =============================================================================

BASE_URL="http://localhost:8082/api/v1"
GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

MEMORY_ID=""
PASS_COUNT=0
FAIL_COUNT=0

echo "══════════════════════════════════════════════════════════════"
echo "🚀 Digital Memory Engine - API Performance Test"
echo "══════════════════════════════════════════════════════════════"
echo ""

# Function to time a request
measure_request() {
    NAME=$1
    METHOD=$2
    ENDPOINT=$3
    DATA=$4
    
    echo -n "Testing $NAME... "
    
    if [ -z "$DATA" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}|%{time_total}" -H "X-User-Id: admin" -H "Content-Type: application/json" -X $METHOD "$BASE_URL$ENDPOINT")
    else
        RESPONSE=$(curl -s -w "\n%{http_code}|%{time_total}" -H "X-User-Id: admin" -H "Content-Type: application/json" -d "$DATA" -X $METHOD "$BASE_URL$ENDPOINT")
    fi
    
    BODY=$(echo "$RESPONSE" | head -n 1)
    STATS=$(echo "$RESPONSE" | tail -n 1)
    CODE=$(echo "$STATS" | cut -d'|' -f1)
    TIME=$(echo "$STATS" | cut -d'|' -f2)
    
    if [[ "$CODE" =~ ^2 ]]; then
        echo -e "${GREEN}✅ OK${NC} ($CODE) - ${CYAN}${TIME}s${NC}"
        ((PASS_COUNT++))
        # Extract ID if it's a creation
        if [[ "$NAME" == "Create Memory" ]]; then
            MEMORY_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | cut -d':' -f2)
            echo "   -> Created Memory ID: $MEMORY_ID"
        fi
    else
        echo -e "${RED}❌ FAILED${NC} ($CODE) - ${TIME}s"
        echo "   Response: $BODY"
        ((FAIL_COUNT++))
    fi
}

echo -e "${YELLOW}━━━ MEMORY OPERATIONS ━━━${NC}"
echo ""

# 1. Create Memory
measure_request "Create Memory" "POST" "/memories" '{"title":"Performance Test Memory","content":"Testing latency of Redis and Kafka integration. This memory will help us benchmark the system performance including caching layers.","importanceScore":8}'

if [ -z "$MEMORY_ID" ]; then
    echo -e "${RED}❌ Stopping tests because memory creation failed.${NC}"
    exit 1
fi

sleep 1

# 2. Get Memory (Cold - first fetch, no cache)
measure_request "Get Memory (Cold)" "GET" "/memories/$MEMORY_ID" ""

# 3. Get Memory (Cached - second fetch, should be cached)
measure_request "Get Memory (Cached)" "GET" "/memories/$MEMORY_ID" ""

# 4. Get Active Memories (Cold - first fetch of list)
measure_request "List Memories (Cold)" "GET" "/memories?page=0&size=10" ""

# 5. Get Active Memories (Cached - should be faster)
measure_request "List Memories (Cached)" "GET" "/memories?page=0&size=10" ""

# 6. Update Memory (Should evict cache)
measure_request "Update Memory" "PATCH" "/memories/$MEMORY_ID" '{"title":"Performance Test - Updated","importanceScore":9}'

# 7. Get Active Memories (Re-fetch after cache eviction)
measure_request "List Memories (Re-fetch)" "GET" "/memories?page=0&size=10" ""

echo ""
echo -e "${YELLOW}━━━ SEARCH OPERATIONS ━━━${NC}"
echo ""

# 8. Search (Full-text search)
measure_request "Search Memories" "POST" "/search" '{"query":"latency testing performance","limit":5}'

# 9. Find Similar
measure_request "Find Similar Memories" "GET" "/search/similar/$MEMORY_ID?limit=5" ""

echo ""
echo -e "${YELLOW}━━━ RELATIONSHIP OPERATIONS ━━━${NC}"
echo ""

# Create another memory for relationship testing
RESP2=$(curl -s -X POST "$BASE_URL/memories" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{"title":"Related Test Memory","content":"This is a related memory for testing relationships and graph operations.","importanceScore":6}')
MEM2_ID=$(echo "$RESP2" | grep -o '"id":[0-9]*' | cut -d: -f2)
echo -e "Created related memory: ID $MEM2_ID"

# 10. Create Relationship
measure_request "Create Relationship" "POST" "/relationships" "{\"sourceMemoryId\":$MEMORY_ID,\"targetMemoryId\":$MEM2_ID,\"type\":\"SUPPORTS\",\"strength\":0.75}"

# 11. Get Related Memories
measure_request "Get Related Memories" "GET" "/relationships/memory/$MEMORY_ID" ""

# 12. Graph Traversal
measure_request "Graph Traversal (depth=2)" "GET" "/relationships/memory/$MEMORY_ID/traverse?depth=2" ""

echo ""
echo -e "${BLUE}━━━ INTELLIGENT ASK ENGINE ━━━${NC}"
echo ""

# 13. Ask Question (POST - full payload)
measure_request "Ask Engine (POST)" "POST" "/ask" '{"question":"What do I know about performance testing?"}'

# 14. Ask Question (GET - quick query)
measure_request "Ask Engine (GET)" "GET" "/ask?q=What+is+being+tested" ""

# 15. Ask about patterns (testing keyword extraction)
measure_request "Ask Engine (patterns)" "POST" "/ask" '{"question":"What are the main topics in my memories?"}'

echo ""
echo -e "${YELLOW}━━━ CLEANUP ━━━${NC}"
echo ""

# Archive test memories
measure_request "Archive Memory 1" "DELETE" "/memories/$MEMORY_ID" ""
measure_request "Archive Memory 2" "DELETE" "/memories/$MEM2_ID" ""

echo ""
echo "══════════════════════════════════════════════════════════════"
echo -e "🏁 ${GREEN}Tests Completed${NC}"
echo "══════════════════════════════════════════════════════════════"
echo ""
echo -e "  ${GREEN}PASSED: $PASS_COUNT${NC}  |  ${RED}FAILED: $FAIL_COUNT${NC}"
echo ""
echo "Performance Notes:"
echo "  • Cold requests: First fetch, no cache - expect higher latency"
echo "  • Cached requests: Should be significantly faster"
echo "  • Ask Engine: Uses NLP + full-text search, higher latency expected"
echo ""
