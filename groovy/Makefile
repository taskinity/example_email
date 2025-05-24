.PHONY: test install start stop clean

# Domyślny model
MODEL ?= qwen2.5:1.5b

test:
	@echo "🧪 Test systemu..."
	@chmod +x test.sh
	@./test.sh

install:
	@echo "📦 Instalacja zależności..."
	@groovy -e "@Grab('org.apache.camel:camel-core:4.4.0') println 'Camel OK'"
	@groovy -e "@Grab('org.slf4j:slf4j-simple:2.0.9') println 'SLF4J OK'"

start:
	@echo "🚀 Uruchamianie systemu..."
	@make test
	@groovy email.groovy

start-debug:
	@echo "🐛 Uruchamianie w trybie debug..."
	@groovy -Dgroovy.grape.report.downloads=true \
	        -Djava.util.logging.level=ALL \
	        email.groovy

stop:
	@echo "⏹️ Zatrzymywanie..."
	@pkill -f "email.groovy" || true
	@pkill -f "ollama serve" || true

clean:
	@echo "🧹 Czyszczenie..."
	@rm -f *.log
	@rm -f .env.backup

status:
	@echo "📊 Status systemu:"
	@echo -n "Ollama: "
	@curl -s http://localhost:11434/api/tags > /dev/null && echo "✅ UP" || echo "❌ DOWN"
	@echo -n "Model $(MODEL): "
	@ollama list | grep -q "$(MODEL)" && echo "✅ READY" || echo "❌ NOT_FOUND"

model:
	@echo "📥 Instalacja modelu $(MODEL)..."
	@ollama pull $(MODEL)
	@echo "✅ Model $(MODEL) gotowy"

logs:
	@echo "📋 Ostatnie logi:"
	@tail -20 *.log 2>/dev/null || echo "Brak logów"

help:
	@echo "🔧 Dostępne komendy:"
	@echo "make test         - test systemu"
	@echo "make install      - instalacja zależności"
	@echo "make start        - uruchomienie systemu"
	@echo "make start-debug  - tryb debug"
	@echo "make stop         - zatrzymanie"
	@echo "make status       - sprawdź status"
	@echo "make model MODEL=llama3.2:1b - zainstaluj model"
	@echo "make clean        - wyczyść pliki tymczasowe"
	@echo "make logs         - pokaż logi"