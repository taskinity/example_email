#!/bin/bash

# Load environment variables
source .env

# Function to check if a command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Check for required commands
for cmd in docker docker-compose curl; do
  if ! command_exists "$cmd"; then
    echo "Error: $cmd is required but not installed."
    exit 1
  fi
done

echo "🚀 Starting Email Automation System with Ollama"
echo "=========================================="

# Create necessary directories
mkdir -p logs

# Start services using docker-compose
echo "Starting Docker containers..."
docker-compose up -d

# Wait for Ollama to be ready
echo -n "Waiting for Ollama to be ready"
until curl -s http://localhost:11434/api/health >/dev/null; do
  echo -n "."
  sleep 1
done
echo -e "\n✅ Ollama is ready!"

# Check if model is downloaded
MODEL_EXISTS=$(curl -s http://localhost:11434/api/tags | grep -c "${OLLAMA_MODEL}")
if [ "$MODEL_EXISTS" -eq 0 ]; then
  echo "Downloading Ollama model: ${OLLAMA_MODEL}"
  curl -X POST http://localhost:11434/api/pull -d "{\"name\": \"${OLLAMA_MODEL}\"}"
fi

# Start the Groovy email processor
echo "Starting Email Processor..."
docker-compose logs -f email-processor

# Show help
echo ""
echo "📧 Email Automation System is running!"
echo "-----------------------------------"
echo "MailHog Web UI:    http://localhost:8025"
echo "Ollama API:        http://localhost:11434"
echo "View logs:         docker-compose logs -f"
echo "Stop services:     docker-compose down"
