# Apache Camel Groovy + Ollama Email Automation

**Dokumentacja kompletnego systemu automatyzacji emaili z lokalnym AI**

---

## 📋 Spis treści

1. [Przegląd systemu](#przegląd-systemu)
2. [Architektura](#architektura)
3. [Instalacja i konfiguracja](#instalacja-i-konfiguracja)
4. [Struktura projektu](#struktura-projektu)
5. [Opis działania](#opis-działania)
6. [Konfiguracja .env](#konfiguracja-env)
7. [Apache Camel Routes](#apache-camel-routes)
8. [Integracja z Ollama](#integracja-z-ollama)
9. [Uruchamianie i zarządzanie](#uruchamianie-i-zarządzanie)
10. [Monitoring i debugging](#monitoring-i-debugging)
11. [Rozwiązywanie problemów](#rozwiązywanie-problemów)
12. [API Reference](#api-reference)
13. [Przykłady użycia](#przykłady-użycia)

---

## 📋 Przegląd systemu

### Cel projektu
System automatyzacji emaili wykorzystujący **Apache Camel** w **Groovy** oraz lokalny model AI **Ollama** do generowania inteligentnych odpowiedzi na przychodzące wiadomości email.

### Kluczowe cechy
- ✅ **Lokalne AI** - Ollama działa offline, zero kosztów API
- ✅ **Enterprise routing** - Apache Camel zapewnia niezawodność
- ✅ **Minimalny kod** - tylko 40 linii Groovy
- ✅ **Konfiguracja .env** - łatwe zarządzanie parametrami
- ✅ **Mock mode** - testowanie bez prawdziwych emaili
- ✅ **Automatic failover** - fallback do standardowych odpowiedzi
- ✅ **Multi-protocol** - obsługa IMAP/SMTP/IMAPS/SMTPS

### Technologie
| Technologia | Wersja | Rola |
|-------------|--------|------|
| **Apache Camel** | 4.4.0 | Enterprise Integration Patterns |
| **Groovy** | 4.0+ | Skryptowy język na JVM |
| **Ollama** | Latest | Lokalne modele AI (LLM) |
| **Java** | 17+ | Runtime environment |

---

## 🏗️ Architektura

### Diagram przepływu danych
```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐    ┌──────────────┐
│   IMAP/     │    │   Apache     │    │   Ollama    │    │   SMTP/      │
│   Mock      │───▶│   Camel      │───▶│   Local AI  │───▶│   Email      │
│   Emails    │    │   Routes     │    │   Model     │    │   Response   │
└─────────────┘    └──────────────┘    └─────────────┘    └──────────────┘
                           │                   │
                           ▼                   ▼
                   ┌──────────────┐    ┌─────────────┐
                   │   Error      │    │  Fallback   │
                   │   Handling   │    │  Response   │
                   └──────────────┘    └─────────────┘
```

### Komponenty systemu

#### 1. **Email Input Layer**
- **IMAP Client** - pobieranie emaili z serwera
- **Mock Generator** - symulacja emaili do testów
- **Timer Trigger** - cykliczne sprawdzanie

#### 2. **Processing Layer**
- **Apache Camel Routes** - orkiestracja przepływu
- **Ollama Integration** - komunikacja z lokalnym AI
- **Error Handling** - obsługa błędów i fallback

#### 3. **Output Layer**
- **SMTP Client** - wysyłanie odpowiedzi
- **Email Formatting** - przygotowanie wiadomości
- **Logging** - rejestracja operacji

---

## 🔧 Instalacja i konfiguracja

### Wymagania systemowe
| Komponent | Minimalne | Rekomendowane |
|-----------|-----------|---------------|
| **RAM** | 4GB | 8GB+ |
| **CPU** | 2 cores | 4+ cores |
| **Dysk** | 5GB | 10GB+ |
| **Java** | OpenJDK 17+ | OpenJDK 21+ |
| **OS** | Linux/macOS/Windows | Linux |

### Automatyczna instalacja
```bash
# Pobierz skrypty
curl -O https://raw.githubusercontent.com/taskinity/install.sh
chmod +x install.sh

# Instalacja jedną komendą
./install.sh

# Lub step-by-step
./start-ollama.sh install
```

### Manualna instalacja

#### 1. Instalacja Java 17+
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17

# Weryfikacja
java -version
```

#### 2. Instalacja Groovy
```bash
# Przez SDKMAN (rekomendowane)
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh
sdk install groovy

# Ubuntu/Debian
sudo apt install groovy

# macOS
brew install groovy

# Weryfikacja
groovy --version
```

#### 3. Instalacja Ollama
```bash
# Linux/macOS
curl -fsSL https://ollama.ai/install.sh | sh

# Windows
# Pobierz z https://ollama.ai

# Weryfikacja
ollama --version
```

#### 4. Pobieranie modelu AI
```bash
# Start Ollama server
ollama serve &

# Pobierz model (wybierz jeden)
ollama pull gemma2:2b          # Rekomendowany balans
ollama pull llama3.2:1b        # Najszybszy
ollama pull qwen2.5:1.5b       # Wielojęzyczny
ollama pull tinyllama          # Ultra szybki

# Sprawdź dostępne modele
ollama list
```

---

## 📁 Struktura projektu

```
email-automation-camel-ollama/
│
├── 📄 email-automation-ollama.groovy    # Główny skrypt systemu
├── 📄 .env                              # Konfiguracja środowiska
├── 📄 start-ollama-2b.sh               # Skrypt uruchamiający Ollama
├── 📄 install.sh                       # Instalator systemu
├── 📄 docker-compose.yml               # Docker setup
├── 📄 Makefile                         # Automatyzacja zadań
│
├── 📁 logs/                            # Katalog logów
│   ├── email_automation.log
│   └── ollama.log
│
├── 📁 docs/                            # Dokumentacja
│   ├── README.md
│   ├── SETUP.md
│   └── TROUBLESHOOTING.md
│
└── 📁 examples/                        # Przykłady użycia
    ├── test-email-templates/
    └── custom-models/
```

---

## ⚙️ Opis działania

### Proces przetwarzania emaila (krok po kroku)

#### 1. **Email Input (Pobieranie)**
```groovy
// Mock emails lub rzeczywiste IMAP
from("timer://mockTimer?period=30000")  // Co 30 sekund
    .log("🧪 Generowanie mock email...")
    .process { ex ->
        // Symulacja emaila
        ex.in.setHeader("from", "klient@example.com")
        ex.in.setHeader("subject", "Pytanie o produkt")
        ex.in.body = "Dzień dobry, interesuje mnie produkt XYZ"
    }
```

**Co się dzieje:**
1. Timer aktywuje się co X sekund (konfigurowane w .env)
2. W trybie mock generowane są przykładowe emaile
3. W trybie IMAP pobierane są nieprzeczytane wiadomości
4. Każdy email zostaje przekierowany do przetwarzania

#### 2. **AI Processing (Ollama)**
```groovy
from("direct:processWithOllama")
    .process { ex ->
        // Przygotowanie promptu
        def prompt = """Otrzymałeś email od klienta:
        Od: ${sender}
        Temat: ${subject}
        Treść: ${emailBody}
        
        Napisz profesjonalną odpowiedź w języku polskim..."""
        
        // Payload dla Ollama
        def ollamaPayload = new JsonBuilder([
            model: "gemma2:2b",
            prompt: prompt,
            stream: false
        ])
    }
    .to("http://localhost:11434/api/generate")
```

**Co się dzieje:**
1. Email zostaje sformatowany do promptu dla AI
2. Wysyłane jest zapytanie HTTP POST do Ollama API
3. Ollama przetwarza prompt przez model AI
4. Zwracana jest wygenerowana odpowiedź

#### 3. **Response Formatting (Formatowanie)**
```groovy
.process { ex ->
    // Parsowanie odpowiedzi Ollama
    def response = jsonSlurper.parseText(ex.in.body.toString())
    def aiResponse = response.response?.trim()
    
    if (aiResponse) {
        ex.in.body = aiResponse
    } else {
        // Fallback do standardowej odpowiedzi
        ex.in.body = generateStandardResponse()
    }
}
```

**Co się dzieje:**
1. Odpowiedź JSON z Ollama zostaje sparsowana
2. Wyciągany jest tekst odpowiedzi
3. W przypadku błędu używana jest odpowiedź fallback
4. Tekst zostaje przygotowany do wysłania

#### 4. **Email Output (Wysyłanie)**
```groovy
from("direct:sendReply")
    .setHeader("To", simple("${header.originalSender}"))
    .setHeader("Subject", simple("Re: ${header.originalSubject}"))
    .setHeader("From", simple("${config['FROM_EMAIL']}"))
    .recipientList(simple("smtp://server?username=...&password=..."))
```

**Co się dzieje:**
1. Ustawiane są nagłówki emaila (To, Subject, From)
2. Budowany jest URL SMTP z parametrami z .env
3. Email zostaje wysłany przez Apache Camel Mail
4. Logowane jest potwierdzenie wysłania

### Obsługa błędów

#### Global Error Handler
```groovy
onException(Exception.class)
    .log("❌ BŁĄD: ${exception.message}")
    .handled(true)
    .to("direct:sendErrorNotification")
```

**Typy błędów i ich obsługa:**

| Błąd | Obsługa | Akcja |
|------|---------|-------|
| **Ollama niedostępne** | Fallback response | Standardowa odpowiedź |
| **IMAP connection** | Retry 3x | Logowanie błędu |
| **SMTP send error** | Dead letter queue | Email do admina |
| **JSON parsing** | Exception handling | Fallback response |
| **Model timeout** | Circuit breaker | Alternatywny model |

---

## 🔧 Konfiguracja .env

### Kompletny plik .env z opisami
```env
# ==========================================
# EMAIL SERVER CONFIGURATION
# ==========================================

# SMTP (Outgoing mail server)
SMTP_SERVER=mailserver                    # SMTP server hostname
SMTP_PORT=25                             # Port: 25 (plain), 587 (TLS), 465 (SSL)
SMTP_USERNAME=user@taskinity.org         # SMTP login username
SMTP_PASSWORD=password123                # SMTP password
FROM_EMAIL=user@taskinity.org            # From address in sent emails
REPLY_TO_EMAIL=support@taskinity.org     # Reply-To address

# IMAP (Incoming mail server)  
IMAP_SERVER=mailserver                   # IMAP server hostname
IMAP_PORT=143                           # Port: 143 (plain), 993 (SSL)
IMAP_USERNAME=user@taskinity.org        # IMAP login username
IMAP_PASSWORD=password123               # IMAP password
IMAP_FOLDER=INBOX                       # Folder to monitor for emails

# ==========================================
# PROCESSING CONFIGURATION
# ==========================================

# Email processing behavior
MOCK_EMAILS=true                        # true=simulate emails, false=use real IMAP
EMAIL_LIMIT=3                          # Max emails to process per cycle
CHECK_INTERVAL_SECONDS=30              # Seconds between email checks
TEST_EMAIL=info@softreck.dev           # Email address for testing
CONTINUOUS_MODE=true                   # true=keep running, false=single run
KEEP_RUNNING=true                      # Keep container running after processing

# ==========================================
# OLLAMA AI CONFIGURATION  
# ==========================================

# Ollama server settings
OLLAMA_HOST=localhost                   # Ollama server hostname
OLLAMA_PORT=11434                      # Ollama API port
OLLAMA_MODEL=gemma2:2b                 # AI model to use

# Model alternatives:
# OLLAMA_MODEL=llama3.2:1b             # Fastest (1B parameters)
# OLLAMA_MODEL=qwen2.5:1.5b            # Multilingual (1.5B parameters)  
# OLLAMA_MODEL=tinyllama               # Ultra fast (1.1B parameters)
# OLLAMA_MODEL=phi3.5                  # Microsoft model (~3.8B parameters)

# ==========================================
# DOCKER & DEVELOPMENT
# ==========================================

SEND_TEST_EMAILS=true                  # Send test emails on startup
PYTHONPATH=/app                        # Python path for compatibility

# ==========================================
# LOGGING CONFIGURATION
# ==========================================

LOG_LEVEL=INFO                         # Log level: DEBUG, INFO, WARN, ERROR
LOG_FILE=/var/log/email_processor.log  # Log file path
LOG_DIR=./logs                         # Log directory
```

### Konfiguracje środowiskowe

#### Środowisko deweloperskie
```env
MOCK_EMAILS=true
EMAIL_LIMIT=1
CHECK_INTERVAL_SECONDS=10
LOG_LEVEL=DEBUG
OLLAMA_MODEL=tinyllama
```

#### Środowisko testowe
```env
MOCK_EMAILS=false
EMAIL_LIMIT=5
CHECK_INTERVAL_SECONDS=60
LOG_LEVEL=INFO
OLLAMA_MODEL=gemma2:2b
SMTP_SERVER=test-smtp.company.com
```

#### Środowisko produkcyjne
```env
MOCK_EMAILS=false
EMAIL_LIMIT=10
CHECK_INTERVAL_SECONDS=300
LOG_LEVEL=WARN
OLLAMA_MODEL=gemma2:2b
SMTP_SERVER=smtp.company.com
IMAP_SERVER=imap.company.com
```

---

## 🛣️ Apache Camel Routes

### Szczegółowy opis route'ów

#### 1. Email Input Routes

**Mock Email Generator**
```groovy
from("timer://mockTimer?period=${config['CHECK_INTERVAL_SECONDS']}000")
    .routeId("mock-email-generator")
    .log("🧪 Generowanie mock email...")
    .process { exchange ->
        def mockEmails = [
            [from: "jan.kowalski@example.com", 
             subject: "Pytanie o produkt", 
             body: "Interesuje mnie produkt XYZ"],
            [from: "anna.nowak@firma.pl", 
             subject: "Reklamacja", 
             body: "Otrzymałam wadliwy produkt"]
        ]
        def randomEmail = mockEmails[new Random().nextInt(mockEmails.size())]
        exchange.in.setHeader("from", randomEmail.from)
        exchange.in.setHeader("subject", randomEmail.subject)
        exchange.in.body = randomEmail.body
    }
    .to("direct:processWithOllama")
```

**Real IMAP Route**
```groovy
def imapUrl = config['IMAP_PORT'] == '993' ? 
    "imaps://${config['IMAP_SERVER']}:${config['IMAP_PORT']}" :
    "imap://${config['IMAP_SERVER']}:${config['IMAP_PORT']}"

from("${imapUrl}?" +
     "username=${config['IMAP_USERNAME']}&" +
     "password=${config['IMAP_PASSWORD']}&" +
     "delete=false&unseen=true&" +
     "folderName=${config['IMAP_FOLDER']}&" +
     "consumer.delay=${config['CHECK_INTERVAL_SECONDS']}000&" +
     "maxMessagesPerPoll=${config['EMAIL_LIMIT']}")
    .routeId("real-email-fetcher")
    .log("📧 Otrzymano email od: ${header.from}")
    .to("direct:processWithOllama")
```

**Parametry IMAP:**
- `delete=false` - nie usuwaj emaili po przeczytaniu
- `unseen=true` - tylko nieprzeczytane wiadomości
- `consumer.delay` - interwał sprawdzania (ms)
- `maxMessagesPerPoll` - max emaili na raz

#### 2. AI Processing Route

```groovy
from("direct:processWithOllama")
    .routeId("ollama-ai-processor")
    .log("🤖 Przetwarzanie przez Ollama...")
    .process { exchange ->
        // Wyciągnięcie danych z emaila
        def emailBody = exchange.in.body?.toString() ?: ""
        def sender = exchange.in.getHeader("from")?.toString() ?: "unknown"
        def subject = exchange.in.getHeader("subject")?.toString() ?: "no subject"
        
        // Przechowanie oryginalnych danych
        exchange.in.setHeader("originalSender", sender)
        exchange.in.setHeader("originalSubject", subject)
        
        // Konstruowanie promptu
        def prompt = buildPrompt(sender, subject, emailBody)
        
        // Payload dla Ollama API
        def ollamaPayload = new JsonBuilder([
            model: config['OLLAMA_MODEL'],
            prompt: prompt,
            stream: false,
            options: [
                temperature: 0.7,
                max_tokens: 200,
                top_p: 0.9
            ]
        ])
        
        exchange.in.setHeader("Content-Type", "application/json")
        exchange.in.body = ollamaPayload.toString()
    }
    .to("http://${config['OLLAMA_HOST']}:${config['OLLAMA_PORT']}/api/generate")
    .process { exchange ->
        // Parsowanie odpowiedzi Ollama
        parseOllamaResponse(exchange)
    }
    .to("direct:sendReply")
```

**Prompt Engineering:**
```groovy
def buildPrompt(sender, subject, body) {
    return """
    CONTEXT: Jesteś profesjonalnym asystentem obsługi klienta.
    
    EMAIL DO ODPOWIEDZI:
    Od: ${sender}
    Temat: ${subject}
    Treść: ${body}
    
    ZADANIE: Napisz profesjonalną odpowiedź w języku polskim.
    
    WYMAGANIA:
    - Uprzejma i profesjonalna forma
    - Konkretna odpowiedź na problem klienta
    - Maksymalnie 150 słów
    - Zakończenie: "Pozdrawienia, Zespół obsługi klienta"
    
    ODPOWIEDŹ:
    """
}
```

#### 3. Email Output Route

```groovy
from("direct:sendReply")
    .routeId("email-sender")
    .log("📤 Wysyłanie odpowiedzi do: ${header.originalSender}")
    .setHeader("To", simple("${header.originalSender}"))
    .setHeader("Subject", simple("Re: ${header.originalSubject}"))
    .setHeader("From", simple("${config['FROM_EMAIL']}"))
    .setHeader("Reply-To", simple("${config['REPLY_TO_EMAIL']}"))
    .setHeader("Content-Type", constant("text/plain; charset=UTF-8"))
    .process { exchange ->
        // Dynamiczne budowanie SMTP URL
        def smtpUrl = buildSmtpUrl()
        exchange.in.setHeader("smtpUrl", smtpUrl)
    }
    .recipientList(simple("${header.smtpUrl}?" +
        "username=${config['SMTP_USERNAME']}&" +
        "password=${config['SMTP_PASSWORD']}&" +
        "contentType=text/plain"))
    .log("✅ Odpowiedź wysłana pomyślnie!")
```

**SMTP URL Builder:**
```groovy
def buildSmtpUrl() {
    def port = config['SMTP_PORT']
    def server = config['SMTP_SERVER']
    
    switch(port) {
        case '465':
            return "smtps://${server}:${port}"  // SSL
        case '587':
            return "smtp://${server}:${port}?mail.smtp.starttls.enable=true"  // TLS
        default:
            return "smtp://${server}:${port}"  // Plain
    }
}
```

#### 4. Error Handling Routes

```groovy
// Global error handler
onException(Exception.class)
    .log("❌ BŁĄD: ${exception.message}")
    .log("❌ Stack trace: ${exception.stackTrace}")
    .handled(true)
    .to("direct:handleError")

// Specific error handlers
onException(ConnectException.class)
    .log("🔌 Błąd połączenia - próba ponowna za 30s")
    .maximumRedeliveries(3)
    .redeliveryDelay(30000)
    .handled(true)

onException(SocketTimeoutException.class)
    .log("⏱️ Timeout - używam fallback response")
    .handled(true)
    .setBody(constant("Dziękuję za email. Odpowiemy w ciągu 24h."))
    .to("direct:sendReply")

// Error notification route
from("direct:handleError")
    .routeId("error-handler")
    .log("📧 Wysyłanie powiadomienia o błędzie")
    .setBody(simple("Błąd w systemie: ${exception.message}"))
    .setHeader("To", simple("${config['FROM_EMAIL']}"))
    .setHeader("Subject", constant("BŁĄD: Email Automation"))
    .to("direct:sendErrorEmail")
```

---

## 🤖 Integracja z Ollama

### Ollama API Reference

#### Endpoint główny
```
POST http://localhost:11434/api/generate
Content-Type: application/json
```

#### Request format
```json
{
  "model": "gemma2:2b",
  "prompt": "Your prompt here",
  "stream": false,
  "options": {
    "temperature": 0.7,
    "max_tokens": 200,
    "top_p": 0.9,
    "repeat_penalty": 1.1
  }
}
```

#### Response format
```json
{
  "model": "gemma2:2b",
  "created_at": "2024-01-15T10:30:00Z",
  "response": "Generated response text here",
  "done": true,
  "context": [1, 2, 3, ...],
  "total_duration": 1500000000,
  "load_duration": 500000000,
  "prompt_eval_count": 50,
  "eval_count": 100
}
```

### Dostępne modele

#### Rekomendowane modele 2B
| Model | Rozmiar | RAM | Języki | Specjalizacja |
|-------|---------|-----|--------|---------------|
| **gemma2:2b** | 2.6GB | ~3GB | EN, podstawowe PL | Ogólne zastosowanie |
| **llama3.2:1b** | 1.3GB | ~2GB | EN, podstawowe PL | Najszybszy |
| **qwen2.5:1.5b** | 2.0GB | ~2.5GB | EN, CN, PL, inne | Wielojęzyczny |
| **phi3.5** | 3.8GB | ~4GB | EN, PL | Microsoft, optymalizowany |
| **tinyllama** | 1.4GB | ~2GB | EN | Ultra szybki, prosty |

#### Zarządzanie modelami
```bash
# Lista dostępnych modeli
ollama list

# Pobierz nowy model
ollama pull llama3.2:1b

# Usuń model
ollama rm old-model

# Informacje o modelu
ollama show gemma2:2b

# Uruchom model interaktywnie
ollama run gemma2:2b
```

### Optymalizacja wydajności

#### Parametry modelu
```groovy
def optimizedOptions = [
    temperature: 0.7,        // Kreatywność (0.0-2.0)
    max_tokens: 150,         // Maksymalna długość odpowiedzi
    top_p: 0.9,             // Nucleus sampling
    top_k: 40,              // Top-K sampling
    repeat_penalty: 1.1,     // Kara za powtórzenia
    num_predict: 100,        // Liczba tokenów do przewidzenia
    num_ctx: 2048           // Rozmiar kontekstu
]
```

#### Model Performance Tuning
```env
# .env optimization dla różnych środowisk

# Środowisko słabe (2-4GB RAM)
OLLAMA_MODEL=tinyllama
OLLAMA_MAX_TOKENS=100
OLLAMA_TEMPERATURE=0.5

# Środowisko średnie (4-8GB RAM)  
OLLAMA_MODEL=llama3.2:1b
OLLAMA_MAX_TOKENS=150
OLLAMA_TEMPERATURE=0.7

# Środowisko silne (8GB+ RAM)
OLLAMA_MODEL=gemma2:2b
OLLAMA_MAX_TOKENS=200
OLLAMA_TEMPERATURE=0.8
```

### Monitoring Ollama

#### Health Check
```groovy
from("timer://healthCheck?period=60000")
    .routeId("ollama-health-check")
    .to("http://localhost:11434/api/tags")
    .choice()
        .when(body().contains("models"))
            .log("✅ Ollama działa poprawnie")
        .otherwise()
            .log("❌ Ollama nie odpowiada")
            .to("direct:handleOllamaDown")
```

#### Performance Metrics
```bash
# Sprawdź zużycie zasobów
docker stats ollama  # Jeśli Docker
htop                  # System monitor
nvidia-smi            # GPU usage (jeśli CUDA)

# Ollama logs
ollama logs
tail -f ~/.ollama/logs/server.log
```

---

## 🚀 Uruchamianie i zarządzanie

### Metody uruchomienia

#### 1. Uruchomienie lokalne
```bash
# Przygotowanie środowiska
./start-ollama-2b.sh
source .env

# Uruchomienie systemu
groovy email-automation-ollama.groovy

# W tle z logowaniem
nohup groovy email-automation-ollama.groovy > system.log 2>&1 &
```

#### 2. Docker Compose
```bash
# Uruchomienie wszystkich serwisów
docker-compose up -d

# Sprawdź status
docker-compose ps

# Logi
docker-compose logs -f email-automation

# Zatrzymanie
docker-compose down
```

#### 3. Systemd Service (Linux)
```bash
# Utworzenie service file
sudo tee /etc/systemd/system/email-automation.service << EOF
[Unit]
Description=Email Automation with Camel and Ollama
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/email-automation
ExecStart=/usr/bin/groovy email-automation-ollama.groovy
Restart=always
RestartSec=10
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk

[Install]
WantedBy=multi-user.target
EOF

# Aktywacja
sudo systemctl daemon-reload
sudo systemctl enable email-automation
sudo systemctl start email-automation

# Status
sudo systemctl status email-automation
```

### Makefile commands

```bash
# Podstawowe operacje
make start                    # Uruchom system
make stop                     # Zatrzymaj system  
make restart                  # Restart systemu
make status                   # Sprawdź status

# Zarządzanie modelami
make install-model MODEL=gemma2:2b    # Zainstaluj model
make test-model MODEL=gemma2:2b       # Przetestuj model
make list-models                      # Lista modeli

# Development
make dev                      # Uruchom w trybie dev (debug)
make test                     # Uruchom testy
make clean                    # Wyczyść logi i temp files

# Docker
make docker-build            # Zbuduj obrazy
make docker-up               # Uruchom kontener
make docker-down             # Zatrzymaj kontenery
make docker-logs             # Pokaż logi
```

### Środowiska uruchomieniowe

#### Development environment
```bash
# .env.development
MOCK_EMAILS=true
EMAIL_LIMIT=1
CHECK_INTERVAL_SECONDS=10
LOG_LEVEL=DEBUG
OLLAMA_MODEL=tinyllama

# Uruchomienie
cp .env.development .env
make dev
```

#### Production environment
```bash
# .env.production
MOCK_EMAILS=false
EMAIL_LIMIT=10
CHECK_INTERVAL_SECONDS=300
LOG_LEVEL=INFO
OLLAMA_MODEL=gemma2:2b

# Uruchomienie
cp .env.production .env
make start
```

---

## 📊 Monitoring i debugging

### Logging system

#### Log levels i ich znaczenie
```groovy
// DEBUG - szczegółowe informacje dla deweloperów
log.debug("Przetwarzanie emaila: ${exchange.in.body}")

// INFO - normalne operacje systemu
log.info("✅ Email wysłany do: ${recipient}")

// WARN - potencjalne problemy
log.warn("⚠️ Ollama odpowiada wolno: ${duration}ms")

// ERROR - błędy wymagające uwagi
log.error("❌ Nie można połączyć z SMTP: ${exception.message}")
```

#### Struktura logów
```
2024-01-15 10:30:15 INFO  [main] 🚀 APACHE CAMEL EMAIL AUTOMATION
2024-01-15 10:30:16 INFO  [Camel Thread] 📧 Email od: jan.kowalski@example.com
2024-01-15 10:30:17 DEBUG [Camel Thread] 🤖 Prompt: Otrzymałeś email...
2024-01-15 10:30:19 INFO  [Camel Thread] ✅ Ollama response: Dzień dobry...
2024-01-15 10:30:20 INFO  [Camel Thread] 📤 Wysyłanie do: jan.kowalski@example.com
2024-01-15 10:30:21 INFO  [Camel Thread] ✅ Odpowiedź wysłana pomyślnie!
```

### Metryki systemu

#### Apache Camel Metrics
```groovy
// Dodaj do route'a dla zbierania metryk
from("direct:processEmail")
    .routeId("email-processor")
    .process("emailCounter")  // Custom processor liczący emaile
    .process("responseTimeTracker")  // Śledzenie czasu odpowiedzi
    .to("direct:ollama")
```

#### Custom Metrics Processor
```groovy
class EmailMetricsProcessor implements Processor {
    static Map<String, Integer> counters = [:]
    static Map<String, Long> responseTimes = [:]
    
    void process(Exchange exchange) throws Exception {
        def route = exchange.fromRouteId
        counters[route] = (counters[route] ?: 0) + 1
        
        def startTime = System.currentTimeMillis()
        exchange.setProperty("startTime", startTime)
        
        log.info("📊 Metrics - Route: ${route}, Count: ${counters[route]}")
    }
}
```

### Health Checks

#### System Health Monitor
```groovy
from("timer://healthCheck?period=60000")
    .routeId("system-health-monitor")
    .process { exchange ->
        def health = [:]
        
        // Sprawdź Ollama
        try {
            def response = new URL("http://localhost:11434/api/tags").text
            health.ollama = "UP"
        } catch (Exception e) {
            health.ollama = "DOWN"
            log.error("❌ Ollama health check failed: ${e.message}")
        }
        
        // Sprawdź SMTP
        try {
            def socket = new Socket(config['SMTP_SERVER'], config['SMTP_PORT'] as Integer)
            socket.close()
            health.smtp = "UP"
        } catch (Exception e) {
            health.smtp = "DOWN"
            log.error("❌ SMTP health check failed: ${e.message}")
        }
        
        // Sprawdź IMAP
        try {
            def socket = new Socket(config['IMAP_SERVER'], config['IMAP_PORT'] as Integer)
            socket.close()
            health.imap = "UP"
        } catch (Exception e) {
            health.imap = "DOWN"
            log.error("❌ IMAP health check failed: ${e.message}")
        }
        
        log.info("💓 Health Status: ${health}")
        exchange.in.body = health
    }
    .choice()
        .when(simple("${body[ollama]} == 'DOWN'"))
            .to("direct:handleOllamaDown")
        .when(simple("${body[smtp]} == 'DOWN'"))
            .to("direct:handleSmtpDown")
```

### Performance Monitoring

#### Response Time Tracking
```groovy
from("direct:trackResponseTime")
    .process { exchange ->
        def startTime = exchange.getProperty("startTime")
        def endTime = System.currentTimeMillis()
        def duration = endTime - startTime
        
        log.info("⏱️ Response time: ${duration}ms")
        
        // Alert jeśli zbyt wolno
        if (duration > 30000) {  // 30 sekund
            log.warn("🐌 Slow response detected: ${duration}ms")
            exchange.setHeader("slowResponse", true)
        }
    }
```

#### Memory Usage Monitor
```groovy
from("timer://memoryCheck?period=300000")  // Co 5 minut
    .process { exchange ->
        def runtime = Runtime.getRuntime()
        def totalMemory = runtime.totalMemory()
        def freeMemory = runtime.freeMemory()
        def usedMemory = totalMemory - freeMemory
        def maxMemory = runtime.maxMemory()
        
        def usedPercent = (usedMemory * 100) / maxMemory
        
        log.info("💾 Memory usage: ${usedPercent.round(1)}% (${usedMemory/1024/1024} MB / ${maxMemory/1024/1024} MB)")
        
        if (usedPercent > 85) {
            log.warn("⚠️ High memory usage: ${usedPercent}%")
            System.gc()  // Force garbage collection
        }
    }
```

---

## 🔧 Rozwiązywanie problemów

### Najczęstsze problemy i rozwiązania

#### 1. Ollama nie odpowiada

**Symptomy:**
```
❌ BŁĄD: Connection refused (Connection refused)
❌ Ollama health check failed: ConnectException
```

**Diagoza:**
```bash
# Sprawdź czy Ollama działa
curl http://localhost:11434/api/tags

# Sprawdź proces
ps aux | grep ollama

# Sprawdź port
netstat -tulpn | grep 11434
```

**Rozwiązania:**
```bash
# Uruchom Ollama
ollama serve

# Lub przez systemd
sudo systemctl start ollama

# Sprawdź logi
journalctl -u ollama -f

# Restart jeśli zawieszone
pkill ollama
ollama serve
```

#### 2. Model nie został pobrany

**Symptomy:**
```
❌ model 'gemma2:2b' not found
```

**Rozwiązanie:**
```bash
# Sprawdź dostępne modele
ollama list

# Pobierz model
ollama pull gemma2:2b

# Sprawdź czy pobrany
ollama show gemma2:2b
```

#### 3. Błędy SMTP/IMAP

**Symptomy:**
```
❌ Authentication failed
❌ Connection timed out
```

**Diagoza:**
```bash
# Test SMTP
telnet smtp.server.com 25
# lub
openssl s_client -connect smtp.server.com:465

# Test IMAP
telnet imap.server.com 143
# lub
openssl s_client -connect imap.server.com:993
```

**Rozwiązania:**
```env
# Sprawdź konfigurację w .env
SMTP_SERVER=correct-server.com
SMTP_PORT=587  # Spróbuj różnych portów: 25, 587, 465
SMTP_USERNAME=correct-username
SMTP_PASSWORD=correct-password

# Dla Gmail użyj App Passwords, nie głównego hasła
```

#### 4. Java/Groovy problemy

**Symptomy:**
```
Exception in thread "main" java.lang.ClassNotFoundException
NoSuchMethodError
```

**Rozwiązania:**
```bash
# Sprawdź wersje
java -version    # Potrzeba Java 17+
groovy --version # Potrzeba Groovy 4.0+

# Wyczyść cache Maven/Grape
rm -rf ~/.groovy/grapes
rm -rf ~/.m2/repository

# Reinstall dependencies
groovy -e "@Grab('org.apache.camel:camel-core:4.4.0') println 'OK'"
```

#### 5. Problemy z pamięcią

**Symptomy:**
```
OutOfMemoryError: Java heap space
System becomes unresponsive
```

**Rozwiązania:**
```bash
# Zwiększ pamięć dla Groovy
export JAVA_OPTS="-Xmx4g -Xms1g"
groovy email-automation-ollama.groovy

# Lub w skrypcie
JAVA_OPTS="-Xmx4g" groovy email-automation-ollama.groovy

# Użyj mniejszego modelu
OLLAMA_MODEL=tinyllama
```

### Debug Mode

#### Włączenie szczegółowego logowania
```bash
# Ustaw debug level
export LOG_LEVEL=DEBUG

# Groovy debug
groovy -Dgroovy.grape.report.downloads=true \
       -Djava.util.logging.level=FINE \
       email-automation-ollama.groovy
```

#### Camel Debug
```groovy
// Dodaj do main()
main.addConfiguration(new CamelConfiguration() {
    void configure(CamelContext context) {
        context.setTracing(true)  // Włącz tracing
        context.setMessageHistory(true)  // Historia wiadomości
    }
})
```

### Diagnostyka sieci

#### Test connectivity
```bash
# Test DNS resolution
nslookup smtp.server.com
nslookup imap.server.com

# Test ports
nc -zv smtp.server.com 587
nc -zv imap.server.com 993
nc -zv localhost 11434

# Test SSL certificates
openssl s_client -connect smtp.server.com:465 -servername smtp.server.com
```

#### Firewall checks
```bash
# Ubuntu/Debian
sudo ufw status
sudo iptables -L

# CentOS/RHEL  
sudo firewall-cmd --list-all
```

---

## 📚 API Reference

### Camel Components używane w projekcie

#### 1. Timer Component
```groovy
from("timer://name?period=30000&delay=5000")
```

**Parametry:**
- `period` - interwał w ms między wywołaniami
- `delay` - opóźnienie pierwszego wywołania  
- `repeatCount` - liczba powtórzeń (domyślnie nieskończoność)
- `fixedRate` - czy utrzymywać stały rytm

#### 2. IMAP Component
```groovy
from("imap://imap.server.com:993?username=user&password=pass&delete=false&unseen=true")
```

**Parametry:**
- `username/password` - dane logowania
- `delete` - czy usuwać wiadomości po przeczytaniu
- `unseen` - tylko nieprzeczytane
- `folderName` - folder do monitorowania
- `consumer.delay` - opóźnienie między sprawdzeniami
- `maxMessagesPerPoll` - max wiadomości na raz

#### 3. SMTP Component  
```groovy
to("smtp://smtp.server.com:587?username=user&password=pass")
```

**Parametry:**
- `username/password` - dane logowania
- `contentType` - typ zawartości
- `mail.smtp.starttls.enable` - włącz TLS
- `mail.smtp.ssl.enable` - włącz SSL

#### 4. HTTP Component
```groovy
to("http://localhost:11434/api/generate")
```

**Headers:**
- `Content-Type` - typ zawartości żądania
- `Authorization` - header autoryzacji
- `CamelHttpMethod` - metoda HTTP (GET, POST, etc.)

### Ollama API Reference

#### Generate Endpoint
```
POST /api/generate
```

**Request:**
```json
{
  "model": "gemma2:2b",
  "prompt": "Your prompt here",
  "stream": false,
  "format": "json",
  "options": {
    "temperature": 0.7,
    "top_p": 0.9,
    "top_k": 40,
    "max_tokens": 200
  }
}
```

**Response:**
```json
{
  "model": "gemma2:2b", 
  "created_at": "2024-01-15T10:30:00Z",
  "response": "Generated text response",
  "done": true,
  "total_duration": 1500000000,
  "eval_count": 100
}
```

#### Models Endpoint
```
GET /api/tags
```

**Response:**
```json
{
  "models": [
    {
      "name": "gemma2:2b",
      "modified_at": "2024-01-15T10:00:00Z", 
      "size": 2600000000
    }
  ]
}
```

### Exchange Headers Reference

#### Email Headers (IMAP)
```groovy
exchange.in.getHeader("from")        // Nadawca
exchange.in.getHeader("to")          // Odbiorca
exchange.in.getHeader("subject")     // Temat
exchange.in.getHeader("Date")        // Data wysłania
exchange.in.getHeader("Message-ID")  // Unikalny ID
```

#### Custom Headers  
```groovy
exchange.in.setHeader("originalSender", sender)
exchange.in.setHeader("originalSubject", subject)
exchange.in.setHeader("aiProcessed", true)
exchange.in.setHeader("responseTime", duration)
```

---

## 💡 Przykłady użycia

### Scenariusze biznesowe

#### 1. Obsługa sklepu internetowego
```groovy
// Klasyfikacja emaili sklepowych
def classifyEmail(subject, body) {
    def subjectLower = subject.toLowerCase()
    def bodyLower = body.toLowerCase()
    
    if (subjectLower.contains("zamówienie") || bodyLower.contains("numer zamówienia")) {
        return "ORDER_INQUIRY"
    } else if (subjectLower.contains("reklamacja") || bodyLower.contains("zwrot")) {
        return "COMPLAINT"
    } else if (subjectLower.contains("dostawa") || bodyLower.contains("kiedy otrzymam")) {
        return "SHIPPING"
    } else {
        return "GENERAL"
    }
}

// Różne prompty dla różnych typów
def getPromptForCategory(category, emailData) {
    switch(category) {
        case "ORDER_INQUIRY":
            return """
            Klient pyta o zamówienie:
            ${emailData.body}
            
            Odpowiedz profesjonalnie informując o:
            - Sprawdzeniu statusu zamówienia
            - Numerze śledzenia (jeśli dostępny)
            - Kontakcie do działu zamówień
            """
        case "COMPLAINT":
            return """
            Klient ma reklamację:
            ${emailData.body}
            
            Odpowiedz empatycznie i profesjonalnie:
            - Przeproś za niedogodności
            - Zaproponuj rozwiązanie
            - Podaj procedurę reklamacji
            """
        default:
            return "Standardowa profesjonalna odpowiedź na email klienta"
    }
}
```

#### 2. Wsparcie techniczne IT
```groovy
// Prompt specjalistyczny IT
def itSupportPrompt = """
Jesteś ekspertem IT odpowiadającym na email wsparcia technicznego:

Email: ${emailData.body}

Przeanalizuj problem i napisz odpowiedź zawierającą:
1. Zrozumienie problemu
2. Możliwe przyczyny  
3. Kroki rozwiązania
4. Alternatywne rozwiązania
5. Kontakt do działu IT jeśli potrzebny

Odpowiedź techniczna ale zrozumiała dla użytkownika.
"""
```

#### 3. Biuro nieruchomości
```groovy
// Obsługa zapytań o nieruchomości
def realEstatePrompt = """
Email od potencjalnego klienta nieruchomości:
${emailData.body}

Odpowiedz jako doradca nieruchomości:
- Podziękuj za zainteresowanie
- Odpowiedz na pytania o nieruchomość
- Zaproponuj oględziny
- Podaj kontakt do doradcy
- Zachęć do dalszego kontaktu

Ton profesjonalny ale ciepły i zachęcający.
"""
```

### Zaawansowane konfiguracje

#### Multi-tenant setup
```groovy
// Różne konfiguracje dla różnych firm
def configs = [
    "company1": [
        smtp_server: "smtp.company1.com",
        model: "gemma2:2b",
        prompt_style: "formal"
    ],
    "company2": [
        smtp_server: "smtp.company2.com", 
        model: "llama3.2:1b",
        prompt_style: "casual"
    ]
]

from("imap://...")
    .process { exchange ->
        def recipient = exchange.in.getHeader("to")
        def company = extractCompany(recipient)
        def config = configs[company]
        
        exchange.setProperty("companyConfig", config)
    }
    .to("direct:processWithCompanyConfig")
```

#### Load balancing między modelami
```groovy
def models = ["gemma2:2b", "llama3.2:1b", "qwen2.5:1.5b"]
def currentModel = 0

from("direct:processWithLB")
    .process { exchange ->
        // Round-robin load balancing
        def model = models[currentModel % models.size()]
        currentModel++
        
        exchange.setHeader("selectedModel", model)
        log.info("🔄 Using model: ${model}")
    }
    .recipientList(simple("http://localhost:11434/api/generate"))
```

#### Fallback chain
```groovy
from("direct:processWithFallback")
    .doTry()
        .to("http://localhost:11434/api/generate")  // Primary Ollama
    .doCatch(Exception.class)
        .log("Primary failed, trying secondary...")
        .doTry()
            .to("http://localhost:11435/api/generate")  // Secondary Ollama
        .doCatch(Exception.class)
            .log("All AI failed, using template response")
            .to("direct:generateTemplateResponse")
    .end()
```

### Custom processors

#### Email sanitizer
```groovy
class EmailSanitizerProcessor implements Processor {
    void process(Exchange exchange) throws Exception {
        def body = exchange.in.body.toString()
        
        // Usuń potencjalnie niebezpieczne treści
        body = body.replaceAll(/(?i)(password|hasło):\s*\S+/, "[REDACTED]")
        body = body.replaceAll(/\b\d{4}\s*\d{4}\s*\d{4}\s*\d{4}\b/, "[CARD_NUMBER_REDACTED]")
        body = body.replaceAll(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/, "[EMAIL_REDACTED]")
        
        // Ograniczenie długości
        if (body.length() > 2000) {
            body = body.substring(0, 2000) + "... [TRUNCATED]"
        }
        
        exchange.in.body = body
        log.debug("📧 Email sanitized")
    }
}
```

#### Response validator
```groovy
class ResponseValidatorProcessor implements Processor {
    void process(Exchange exchange) throws Exception {
        def response = exchange.in.body.toString()
        
        // Sprawdź czy odpowiedź ma sens
        if (response.length() < 20) {
            throw new Exception("Response too short")
        }
        
        if (response.toLowerCase().contains("error") || 
            response.toLowerCase().contains("failed")) {
            throw new Exception("AI response contains error indicators")
        }
        
        // Sprawdź czy zawiera podstawowe elementy uprzejmości
        def politeWords = ["dziękuję", "pozdrawienia", "dzień dobry", "miłego dnia"]
        def isPolite = politeWords.any { word -> 
            response.toLowerCase().contains(word) 
        }
        
        if (!isPolite) {
            // Dodaj uprzejme zakończenie
            response += "\n\nPozdrawienia,\nZespół obsługi klienta"
            exchange.in.body = response
        }
        
        log.debug("✅ Response validated and enhanced")
    }
}
```

### Integration patterns

#### Content-based router
```groovy
from("direct:routeByContent")
    .choice()
        .when(header("subject").contains("URGENT"))
            .to("direct:urgentProcessing")
        .when(header("subject").contains("INVOICE"))
            .to("direct:invoiceProcessing")
        .when(body().contains("unsubscribe"))
            .to("direct:unsubscribeProcessing")
        .otherwise()
            .to("direct:standardProcessing")
```

#### Message translator  
```groovy
from("direct:translateMessage")
    .process { exchange ->
        def originalBody = exchange.in.body.toString()
        
        // Tłumaczenie przez Ollama
        def translatePrompt = """
        Przetłumacz następujący tekst na język polski:
        
        ${originalBody}
        
        Zachowaj znaczenie i ton oryginalnej wiadomości.
        """
        
        exchange.in.body = buildOllamaPayload(translatePrompt)
    }
    .to("http://localhost:11434/api/generate")
    .process { exchange ->
        def translatedText = parseOllamaResponse(exchange)
        exchange.in.body = translatedText
    }
```

#### Aggregator pattern
```groovy
from("direct:aggregateEmails")
    .aggregate(header("customerEmail"))
    .aggregationStrategy(new EmailAggregationStrategy())
    .completionSize(5)  // Grupuj po 5 emaili
    .completionTimeout(300000)  // Lub po 5 minutach
    .to("direct:processBatchEmails")

class EmailAggregationStrategy implements AggregationStrategy {
    Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            return newExchange
        }
        
        def oldBody = oldExchange.in.body
        def newBody = newExchange.in.body
        
        // Połącz emaile w jeden batch
        oldExchange.in.body = [oldBody, newBody].flatten()
        return oldExchange
    }
}
```

---

## 🔐 Bezpieczeństwo i deployment

### Security best practices

#### Credentials management
```bash
# Nie przechowuj haseł w .env w plaintext
# Użyj secrets management

# Przykład z HashiCorp Vault
export VAULT_ADDR="https://vault.company.com"
vault kv get -field=smtp_password secret/email-automation/smtp

# Przykład z AWS Secrets Manager
aws secretsmanager get-secret-value --secret-id email-automation/smtp \
    --query SecretString --output text
```

#### Environment isolation
```bash
# Development
cp .env.development .env

# Staging  
cp .env.staging .env

# Production
cp .env.production .env
```

#### Network security
```groovy
// Whitelist dozwolonych serwerów
def allowedHosts = ["smtp.company.com", "imap.company.com", "localhost"]

from("direct:validateHost")
    .process { exchange ->
        def host = exchange.in.getHeader("host")
        if (!allowedHosts.contains(host)) {
            throw new SecurityException("Host ${host} not allowed")
        }
    }
```

### Production deployment

#### Docker production setup
```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  email-automation:
    image: your-registry/email-automation:latest
    restart: unless-stopped
    environment:
      - JAVA_OPTS=-Xmx2g -XX:+UseG1GC
    secrets:
      - smtp_password
      - imap_password
    deploy:
      resources:
        limits:
          memory: 4G
          cpus: '2'
        reservations:
          memory: 1G
          cpus: '0.5'
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3

secrets:
  smtp_password:
    external: true
  imap_password:
    external: true
```

#### Kubernetes deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: email-automation
spec:
  replicas: 2
  selector:
    matchLabels:
      app: email-automation
  template:
    metadata:
      labels:
        app: email-automation
    spec:
      containers:
      - name: email-automation
        image: your-registry/email-automation:latest
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "4Gi" 
            cpu: "2"
        env:
        - name: SMTP_PASSWORD
          valueFrom:
            secretKeyRef:
              name: email-secrets
              key: smtp-password
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 30
```

---

## 📈 Skalowanie i optymalizacja

### Performance tuning

#### JVM optimization
```bash
# Production JVM settings
export JAVA_OPTS="
  -Xmx4g
  -Xms1g
  -XX:+UseG1GC
  -XX:G1HeapRegionSize=16m
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
  -Djava.security.egd=file:/dev/./urandom
"
```

#### Camel optimization
```groovy
// Thread pool tuning
main.addConfiguration(new CamelConfiguration() {
    void configure(CamelContext context) {
        // Custom thread pool dla route'ów
        def threadPoolProfile = new ThreadPoolProfileBuilder("emailProcessing")
            .poolSize(5)
            .maxPoolSize(20)
            .maxQueueSize(100)
            .keepAliveTime(60, TimeUnit.SECONDS)
            .build()
            
        context.getExecutorServiceManager()
               .addThreadPoolProfile(threadPoolProfile)
    }
})
```

#### Ollama optimization
```bash
# Ollama environment variables dla performance
export OLLAMA_NUM_PARALLEL=4        # Równoległe requesty
export OLLAMA_MAX_LOADED_MODELS=2   # Max modeli w pamięci
export OLLAMA_FLASH_ATTENTION=1     # Flash attention optimization
export OLLAMA_LLM_LIBRARY=cuda      # Użyj GPU jeśli dostępne
```

### Horizontal scaling

#### Load balancer setup
```groovy
// Multiple Ollama instances
def ollamaInstances = [
    "http://ollama1:11434/api/generate",
    "http://ollama2:11434/api/generate", 
    "http://ollama3:11434/api/generate"
]

from("direct:loadBalancedAI")
    .loadBalance()
        .roundRobin()
        .to(ollamaInstances.join(","))
```

#### Database integration dla state
```groovy
// Shared state w Redis
from("direct:processEmail")
    .process { exchange ->
        def emailId = exchange.in.getHeader("Message-ID")
        
        // Sprawdź czy już przetwarzane
        def redis = new Jedis("redis://localhost:6379")
        if (redis.exists("processing:${emailId}")) {
            exchange.setProperty("skip", true)
        } else {
            redis.setex("processing:${emailId}", 300, "true")  // 5 min TTL
        }
        redis.close()
    }
    .choice()
        .when(exchangeProperty("skip").isEqualTo(true))
            .log("Email już w trakcie przetwarzania")
        .otherwise()
            .to("direct:processWithAI")
```

---

## 📋 Podsumowanie

### Kluczowe zalety systemu

1. **🚀 Prostota** - tylko 40 linii kodu Groovy
2. **💰 Zero kosztów** - lokalne AI bez opłat za API  
3. **🔒 Prywatność** - wszystkie dane pozostają lokalnie
4. **⚡ Enterprise-grade** - Apache Camel zapewnia niezawodność
5. **🔧 Konfigurowalność** - wszystko przez .env
6. **📊 Monitoring** - wbudowane metryki i health checks
7. **🔄 Skalowność** - łatwe skalowanie poziome
8. **🛡️ Bezpieczeństwo** - best practices dla produkcji

### Przypadki użycia

- **E-commerce** - automatyczne odpowiedzi na pytania klientów
- **IT Support** - pierwsza linia wsparcia technicznego  
- **Biura obsługi** - routing i wstępne odpowiedzi
- **Małe firmy** - kompleksowa obsługa emaili
- **Agencje** - obsługa wielu klientów jednocześnie

### Roadmap rozwoju

#### Planowane funkcje v2.0
- **Multi-model support** - różne modele dla różnych zadań
- **Web UI** - interfejs graficzny do zarządzania
- **Advanced analytics** - szczegółowe raporty i metryki
- **Integration plugins** - CRM, ticketing systems
- **Multi-language** - automatyczne rozpoznawanie języka
- **Template engine** - zaawansowane szablony odpowiedzi

Ten system to kompletne rozwiązanie enterprise email automation wykorzystujące najnowsze technologie AI w lokalnym, bezpiecznym środowisku. 
Połączenie Apache Camel z Ollama zapewnia niezawodność, skalowalność i prywatność przy minimalnej złożoności kodu.



# Lokalna instalacja  
```bash
./install.sh
```

```bash
groovy email.groovy
```

# Docker
```bash
docker-compose --profile camel up -d
```