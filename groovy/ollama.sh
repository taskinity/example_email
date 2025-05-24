#!/bin/bash

# Kolory dla lepszej czytelności
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 OLLAMA SERVER STARTUP SCRIPT${NC}"
echo -e "${BLUE}================================${NC}"

# Funkcja logowania
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Sprawdzenie architektury systemu
ARCH=$(uname -m)
OS=$(uname -s)
log_info "Wykryto system: ${OS} ${ARCH}"

# Dostępne modele 2B (optymalne dla słabszego sprzętu)
MODELS_2B=(
    "llama3.2:1b"      # Najszybszy - 1B parametrów
    "gemma2:2b"        # Dobry balans jakość/szybkość - 2B parametrów
    "qwen2.5:1.5b"     # Świetny do języków azjatyckich - 1.5B
    "phi3.5"           # Microsoft model - ~3.8B ale zoptymalizowany
    "tinyllama"        # Ultra szybki - 1.1B parametrów
)

# Konfiguracja
OLLAMA_HOST=${OLLAMA_HOST:-"localhost"}
OLLAMA_PORT=${OLLAMA_PORT:-"11434"}
SELECTED_MODEL=${OLLAMA_MODEL:-"gemma2:2b"}
OLLAMA_URL="http://${OLLAMA_HOST}:${OLLAMA_PORT}"

# Funkcja sprawdzenia czy Ollama jest zainstalowana
check_ollama_installed() {
    if command -v ollama &> /dev/null; then
        log_info "Ollama jest zainstalowana: $(ollama --version 2>/dev/null || echo 'unknown version')"
        return 0
    else
        log_warn "Ollama nie jest zainstalowana"
        return 1
    fi
}

# Funkcja instalacji Ollama
install_ollama() {
    log_info "Instalacja Ollama..."

    case "$OS" in
        "Linux")
            # Automatyczna instalacja dla Linux
            curl -fsSL https://ollama.ai/install.sh | sh
            ;;
        "Darwin")
            # macOS
            if command -v brew &> /dev/null; then
                brew install ollama
            else
                log_error "Na macOS zainstaluj brew lub pobierz Ollama z https://ollama.ai"
                exit 1
            fi
            ;;
        *)
            log_error "Nieobsługiwany system operacyjny: $OS"
            log_info "Pobierz Ollama z https://ollama.ai"
            exit 1
            ;;
    esac

    # Sprawdzenie czy instalacja się powiodła
    if check_ollama_installed; then
        log_info "✅ Ollama zainstalowana pomyślnie"
    else
        log_error "❌ Instalacja Ollama nie powiodła się"
        exit 1
    fi
}

# Funkcja sprawdzenia czy Ollama server działa
check_ollama_running() {
    if curl -s "${OLLAMA_URL}/api/tags" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Funkcja uruchamiania Ollama server
start_ollama_server() {
    log_info "Uruchamianie Ollama server..."

    if check_ollama_running; then
        log_info "✅ Ollama server już działa na ${OLLAMA_URL}"
        return 0
    fi

    # Uruchomienie w tle
    case "$OS" in
        "Linux")
            # Sprawdź czy systemd service istnieje
            if systemctl list-unit-files | grep -q ollama.service; then
                log_info "Uruchamianie przez systemctl..."
                sudo systemctl start ollama
                sudo systemctl enable ollama
            else
                log_info "Uruchamianie w tle..."
                nohup ollama serve > ollama.log 2>&1 &
                echo $! > ollama.pid
            fi
            ;;
        "Darwin")
            # macOS - uruchomienie w tle
            log_info "Uruchamianie w tle na macOS..."
            nohup ollama serve > ollama.log 2>&1 &
            echo $! > ollama.pid
            ;;
    esac

    # Oczekiwanie na uruchomienie
    log_info "Oczekiwanie na uruchomienie serwera..."
    for i in {1..30}; do
        if check_ollama_running; then
            log_info "✅ Ollama server uruchomiony pomyślnie!"
            return 0
        fi
        sleep 2
        echo -n "."
    done

    log_error "❌ Nie udało się uruchomić Ollama server"
    return 1
}

# Funkcja pobierania modelu
pull_model() {
    local model=$1
    log_info "Pobieranie modelu: ${model}"

    # Sprawdź czy model już istnieje
    if ollama list | grep -q "$model"; then
        log_info "✅ Model ${model} już istnieje"
        return 0
    fi

    # Pobierz model
    log_info "⬇️ Pobieranie ${model}... (może potrwać kilka minut)"
    if ollama pull "$model"; then
        log_info "✅ Model ${model} pobrany pomyślnie"
        return 0
    else
        log_error "❌ Nie udało się pobrać modelu ${model}"
        return 1
    fi
}

# Funkcja testowania modelu
test_model() {
    local model=$1
    log_info "Testowanie modelu: ${model}"

    local test_prompt="Napisz krótką odpowiedź na email: 'Dzień dobry, mam pytanie o Państwa produkt.'"

    log_info "Wysyłanie test promptu..."
    local response=$(curl -s -X POST "${OLLAMA_URL}/api/generate" \
        -d "{\"model\":\"${model}\",\"prompt\":\"${test_prompt}\",\"stream\":false}" \
        | grep -o '"response":"[^"]*"' | sed 's/"response":"//;s/"$//')

    if [ -n "$response" ] && [ "$response" != "null" ]; then
        log_info "✅ Model działa! Przykładowa odpowiedź:"
        echo -e "${BLUE}${response}${NC}"
        return 0
    else
        log_error "❌ Model nie odpowiada poprawnie"
        return 1
    fi
}

