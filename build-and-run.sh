#!/bin/bash

set -e

echo "🚀 Building and starting services..."
docker-compose up -d --build

echo "✅ Services started!"
echo "📊 API: http://localhost:8080"
echo "📝 Logs: docker-compose logs -f"
