#!/bin/bash

# Load environment variables
source .env

# Check if Ollama is already running
if ! curl -s "http://${OLLAMA_HOST}:${OLLAMA_PORT}" > /dev/null; then
    echo "Starting Ollama server..."
    nohup ollama serve > ollama.log 2>&1 &
    OLLAMA_PID=$!
    echo $OLLAMA_PID > ollama.pid
    
    # Wait for server to start
    echo -n "Waiting for Ollama to start"
    until curl -s "http://${OLLAMA_HOST}:${OLLAMA_PORT}" > /dev/null; do
        echo -n "."
        sleep 1
    done
    echo -e "\nOllama server started with PID: $OLLAMA_PID"
    
    # Pull the model if not exists
    echo "Checking for model: ${OLLAMA_MODEL}"
    if ! ollama list | grep -q "${OLLAMA_MODEL}"; then
        echo "Pulling model: ${OLLAMA_MODEL}"
        ollama pull ${OLLAMA_MODEL}
    fi
    
    echo "Ollama server is ready at http://${OLLAMA_HOST}:${OLLAMA_PORT}"
else
    echo "Ollama server is already running at http://${OLLAMA_HOST}:${OLLAMA_PORT}"
fi