# Funkcja wyświetlania menu wyboru modelu
select_model() {
    echo -e "\n${YELLOW}Dostępne modele 2B (optymalne dla słabszego sprzętu):${NC}"
    echo "1) llama3.2:1b     - Najszybszy, 1B parametrów (~1.3GB RAM)"
    echo "2) gemma2:2b       - Dobry balans, 2B parametrów (~2.6GB RAM) [DOMYŚLNY]"
    echo "3) qwen2.5:1.5b    - Wielojęzyczny, 1.5B parametrów (~2GB RAM)"
    echo "4) phi3.5          - Microsoft, ~3.8B parametrów (~4GB RAM)"
    echo "5) tinyllama       - Ultra szybki, 1.1B parametrów (~1.4GB RAM)"
    echo "6) Własny model"
    echo ""

    read -p "Wybierz model (1-6) [domyślnie 2]: " choice

    case $choice in
        1) SELECTED_MODEL="llama3.2:1b" ;;
        3) SELECTED_MODEL="qwen2.5:1.5b" ;;
        4) SELECTED_MODEL="phi3.5" ;;
        5) SELECTED_MODEL="tinyllama" ;;
        6)
            read -p "Podaj nazwę modelu: " custom_model
            SELECTED_MODEL="$custom_model"
            ;;
        *) SELECTED_MODEL="gemma2:2b" ;;
    esac

    log_info "Wybrany model: ${SELECTED_MODEL}"
}

# Funkcja główna
main() {
    echo -e "${BLUE}Parametry:${NC}"
    echo "- Host: ${OLLAMA_HOST}"
    echo "- Port: ${OLLAMA_PORT}"
    echo "- Model: ${SELECTED_MODEL}"
    echo ""

    # Sprawdź argumenty
    case "${1:-}" in
        "install")
            install_ollama
            exit $?
            ;;
        "stop")
            log_info "Zatrzymywanie Ollama server..."
            if [ -f ollama.pid ]; then
                kill $(cat ollama.pid) 2>/dev/null
                rm ollama.pid
            fi
            sudo systemctl stop ollama 2>/dev/null || true
            log_info "✅ Ollama zatrzymana"
            exit 0
            ;;
        "status")
            if check_ollama_running; then
                log_info "✅ Ollama server działa na ${OLLAMA_URL}"
                echo -e "\n${YELLOW}Dostępne modele:${NC}"
                ollama list
            else
                log_warn "❌ Ollama server nie działa"
            fi
            exit 0
            ;;
        "select")
            select_model
            ;;
        "test")
            if [ -n "${2:-}" ]; then
                test_model "$2"
            else
                test_model "$SELECTED_MODEL"
            fi
            exit $?
            ;;
    esac

    # Główny flow
    log_info "Rozpoczynanie konfiguracji Ollama..."

    # 1. Sprawdź/zainstaluj Ollama
    if ! check_ollama_installed; then
        read -p "Czy zainstalować Ollama? (y/N): " install_choice
        if [[ $install_choice =~ ^[Yy]$ ]]; then
            install_ollama
        else
            log_error "Ollama jest wymagana do działania"
            exit 1
        fi
    fi

    # 2. Uruchom server
    if ! start_ollama_server; then
        log_error "Nie udało się uruchomić Ollama server"
        exit 1
    fi

    # 3. Wybierz model (jeśli nie podano argumentu)
    if [[ "${1:-}" == "select" ]] || [[ -z "${OLLAMA_MODEL:-}" ]]; then
        select_model
    fi

    # 4. Pobierz model
    if ! pull_model "$SELECTED_MODEL"; then
        log_error "Nie udało się pobrać modelu"
        exit 1
    fi

    # 5. Testuj model
    if ! test_model "$SELECTED_MODEL"; then
        log_warn "Model może mieć problemy, ale kontynuujemy..."
    fi

    # 6. Podsumowanie
    echo -e "\n${GREEN}🎉 OLLAMA SERVER GOTOWY!${NC}"
    echo -e "${GREEN}================================${NC}"
    echo "🔗 URL: ${OLLAMA_URL}"
    echo "🤖 Model: ${SELECTED_MODEL}"
    echo "📊 Status: ollama list"
    echo "🔥 Test: curl -X POST ${OLLAMA_URL}/api/generate -d '{\"model\":\"${SELECTED_MODEL}\",\"prompt\":\"Hello\",\"stream\":false}'"
    echo ""
    echo -e "${YELLOW}Dostępne komendy:${NC}"
    echo "./start-ollama-2b.sh status   - sprawdź status"
    echo "./start-ollama-2b.sh stop     - zatrzymaj server"
    echo "./start-ollama-2b.sh select   - wybierz inny model"
    echo "./start-ollama-2b.sh test     - przetestuj model"
    echo ""

    # Zapisz konfigurację do .env
    if [ ! -f .env.ollama ]; then
        cat > .env.ollama << EOF
# Ollama configuration
OLLAMA_HOST=${OLLAMA_HOST}
OLLAMA_PORT=${OLLAMA_PORT}
OLLAMA_MODEL=${SELECTED_MODEL}
OLLAMA_URL=${OLLAMA_URL}
EOF
        log_info "💾 Konfiguracja zapisana do .env.ollama"
    fi
}

# Sprawdź czy script jest uruchamiany bezpośrednio
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi