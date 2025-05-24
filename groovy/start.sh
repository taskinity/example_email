#!/bin/bash

# Load environment variables
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
else
    echo "❌ Error: .env file not found"
    exit 1
fi

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check for required commands
for cmd in groovy curl; do
    if ! command_exists "$cmd"; then
        echo "❌ Error: $cmd is required but not installed."
        exit 1
    fi
done

# Check if Ollama is running
check_ollama() {
    if ! curl -s http://localhost:11434/api/health >/dev/null; then
        echo "❌ Ollama is not running. Starting Ollama in the background..."
        nohup ollama serve > ollama.log 2>&1 &
        OLLAMA_PID=$!
        echo "🔄 Waiting for Ollama to start..."
        sleep 5
    else
        echo "✅ Ollama is already running"
    fi
}

# Check if model is available
download_model() {
    local model=$1
    echo "🔍 Checking if model '$model' is available..."
    
    if ! curl -s http://localhost:11434/api/tags | grep -q "$model"; then
        echo "⬇️  Downloading model: $model"
        ollama pull "$model"
        if [ $? -ne 0 ]; then
            echo "❌ Failed to download model: $model"
            return 1
        fi
    else
        echo "✅ Model '$model' is already downloaded"
    fi
    return 0
}

# Main execution
echo "🚀 Starting Email Automation System"
echo "================================"

# Start Ollama if not running
check_ollama

# Download model if needed
if ! download_model "$OLLAMA_MODEL"; then
    echo "❌ Cannot continue without the model. Exiting..."
    exit 1
fi

# Create logs directory if it doesn't exist
mkdir -p logs

# Run the Groovy script
echo -e "\n🚀 Starting Groovy Email Processor..."
echo "   Press Ctrl+C to stop"
echo "   Logs will be saved to logs/email_processor.log"
echo "================================================"

# Run the Groovy script
nohup groovy email.groovy > logs/email_processor.log 2>&1 &
GROOVY_PID=$!

# Function to clean up on exit
cleanup() {
    echo -e "\n🛑 Stopping processes..."
    kill $GROOVY_PID 2>/dev/null
    if [ ! -z "$OLLAMA_PID" ]; then
        kill $OLLAMA_PID 2>/dev/null
    fi
    exit 0
}

# Set up trap to catch Ctrl+C
trap cleanup INT

# Show logs
tail -f logs/email_processor.log