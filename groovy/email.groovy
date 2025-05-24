@Grab('org.apache.camel:camel-core:4.4.0')
@Grab('org.apache.camel:camel-mail:4.4.0')
@Grab('org.apache.camel:camel-http:4.4.0')
@Grab('org.apache.camel:camel-jackson:4.4.0')
@Grab('org.apache.camel:camel-main:4.4.0')
@Grab('org.slf4j:slf4j-simple:2.0.9')

import org.apache.camel.main.Main
import org.apache.camel.builder.RouteBuilder
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

// Ładowanie konfiguracji z .env
def loadEnvConfig() {
    def config = [:]
    def envFile = new File('.env')

    if (!envFile.exists()) {
        println "❌ Brak pliku .env - tworzę przykładowy"
        createSampleEnv()
        return loadDefaultConfig()
    }

    envFile.eachLine { line ->
        if (line && !line.startsWith('#') && line.contains('=')) {
            def parts = line.split('=', 2)
            if (parts.length == 2) {
                config[parts[0].trim()] = parts[1].trim()
            }
        }
    }
    return config
}

def createSampleEnv() {
    new File('.env').text = '''# Email Processing Configuration
SMTP_SERVER=localhost
SMTP_PORT=1025
SMTP_USERNAME=test@example.com
SMTP_PASSWORD=password
FROM_EMAIL=test@example.com
REPLY_TO_EMAIL=support@example.com

IMAP_SERVER=localhost
IMAP_PORT=1025
IMAP_USERNAME=test@example.com
IMAP_PASSWORD=password
IMAP_FOLDER=INBOX

MOCK_EMAILS=true
EMAIL_LIMIT=3
CHECK_INTERVAL_SECONDS=60
TEST_EMAIL=info@example.com

OLLAMA_HOST=localhost
OLLAMA_PORT=11434
OLLAMA_MODEL=qwen2.5:1.5b
'''
}

def loadDefaultConfig() {
    return [
        'SMTP_SERVER': 'localhost',
        'SMTP_PORT': '1025',
        'SMTP_USERNAME': 'test@example.com',
        'SMTP_PASSWORD': 'password',
        'FROM_EMAIL': 'test@example.com',
        'REPLY_TO_EMAIL': 'support@example.com',
        'IMAP_SERVER': 'localhost',
        'IMAP_PORT': '1025',
        'IMAP_USERNAME': 'test@example.com',
        'IMAP_PASSWORD': 'password',
        'IMAP_FOLDER': 'INBOX',
        'MOCK_EMAILS': 'true',
        'EMAIL_LIMIT': '3',
        'CHECK_INTERVAL_SECONDS': '60',
        'TEST_EMAIL': 'info@example.com',
        'OLLAMA_HOST': 'localhost',
        'OLLAMA_PORT': '11434',
        'OLLAMA_MODEL': 'qwen2.5:1.5b'
    ]
}

def config = loadEnvConfig()

// Konfiguracja Ollama
def OLLAMA_HOST = config['OLLAMA_HOST'] ?: 'localhost'
def OLLAMA_PORT = config['OLLAMA_PORT'] ?: '11434'
def OLLAMA_MODEL = config['OLLAMA_MODEL'] ?: 'qwen2.5:1.5b'
def OLLAMA_URL = "http://${OLLAMA_HOST}:${OLLAMA_PORT}/api/generate"

println """
🚀 CAMEL + OLLAMA EMAIL AUTOMATION (FIXED)
==========================================
📧 SMTP: ${config['SMTP_SERVER']}:${config['SMTP_PORT']}
📨 IMAP: ${config['IMAP_SERVER']}:${config['IMAP_PORT']}
👤 User: ${config['SMTP_USERNAME']}
🤖 Ollama: ${OLLAMA_URL}
📦 Model: ${OLLAMA_MODEL}
🔄 Interval: ${config['CHECK_INTERVAL_SECONDS']}s
📊 Limit: ${config['EMAIL_LIMIT']}
🧪 Mock: ${config['MOCK_EMAILS']}

Upewnij się że Ollama działa: curl ${OLLAMA_URL}
Naciśnij Ctrl+C aby zatrzymać...
"""

// Test Ollama connection
def testOllama() {
    try {
        // First test basic connectivity
        def healthUrl = "http://${OLLAMA_HOST}:${OLLAMA_PORT}/api/health"
        def healthConn = new URL(healthUrl).openConnection() as HttpURLConnection
        healthConn.requestMethod = 'GET'
        
        if (healthConn.responseCode != 200) {
            throw new Exception("Ollama health check failed: ${healthConn.responseCode}")
        }
        
        // Then test if model is available
        def modelUrl = "http://${OLLAMA_HOST}:${OLLAMA_PORT}/api/tags"
        def modelConn = new URL(modelUrl).openConnection() as HttpURLConnection
        modelConn.requestMethod = 'GET'
        
        def response = new groovy.json.JsonSlurper().parseText(modelConn.content.text)
        def models = response.models?.collect { it.name } ?: []
        
        if (!models.any { it ==~ /.*${OLLAMA_MODEL}.*/ }) {
            println "⚠️  Model '${OLLAMA_MODEL}' not found. Available models: ${models.join(', ')}"
            println "    You may need to run: ollama pull ${OLLAMA_MODEL}"
            return false
        }
        
        println "✅ Ollama is running and model '${OLLAMA_MODEL}' is available"
        return true
        
    } catch (Exception e) {
        println "❌ Ollama connection failed: ${e.message}"
        println "   Make sure Ollama is running: ollama serve"
        println "   Check if the model is downloaded: ollama pull ${OLLAMA_MODEL}"
        return false
    }
}

