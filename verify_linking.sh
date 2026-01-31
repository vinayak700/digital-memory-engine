#!/bin/bash

BASE_URL="http://localhost:8082/api/v1"
AUTH_USER="vinayak08_test"
AUTH_PASS="VinayakTest1!"
TITLE="The Java Virtual Machine (JVM) - Java's Secret Superpower"
CONTENT="The JVM is the runtime engine that executes compiled Java bytecode, providing the \"write once, run anywhere\" capability that made Java revolutionary. It's not just an interpreter—it's a sophisticated system with Just-In-Time (JIT) compilation, garbage collection, memory management, and security features. Understanding the JVM means understanding how Java really works under the hood: from class loading and bytecode verification to hotspot optimization and garbage collection algorithms (like G1, ZGC, Shenandoah). Modern JVMs can even outperform natively compiled code in some scenarios through adaptive optimization. Key Aspects: Bytecode execution and JIT compilation. Memory structure (heap, stack, method area, PC registers). Garbage collection algorithms and tuning. Class loading mechanism and security sandbox. Performance monitoring and troubleshooting tools (jvisualvm, jstack, jmap). JVM languages beyond Java (Kotlin, Scala, Groovy)"

echo "1. Creating first memory..."
RESPONSE1=$(curl -s -X POST "$BASE_URL/memories" \
  -u "$AUTH_USER:$AUTH_PASS" \
  -H "Content-Type: application/json" \
  -d @memory_payload.json)

ID1=$(echo $RESPONSE1 | grep -o '"id":[0-9]*' | head -1 | awk -F':' '{print $2}')
echo "Created Memory 1 with ID: $ID1"

echo "2. Creating second memory (identical)..."
RESPONSE2=$(curl -s -X POST "$BASE_URL/memories" \
  -u "$AUTH_USER:$AUTH_PASS" \
  -H "Content-Type: application/json" \
  -d @memory_payload.json)

ID2=$(echo $RESPONSE2 | grep -o '"id":[0-9]*' | head -1 | awk -F':' '{print $2}')
echo "Created Memory 2 with ID: $ID2"

echo "3. Waiting for async processing (10 seconds)..."
sleep 10

echo "4. Checking details of Memory 1 (ID: $ID1)..."
DETAILS=$(curl -s -X GET "$BASE_URL/memories/$ID1" \
  -u "$AUTH_USER:$AUTH_PASS")

# Fetch related memories separately if they are not in the main memory response
# Based on RelationshipController, there might be a specific endpoint for related memories
# /api/v1/relationships/memory/{memoryId} ?
# Let's check RelationshipController.java again via tool to be sure, but usually it's separate or embedded.
# I'll check /api/v1/memories/{id} first.

echo "Memory 1 Details:"
echo $DETAILS

# Also check related endpoint if exists
RELATED=$(curl -s -X GET "$BASE_URL/relationships/memory/$ID1" \
  -u "$AUTH_USER:$AUTH_PASS")
echo "Related Memories for ID $ID1:"
echo $RELATED

if [[ $RELATED == *"$ID2"* ]]; then
  echo "SUCCESS: Memory 1 is linked to Memory 2!"
else
  echo "FAILURE: Memory 1 is NOT linked to Memory 2."
fi
