#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    # Export only non-comment, non-empty lines
    export $(grep -v '^#' .env | xargs)
else
    echo "❌ Error: .env file not found"
    exit 1
fi

# Set default values if not set in .env
OLLAMA_MODEL=${OLLAMA_MODEL:-qwen2.5:1.5b}
OLLAMA_HOST=${OLLAMA_HOST:-localhost}
OLLAMA_PORT=${OLLAMA_PORT:-11434}
MOCK_EMAILS=${MOCK_EMAILS:-true}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if URL is accessible
check_url() {
    local url="$1"
    if curl -s --head --request GET "$url" | grep -q "HTTP/.*200"; then
        return 0
    else
        return 1
    fi
}

# Check for required commands
for cmd in groovy curl ollama; do
    if ! command_exists "$cmd"; then
        echo "❌ Error: $cmd is required but not installed."
        exit 1
    fi
done

# Function to start Ollama
start_ollama() {
    echo "🔍 Checking if Ollama is running..."
    
    # Check if Ollama is already running
    if check_url "http://${OLLAMA_HOST}:${OLLAMA_PORT}/api/version"; then
        echo "✅ Ollama is already running"
        return 0
    fi
    
    # Start Ollama
    echo "🚀 Starting Ollama in the background..."
    nohup ollama serve > ollama.log 2>&1 &
    export OLLAMA_PID=$!
    
    # Wait for Ollama to start
    echo "⏳ Waiting for Ollama to start (max 30 seconds)..."
    local max_attempts=30
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if check_url "http://${OLLAMA_HOST}:${OLLAMA_PORT}/api/version"; then
            echo "✅ Ollama started successfully"
            return 0
        fi
        sleep 1
        attempt=$((attempt + 1))
    done
    
    echo "❌ Failed to start Ollama after $max_attempts seconds"
    return 1
}

# Function to check if model is available
check_model() {
    echo "🔍 Checking if model '${OLLAMA_MODEL}' is available..."
    
    if ollama list | grep -q "${OLLAMA_MODEL}"; then
        echo "✅ Model '${OLLAMA_MODEL}' is already downloaded"
        return 0
    fi
    
    echo "⬇️  Model '${OLLAMA_MODEL}' not found. Attempting to download..."
    if ollama pull "${OLLAMA_MODEL}"; then
        echo "✅ Successfully downloaded model '${OLLAMA_MODEL}'"
        return 0
    else
        echo "❌ Failed to download model '${OLLAMA_MODEL}'"
        return 1
    fi
}

# Main execution
echo ""
echo "🚀 Starting Email Automation System"
echo "================================"
echo "🤖 Model: ${OLLAMA_MODEL}"
echo "🌐 Host: ${OLLAMA_HOST}:${OLLAMA_PORT}"
echo "🎭 Mock Mode: ${MOCK_EMAILS}"
echo ""

# Ensure logs directory exists
mkdir -p logs

# Start Ollama
if ! start_ollama; then
    echo "❌ Failed to start Ollama. Please check the logs and try again."
    exit 1
fi

# Check model availability if not in mock mode
if [ "${MOCK_EMAILS}" != "true" ]; then
    if ! check_model; then
        echo "⚠️  Falling back to mock mode"
        export MOCK_EMAILS="true"
    fi
fi

# Run the Groovy script
echo -e "\n🚀 Starting Groovy Email Processor..."
echo "   Press Ctrl+C to stop"
echo "   Logs will be saved to logs/email_processor.log"
echo "================================================"

# Run Groovy in the background
nohup groovy -Dgroovy.grape.report.downloads=true email.groovy > logs/email_processor.log 2>&1 &
GROOVY_PID=$!

# Cleanup function
cleanup() {
    echo -e "\n🛑 Stopping processes..."
    kill $GROOVY_PID 2>/dev/null
    if [ ! -z "$OLLAMA_PID" ]; then
        kill $OLLAMA_PID 2>/dev/null
    fi
    echo "✅ Stopped all processes"
    exit 0
}

# Set up trap to catch Ctrl+C
trap cleanup INT

# Keep script running
wait $GROOVY_PID

# Set up trap to catch Ctrl+C
trap cleanup INT

# Show logs in real-time
tail -f logs/email_processor.log