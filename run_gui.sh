#!/bin/bash
# Script to start the Digital Memory UI

UI_DIR="/Users/vinayakg08/Documents/digital-memory-ui"

echo "🚀 Starting Digital Memory UI..."
echo "📂 UI Directory: $UI_DIR"

if [ ! -d "$UI_DIR" ]; then
    echo "❌ Error: UI directory not found at $UI_DIR"
    exit 1
fi

cd "$UI_DIR"
npm run dev
