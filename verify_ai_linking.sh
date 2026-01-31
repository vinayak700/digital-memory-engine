#!/bin/bash

BASE_URL="http://localhost:8082/api/v1"
AUTH_USER="vinayak08_test"
AUTH_PASS="VinayakTest1!"

TITLE1="JVM Garbage Collection - An Overview"
CONTENT1="Garbage collection in Java is an automatic process that manages memory. It identifies and deletes unused objects to free up space in the heap. Understanding how it works is key to optimizing Java application performance."

TITLE2="Tuning the G1 Garbage Collector in Java"
CONTENT2="The G1 (Garbage First) collector is a modern garbage collection algorithm designed for multi-processor machines with large memory. Tuning its parameters like MaxGCPauseMillis and ParallelGCThreads can significantly improve latency."

echo "1. Creating Memory A (JVM GC Overview)..."
RESPONSE1=$(curl -s -X POST "$BASE_URL/memories" \
  -u "$AUTH_USER:$AUTH_PASS" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"$TITLE1\",
    \"content\": \"$CONTENT1\",
    \"importanceScore\": 7
  }")

ID1=$(echo $RESPONSE1 | grep -o '"id":[0-9]*' | head -1 | awk -F':' '{print $2}')
echo "Created Memory A with ID: $ID1"

echo "2. Creating Memory B (G1 Tuning)..."
RESPONSE2=$(curl -s -X POST "$BASE_URL/memories" \
  -u "$AUTH_USER:$AUTH_PASS" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"$TITLE2\",
    \"content\": \"$CONTENT2\",
    \"importanceScore\": 8
  }")

ID2=$(echo $RESPONSE2 | grep -o '"id":[0-9]*' | head -1 | awk -F':' '{print $2}')
echo "Created Memory B with ID: $ID2"

echo "3. Waiting for async AI processing (15 seconds)..."
sleep 15

echo "4. Verifying link from A to B..."
RELATED_A=$(curl -s -X GET "$BASE_URL/relationships/memory/$ID1" \
  -u "$AUTH_USER:$AUTH_PASS")

if [[ $RELATED_A == *"$ID2"* ]]; then
  echo "SUCCESS: Memory A is linked to Memory B!"
else
  echo "FAILURE: Memory A is NOT linked to Memory B."
fi

echo "5. Verifying link from B to A (Bidirectional Check)..."
RELATED_B=$(curl -s -X GET "$BASE_URL/relationships/memory/$ID2" \
  -u "$AUTH_USER:$AUTH_PASS")

if [[ $RELATED_B == *"$ID1"* ]]; then
  echo "SUCCESS: Memory B is linked to Memory A!"
else
  echo "FAILURE: Memory B is NOT linked to Memory A."
fi