// Funkcja generowania standardowej odpowiedzi
def generateStandardResponse(String sender, String subject, String body) {
    def senderName = sender.split('@')[0].replace('.', ' ')
        .split(' ').collect { it.capitalize() }.join(' ')

    def subjectLower = subject.toLowerCase()
    def bodyLower = body.toLowerCase()

    if (subjectLower.contains('pytanie') || bodyLower.contains('pytanie')) {
        return """Dzień dobry ${senderName},

Dziękuję za Twoje pytanie. Nasz zespół przeanalizuje Twoją sprawę i skontaktuje się z Tobą w ciągu 24 godzin.

Pozdrawienia,
Zespół obsługi klienta"""
    } else if (subjectLower.contains('reklamacja') || bodyLower.contains('reklamacja')) {
        return """Dzień dobry ${senderName},

Przepraszamy za niedogodności. Twoja reklamacja została przekazana do odpowiedniego działu. Skontaktujemy się z Tobą w ciągu 48 godzin.

Pozdrawienia,
Zespół obsługi klienta"""
    } else {
        return """Dzień dobry ${senderName},

Dziękuję za Twój email. Twoja wiadomość została otrzymana i zostanie przetworzona w kolejności wpływu.

Pozdrawienia,
Zespół obsługi klienta"""
    }
}

// Test Ollama before starting
if (!testOllama()) {
    println "\n⚠️ Ollama is not available. The system will continue but AI features will be disabled."
    println "   You can still use the system with mock responses."
    config['MOCK_EMAILS'] = 'true'
    config['SEND_TEST_EMAILS'] = 'false'
}

// Camel Main context
Main main = new Main()

// RouteBuilder class - poprawna składnia dla Camel 4.4.0
class EmailRouteBuilder extends RouteBuilder {
    def config

    EmailRouteBuilder(config) {
        this.config = config
    }

