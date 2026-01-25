#!/bin/bash

BASE_URL="http://localhost:8082/api/v1"
AUTH="-u admin:admin"
CONTENT_TYPE="-H 'Content-Type: application/json'"

echo "=================================================="
echo "🚀 Digital Memory Engine - API Performance Test"
echo "=================================================="

# Function to time a request
measure_request() {
    NAME=$1
    METHOD=$2
    ENDPOINT=$3
    DATA=$4
    
    echo -n "Testing $NAME... "
    
    if [ -z "$DATA" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}|%{time_total}" $AUTH $METHOD "$BASE_URL$ENDPOINT")
    else
        RESPONSE=$(curl -s -w "\n%{http_code}|%{time_total}" $AUTH $CONTENT_TYPE -d "$DATA" $METHOD "$BASE_URL$ENDPOINT")
    fi
    
    BODY=$(echo "$RESPONSE" | head -n 1)
    STATS=$(echo "$RESPONSE" | tail -n 1)
    CODE=$(echo "$STATS" | cut -d'|' -f1)
    TIME=$(echo "$STATS" | cut -d'|' -f2)
    
    if [[ "$CODE" =~ ^2 ]]; then
        echo "✅ OK ($CODE) - ${TIME}s"
        # Extract ID if it's a creation
        if [[ "$NAME" == "Create Memory" ]]; then
            MEMORY_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | cut -d':' -f2)
            echo "   -> Created Memory ID: $MEMORY_ID"
        fi
    else
        echo "❌ FAILED ($CODE) - ${TIME}s"
        echo "   Response: $BODY"
    fi
}

# 1. Create Memory
measure_request "Create Memory" "POST" "/memories" '{"title":"Performance Test Memory","content":"Testing latency of Redis and Kafka integration.","importanceScore":8}'

if [ -z "$MEMORY_ID" ]; then
    echo "❌ Stopping tests because memory creation failed."
    exit 1
fi

sleep 1

# 2. Get Memory (Cold)
measure_request "Get Memory (Cold)" "GET" "/memories/$MEMORY_ID" ""

# 3. Get Memory (Cached)
measure_request "Get Memory (Cached)" "GET" "/memories/$MEMORY_ID" ""

# 4. Get Active Memories (Cold)
measure_request "List Memories (Cold)" "GET" "/memories?page=0&size=10" ""

# 5. Get Active Memories (Cached)
measure_request "List Memories (Cached)" "GET" "/memories?page=0&size=10" ""

# 6. Update Memory (Should evict list cache)
measure_request "Update Memory" "PATCH" "/memories/$MEMORY_ID" '{"title":"Performance Test Updated","importanceScore":9}'

# 7. Get Active Memories (Should be Cold after eviction)
measure_request "List Memories (Re-fetch)" "GET" "/memories?page=0&size=10" ""

echo "=================================================="
echo "🏁 Tests Completed"
echo "=================================================="
