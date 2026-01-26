#!/bin/bash
# Comprehensive API Test Script for Digital Memory Engine
# Tests all endpoints with various scenarios

BASE_URL="http://localhost:8082"
API_URL="$BASE_URL/api/v1"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'
PASS=0
FAIL=0

header() {
    echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}  $1${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

test_endpoint() {
    local name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_code="$5"
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -H "X-User-Id: test-user" \
            -d "$data" "$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "X-User-Id: test-user" "$endpoint")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [[ "$http_code" =~ ^$expected_code ]]; then
        echo -e "${GREEN}✓${NC} $name (HTTP $http_code)"
        ((PASS++))
        echo "$body"
        return 0
    else
        echo -e "${RED}✗${NC} $name (Expected $expected_code, got $http_code)"
        echo "Response: $body"
        ((FAIL++))
        return 1
    fi
}

echo "════════════════════════════════════════════════════"
echo "   Digital Memory Engine - Comprehensive API Tests   "
echo "════════════════════════════════════════════════════"

# Wait for server
echo -n "Waiting for server..."
for i in {1..30}; do
    if curl -s "$BASE_URL/actuator/health" | grep -q "UP"; then
        echo -e " ${GREEN}Ready!${NC}"
        break
    fi
    sleep 1
    echo -n "."
done

header "1. SYSTEM ENDPOINTS"
test_endpoint "Health Check" "GET" "$BASE_URL/actuator/health" "" "200"
test_endpoint "API Test" "GET" "$BASE_URL/api/test" "" "200"

header "2. MEMORY CRUD OPERATIONS"

# Create memories for testing
echo -e "\nCreating test memories..."
MEM1=$(curl -s -X POST -H "Content-Type: application/json" -H "X-User-Id: test-user" \
    -d '{"title":"Test Memory 1","content":"First test content","importanceScore":8}' \
    "$API_URL/memories")
MEM1_ID=$(echo "$MEM1" | grep -o '"id":[0-9]*' | cut -d: -f2)
echo -e "${GREEN}✓${NC} Created Memory 1 (ID: $MEM1_ID)"
((PASS++))

MEM2=$(curl -s -X POST -H "Content-Type: application/json" -H "X-User-Id: test-user" \
    -d '{"title":"Test Memory 2","content":"Second test content","importanceScore":5}' \
    "$API_URL/memories")
MEM2_ID=$(echo "$MEM2" | grep -o '"id":[0-9]*' | cut -d: -f2)
echo -e "${GREEN}✓${NC} Created Memory 2 (ID: $MEM2_ID)"
((PASS++))

test_endpoint "Get Memory by ID" "GET" "$API_URL/memories/$MEM1_ID" "" "200"
test_endpoint "List All Memories" "GET" "$API_URL/memories?page=0&size=10" "" "200"
test_endpoint "Update Memory" "PATCH" "$API_URL/memories/$MEM1_ID" '{"title":"Updated Title"}' "200"

header "3. SEARCH OPERATIONS"
test_endpoint "Search Memories" "POST" "$API_URL/search" '{"query":"test","limit":5}' "200"
test_endpoint "Find Similar" "GET" "$API_URL/search/similar/$MEM1_ID?limit=3" "" "200"

header "4. RELATIONSHIP OPERATIONS"
test_endpoint "Create Relationship" "POST" "$API_URL/relationships" \
    "{\"sourceMemoryId\":$MEM1_ID,\"targetMemoryId\":$MEM2_ID,\"type\":\"RELATED_TO\",\"strength\":0.8}" "201"
test_endpoint "Get Related Memories" "GET" "$API_URL/relationships/memory/$MEM1_ID" "" "200"
test_endpoint "Traverse Graph" "GET" "$API_URL/relationships/memory/$MEM1_ID/traverse?depth=2" "" "200"

header "5. ARCHIVE OPERATIONS"
test_endpoint "Archive Memory" "DELETE" "$API_URL/memories/$MEM1_ID" "" "204"

header "6. EDGE CASES"
test_endpoint "Get Non-Existent Memory" "GET" "$API_URL/memories/99999" "" "404"
test_endpoint "Invalid Create Request" "POST" "$API_URL/memories" '{"title":""}' "400"

# Summary
echo -e "\n════════════════════════════════════════════════════"
echo -e "   TEST SUMMARY"
echo -e "════════════════════════════════════════════════════"
echo -e "   ${GREEN}Passed: $PASS${NC}"
echo -e "   ${RED}Failed: $FAIL${NC}"
echo -e "════════════════════════════════════════════════════"

# Print all working curls
header "VERIFIED CURL COMMANDS"
cat << 'EOF'
# ========== SYSTEM ==========
curl -X GET http://localhost:8082/actuator/health
curl -X GET http://localhost:8082/api/test

# ========== MEMORIES ==========
# Create Memory
curl -X POST http://localhost:8082/api/v1/memories \
  -H "Content-Type: application/json" \
  -H "X-User-Id: vinayak" \
  -d '{"title":"My Memory","content":"Content here","importanceScore":7}'

# List Memories
curl -X GET "http://localhost:8082/api/v1/memories?page=0&size=10" \
  -H "X-User-Id: vinayak"

# Get Memory
curl -X GET http://localhost:8082/api/v1/memories/{id} \
  -H "X-User-Id: vinayak"

# Update Memory
curl -X PATCH http://localhost:8082/api/v1/memories/{id} \
  -H "Content-Type: application/json" \
  -H "X-User-Id: vinayak" \
  -d '{"title":"Updated Title","importanceScore":9}'

# Archive Memory
curl -X DELETE http://localhost:8082/api/v1/memories/{id} \
  -H "X-User-Id: vinayak"

# ========== SEARCH ==========
# Semantic Search
curl -X POST http://localhost:8082/api/v1/search \
  -H "Content-Type: application/json" \
  -H "X-User-Id: vinayak" \
  -d '{"query":"meeting notes","limit":5}'

# Find Similar
curl -X GET "http://localhost:8082/api/v1/search/similar/{id}?limit=5" \
  -H "X-User-Id: vinayak"

# ========== RELATIONSHIPS ==========
# Create Relationship
curl -X POST http://localhost:8082/api/v1/relationships \
  -H "Content-Type: application/json" \
  -H "X-User-Id: vinayak" \
  -d '{"sourceMemoryId":1,"targetMemoryId":2,"type":"RELATED_TO","strength":0.8}'

# Get Related Memories
curl -X GET http://localhost:8082/api/v1/relationships/memory/{id} \
  -H "X-User-Id: vinayak"

# Traverse Graph
curl -X GET "http://localhost:8082/api/v1/relationships/memory/{id}/traverse?depth=2" \
  -H "X-User-Id: vinayak"
EOF