    void configure() {

        // Obsługa błędów
        onException(Exception.class)
            .log("❌ BŁĄD: \${exception.message}")
            .handled(true)
            .to("direct:handleError")

        // MOCK EMAILS lub RZECZYWISTE
        if (config['MOCK_EMAILS'] == 'true') {
            from("timer://mockTimer?period=${config['CHECK_INTERVAL_SECONDS']}000&delay=5000")
                .routeId("mock-email-generator")
                .log("🧪 Generowanie mock email...")
                .process { ex ->
                    def mockEmails = [
                        [from: "jan.kowalski@example.com", subject: "Pytanie o produkt",
                         body: "Dzień dobry, interesuje mnie Państwa produkt XYZ. Proszę o informacje o cenie i dostępności."],
                        [from: "anna.nowak@firma.pl", subject: "Reklamacja zamówienia",
                         body: "Otrzymałam wadliwy produkt w zamówieniu nr 12345. Proszę o kontakt w sprawie zwrotu."],
                        [from: config['TEST_EMAIL'], subject: "Test automatyzacji",
                         body: "To jest testowy email do sprawdzenia systemu automatyzacji odpowiedzi."]
                    ]
                    def randomEmail = mockEmails[new Random().nextInt(mockEmails.size())]
                    ex.in.setHeader("from", randomEmail.from)
                    ex.in.setHeader("subject", randomEmail.subject)
                    ex.in.body = randomEmail.body
                }
                .to("direct:processWithOllama")
        } else {
            // Budowanie URL IMAP z .env
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
                .log("📧 Email od: \${header.from}")
                .to("direct:processWithOllama")
        }

        // Przetwarzanie przez Ollama
        from("direct:processWithOllama")
            .routeId("ollama-processor")
            .log("🤖 Przetwarzanie przez Ollama...")
            .process { ex ->
                def emailBody = ex.in.body?.toString() ?: ""
                def sender = ex.in.getHeader("from")?.toString() ?: "unknown"
                def subject = ex.in.getHeader("subject")?.toString() ?: "no subject"

                // Przechowanie oryginalnych danych
                ex.in.setHeader("originalSender", sender)
                ex.in.setHeader("originalSubject", subject)

                // Prompt dla Ollama
                def prompt = """Otrzymałeś email od klienta:

Od: ${sender}
Temat: ${subject}
Treść: ${emailBody}

Napisz profesjonalną, pomocną odpowiedź w języku polskim. Odpowiedź powinna być:
- Uprzejma i profesjonalna
- Konkretna i adresująca problem klienta
- Nie dłuższa niż 150 słów
- Zakończona podpisem "Pozdrawienia, Zespół obsługi klienta"

Odpowiedź:"""

                // Payload dla Ollama API
                def ollamaPayload = new JsonBuilder([
                    model: config['OLLAMA_MODEL'],
                    prompt: prompt,
                    stream: false,
                    options: [
                        temperature: 0.7,
                        max_tokens: 200
                    ]
                ])

                ex.in.setHeader("Content-Type", "application/json")
                ex.in.body = ollamaPayload.toString()
            }
            .doTry()
                .to("http://${config['OLLAMA_HOST']}:${config['OLLAMA_PORT']}/api/generate")
                .process { ex ->
                    try {
                        // Parsowanie odpowiedzi Ollama
                        def jsonSlurper = new JsonSlurper()
                        def response = jsonSlurper.parseText(ex.in.body.toString())
                        def aiResponse = response.response?.trim()

                        if (aiResponse && aiResponse.length() > 10) {
                            ex.in.body = aiResponse
                            log.info("✅ Odpowiedź Ollama: ${aiResponse.take(60)}...")
                        } else {
                            throw new Exception("Pusta lub zbyt krótka odpowiedź z Ollama")
                        }
                    } catch (Exception e) {
                        log.error("❌ Błąd parsowania Ollama: ${e.message}")
                        throw e
                    }
                }
            .doCatch(Exception.class)
                .log("🔄 Ollama failed, using standard response")
                .process { ex ->
                    def sender = ex.in.getHeader("originalSender")
                    def subject = ex.in.getHeader("originalSubject")
                    def body = ex.in.getHeader("originalBody")?.toString() ?: ""
                    def standardResponse = generateStandardResponse(sender, subject, body)
                    ex.in.body = standardResponse
                }
            .end()
            .to("direct:sendReply")

        // Wysyłanie odpowiedzi
        from("direct:sendReply")
            .routeId("email-sender")
            .log("📤 Wysyłanie do: \${header.originalSender}")
            .setHeader("To", simple("\${header.originalSender}"))
            .setHeader("Subject", simple("Re: \${header.originalSubject}"))
            .setHeader("From", simple("${config['FROM_EMAIL']}"))
            .setHeader("Reply-To", simple("${config['REPLY_TO_EMAIL']}"))
            .choice()
                .when(simple("${config['MOCK_EMAILS']} == 'true'"))
                    .log("📧 MOCK: Email wysłany do \${header.To}")
                    .log("📧 MOCK: Temat: \${header.Subject}")
                    .log("📧 MOCK: Treść: \${body}")
                .otherwise()
                    .process { ex ->
                        // Budowanie URL SMTP z .env
                        def smtpUrl = ""
                        if (config['SMTP_PORT'] == '465') {
                            smtpUrl = "smtps://${config['SMTP_SERVER']}:${config['SMTP_PORT']}"
                        } else if (config['SMTP_PORT'] == '587') {
                            smtpUrl = "smtp://${config['SMTP_SERVER']}:${config['SMTP_PORT']}?mail.smtp.starttls.enable=true"
                        } else {
                            smtpUrl = "smtp://${config['SMTP_SERVER']}:${config['SMTP_PORT']}"
                        }
                        ex.in.setHeader("smtpUrl", smtpUrl)
                    }
                    .recipientList(simple("\${header.smtpUrl}?" +
                        "username=${config['SMTP_USERNAME']}&" +
                        "password=${config['SMTP_PASSWORD']}"))
            .end()
            .log("✅ Odpowiedź wysłana!")

        // Obsługa błędów
        from("direct:handleError")
            .routeId("error-handler")
            .log("⚠️ Wysyłanie powiadomienia o błędzie")
            .setBody(constant("Wystąpił błąd w systemie automatyzacji emaili. Sprawdź logi systemu."))
            .setHeader("To", simple("${config['FROM_EMAIL']}"))
            .setHeader("Subject", constant("BŁĄD: Email Automation System"))
            .setHeader("From", simple("${config['FROM_EMAIL']}"))
            .choice()
                .when(simple("${config['MOCK_EMAILS']} == 'true'"))
                    .log("📧 MOCK ERROR EMAIL: \${body}")
                .otherwise()
                    .process { ex ->
                        def smtpUrl = config['SMTP_PORT'] == '465' ?
                            "smtps://${config['SMTP_SERVER']}:${config['SMTP_PORT']}" :
                            "smtp://${config['SMTP_SERVER']}:${config['SMTP_PORT']}"
                        ex.in.setHeader("smtpUrl", smtpUrl)
                    }
                    .recipientList(simple("\${header.smtpUrl}?" +
                        "username=${config['SMTP_USERNAME']}&" +
                        "password=${config['SMTP_PASSWORD']}"))
            .end()
    }
}

// Dodanie RouteBuilder do Main - poprawna składnia
main.configure().routeBuilder(new EmailRouteBuilder(config))

// Uruchomienie
try {
    main.run()
} catch (Exception e) {
    println "❌ Błąd uruchomienia: ${e.message}"
    e.printStackTrace()
}