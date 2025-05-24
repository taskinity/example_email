#!/bin/bash

echo "🧪 Quick test Camel + Ollama"

# Sprawdź czy Ollama działa
if curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "✅ Ollama UP"
else
    echo "❌ Ollama DOWN - uruchamiam..."
    ollama serve &
    sleep 5

    # Sprawdź czy model istnieje
    if ! ollama list | grep -q "qwen2.5:1.5b"; then
        echo "📥 Pobieranie modelu qwen2.5:1.5b..."
        ollama pull qwen2.5:1.5b
    fi
fi

# Test modelu
echo "🧪 Test modelu..."
curl -X POST http://localhost:11434/api/generate \
  -d '{"model":"qwen2.5:1.5b","prompt":"Odpowiedz krótko: Dzień dobry","stream":false}' \
  -H "Content-Type: application/json" | grep -o '"response":"[^"]*"'

echo ""
echo "✅ Gotowe! Uruchom: groovy email.groovy"