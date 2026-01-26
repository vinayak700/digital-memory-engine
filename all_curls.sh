#!/bin/bash

BASE_URL="http://localhost:8082"
API_URL="$BASE_URL/api/v1"
# Define headers without internal quotes for safer variable expansion
H_TYPE="-H Content-Type:application/json"
H_USER="-H X-User-Id:vinayak"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "=================================================="
echo "🚀 Digital Memory Engine - Comprehensive API Test"
echo "=================================================="

# Function to run test
run_test() {
    NAME=$1
    CMD=$2
    
    echo -n "Testing $NAME... "
    # We use eval to execute the command string
    RESPONSE=$(eval "$CMD")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    # Remove the last line (status code) to get body
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [[ "$HTTP_CODE" =~ ^2 ]]; then
        echo -e "${GREEN}✅ OK ($HTTP_CODE)${NC}"
        return 0
    else
        echo -e "${RED}❌ FAILED ($HTTP_CODE)${NC}"
        echo "Response: $BODY"
        return 1
    fi
}

# 1. System Endpoints
echo "--- System Endpoints ---"
run_test "Home Page" "curl -s -w '\n%{http_code}' '$BASE_URL/'"
run_test "API Test" "curl -s -w '\n%{http_code}' $H_TYPE $H_USER '$BASE_URL/api/test'"
run_test "Health Check" "curl -s -w '\n%{http_code}' '$BASE_URL/actuator/health'"

# 2. Memory CRUD
echo -e "\n--- Memory CRUD ---"
# Create Memory A
echo -n "Creating Memory A... "
RESP=$(curl -s -w '\n%{http_code}' $H_TYPE $H_USER -d '{"title":"Memory A","content":"Testing creation","importanceScore":5}' "$API_URL/memories")
MEM_A_ID=$(echo "$RESP" | head -n1 | grep -o '"id":[0-9]*' | cut -d':' -f2)

if [ -n "$MEM_A_ID" ]; then
    echo -e "${GREEN}✅ Success (ID: $MEM_A_ID)${NC}"
else
    echo -e "${RED}❌ FAILED${NC}"
    echo "$RESP"
    exit 1
fi

# Create Memory B (for relationship)
echo -n "Creating Memory B... "
RESP=$(curl -s -w '\n%{http_code}' $H_TYPE $H_USER -d '{"title":"Memory B","content":"Testing relationship","importanceScore":8}' "$API_URL/memories")
MEM_B_ID=$(echo "$RESP" | head -n1 | grep -o '"id":[0-9]*' | cut -d':' -f2)
echo -e "${GREEN}✅ Success (ID: $MEM_B_ID)${NC}"

# Get Memory A
run_test "Get Memory A" "curl -s -w '\n%{http_code}' $H_USER '$API_URL/memories/$MEM_A_ID'"

# Update Memory A
run_test "Update Memory A" "curl -s -w '\n%{http_code}' $H_TYPE $H_USER -X PATCH -d '{\"title\":\"Memory A Updated\"}' '$API_URL/memories/$MEM_A_ID'"

# List Memories
run_test "List Memories" "curl -s -w '\n%{http_code}' $H_USER '$API_URL/memories?page=0&size=5'"

# 3. Search
echo -e "\n--- Search ---"
run_test "Search Query" "curl -s -w '\n%{http_code}' $H_TYPE $H_USER -X POST -d '{\"query\":\"testing\",\"limit\":5}' '$API_URL/search'"
run_test "Find Similar" "curl -s -w '\n%{http_code}' $H_USER '$API_URL/search/similar/$MEM_A_ID'"

# 4. Relationships
echo -e "\n--- Relationships ---"
# Create Relationship
run_test "Create Relationship" "curl -s -w '\n%{http_code}' $H_TYPE $H_USER -X POST -d '{\"sourceMemoryId\":$MEM_A_ID,\"targetMemoryId\":$MEM_B_ID,\"type\":\"RELATED_TO\",\"strength\":0.8}' '$API_URL/relationships'"

# Get Related
run_test "Get Related (Memory A)" "curl -s -w '\n%{http_code}' $H_USER '$API_URL/relationships/memory/$MEM_A_ID'"

# Traverse
run_test "Traverse Graph (Memory A)" "curl -s -w '\n%{http_code}' $H_USER '$API_URL/relationships/memory/$MEM_A_ID/traverse?depth=2'"

# 5. Cleanup (Archive/Delete)
echo -e "\n--- Cleanup ---"
run_test "Archive Memory A" "curl -s -w '\n%{http_code}' $H_USER -X DELETE '$API_URL/memories/$MEM_A_ID'"

echo "=================================================="
echo "📝 Verified CURL Commands"
echo "=================================================="
cat <<EOF
# 1. System
curl -X GET $BASE_URL/
curl -X GET $BASE_URL/api/test
curl -X GET $BASE_URL/actuator/health

# 2. Memories
curl -H "Content-Type: application/json" -H "X-User-Id: vinayak" -X POST -d '{"title":"My Memory","content":"Details...","importanceScore":5}' $API_URL/memories
curl -H "X-User-Id: vinayak" -X GET "$API_URL/memories?page=0&size=10"
curl -H "X-User-Id: vinayak" -X GET $API_URL/memories/{id}
curl -H "Content-Type: application/json" -H "X-User-Id: vinayak" -X PATCH -d '{"title":"Updated Title"}' $API_URL/memories/{id}
curl -H "X-User-Id: vinayak" -X DELETE $API_URL/memories/{id}

# 3. Search
curl -H "Content-Type: application/json" -H "X-User-Id: vinayak" -X POST -d '{"query":"search term","limit":5}' $API_URL/search
curl -H "X-User-Id: vinayak" -X GET $API_URL/search/similar/{id}

# 4. Relationships
curl -H "Content-Type: application/json" -H "X-User-Id: vinayak" -X POST -d '{"sourceMemoryId":1,"targetMemoryId":2,"type":"RELATED_TO","strength":0.8}' $API_URL/relationships
curl -H "X-User-Id: vinayak" -X GET $API_URL/relationships/memory/{id}
curl -H "X-User-Id: vinayak" -X GET "$API_URL/relationships/memory/{id}/traverse?depth=2"
EOF
