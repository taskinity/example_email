@Grab('org.apache.camel:camel-core:4.4.0')
@Grab('org.apache.camel:camel-main:4.4.0')
@Grab('org.apache.camel:camel-http:4.4.0')
@Grab('org.apache.camel:camel-mail:4.4.0')
@Grab('org.slf4j:slf4j-simple:2.0.9')

import org.apache.camel.main.Main
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.CamelContext
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

// === KONFIGURACJA ===
def loadConfig() {
    def config = [:]
    def envFile = new File('.env')

    if (envFile.exists()) {
        envFile.eachLine { line ->
            if (line && !line.startsWith('#') && line.contains('=')) {
                def parts = line.split('=', 2)
                if (parts.length == 2) {
                    config[parts[0].trim()] = parts[1].trim()
                }
            }
        }
    }

    // Domyślne wartości
    config.putIfAbsent('MOCK_EMAILS', 'true')
    config.putIfAbsent('CHECK_INTERVAL_SECONDS', '10')
    config.putIfAbsent('EMAIL_LIMIT', '3')
    config.putIfAbsent('OLLAMA_HOST', 'localhost')
    config.putIfAbsent('OLLAMA_PORT', '11434')
    config.putIfAbsent('OLLAMA_MODEL', 'qwen2.5:1.5b')
    config.putIfAbsent('TEST_EMAIL', 'info@softreck.com')

    return config
}

def config = loadConfig()

println """
🚀 CAMEL 4.4.0 EMAIL PROCESSOR (WORKING VERSION)
================================================
🤖 Ollama: http://${config['OLLAMA_HOST']}:${config['OLLAMA_PORT']}
📦 Model: ${config['OLLAMA_MODEL']}
🔄 Interval: ${config['CHECK_INTERVAL_SECONDS']}s
📊 Limit: ${config['EMAIL_LIMIT']}
🧪 Mock: ${config['MOCK_EMAILS']}

Testing Ollama connection...
"""

// === TEST OLLAMA ===
def testOllama() {
    try {
        def url = "http://${config['OLLAMA_HOST']}:${config['OLLAMA_PORT']}/api/tags"
        def connection = new URL(url).openConnection()
        connection.setConnectTimeout(5000)
        def response = connection.inputStream.text
        println "✅ Ollama connected successfully"
        return true
    } catch (Exception e) {
        println "❌ Ollama connection failed: ${e.message}"
        return false
    }
}

def ollamaAvailable = testOllama()

// === GLOBALNE ZMIENNE ===
def emailCount = 0
def maxEmails = Integer.parseInt(config['EMAIL_LIMIT'])

// === FUNKCJE POMOCNICZE ===
def generateMockEmail() {
    def emails = [
        [from: "jan.kowalski@example.com", subject: "Pytanie o produkt",
         body: "Dzień dobry, interesuje mnie Państwa oferta produktów."],
        [from: "anna.nowak@firma.pl", subject: "Reklamacja zamówienia",
         body: "Mam problem z zamówieniem nr 12345. Proszę o kontakt."],
        [from: "admin@urgent.com", subject: "PILNE - Problem z systemem",
         body: "System nie działa od godziny. Potrzebujemy pomocy!"],
        [from: config['TEST_EMAIL'], subject: "Test automatyzacji",
         body: "To jest testowy email sprawdzający automatyzację."]
    ]
    return emails[new Random().nextInt(emails.size())]
}

def generateStandardResponse(sender, subject, body) {
    def name = sender.split('@')[0].replace('.', ' ').split(' ').collect { it.capitalize() }.join(' ')

    if (subject.toLowerCase().contains('pilne') || body.toLowerCase().contains('pilne')) {
        return """Dzień dobry ${name},

Otrzymaliśmy Państwa pilną wiadomość. Nasz zespół techniczny został powiadomiony i skontaktuje się z Państwem w ciągu 2 godzin.

W razie dalszych pytań prosimy o kontakt.

Pozdrawienia,
Zespół obsługi klienta"""
    } else if (subject.toLowerCase().contains('reklamacja')) {
        return """Dzień dobry ${name},

Dziękujemy za zgłoszenie reklamacji. Przekazaliśmy sprawę do odpowiedniego działu, który skontaktuje się z Państwem w ciągu 48 godzin.

Przepraszamy za niedogodności.

Pozdrawienia,
Zespół obsługi klienta"""
    } else {
        return """Dzień dobry ${name},

Dziękujemy za Państwa wiadomość. Otrzymaliśmy ją i przetworzymy w kolejności wpływu.

W razie pytań jesteśmy do dyspozycji.

Pozdrawienia,
Zespół obsługi klienta"""
    }
}

def processWithOllama(emailData) {
    if (!ollamaAvailable) {
        return generateStandardResponse(emailData.from, emailData.subject, emailData.body)
    }

    try {
        def prompt = """Otrzymałeś email od klienta:

Od: ${emailData.from}
Temat: ${emailData.subject}
Treść: ${emailData.body}

Napisz profesjonalną odpowiedź w języku polskim (max 150 słów):"""

        def payload = new JsonBuilder([
            model: config['OLLAMA_MODEL'],
            prompt: prompt,
            stream: false,
            options: [temperature: 0.7, max_tokens: 200]
        ])

        def connection = new URL("http://${config['OLLAMA_HOST']}:${config['OLLAMA_PORT']}/api/generate").openConnection()
        connection.requestMethod = 'POST'
        connection.setRequestProperty('Content-Type', 'application/json')
        connection.doOutput = true
        connection.outputStream.write(payload.toString().bytes)

        def response = new JsonSlurper().parseText(connection.inputStream.text)
        def aiResponse = response.response?.trim()

        if (aiResponse && aiResponse.length() > 20) {
            println "✅ Ollama response: ${aiResponse.take(50)}..."
            return aiResponse
        } else {
            throw new Exception("Empty or too short response")
        }

    } catch (Exception e) {
        println "❌ Ollama failed: ${e.message} - using standard response"
        return generateStandardResponse(emailData.from, emailData.subject, emailData.body)
    }
}

// === CAMEL APPLICATION - POPRAWIONA SKŁADNIA ===
println "🚀 Starting Camel context..."

try {
    // KLUCZOWA ZMIANA: Tworzenie Main bez addRouteBuilder
    Main main = new Main()

    // POPRAWNA SKŁADNIA dla Camel 4.4.0
    main.configure().withRoutesBuilder(new RouteBuilder() {
        @Override
        void configure() throws Exception {

            // Error handling
            onException(Exception.class)
                .log("❌ ERROR: \${exception.message}")
                .handled(true)

            // Main processing timer
            def intervalMs = Integer.parseInt(config['CHECK_INTERVAL_SECONDS']) * 1000

            from("timer://emailProcessor?period=${intervalMs}&delay=3000")
                .routeId("email-processor")
                .log("🔄 Timer triggered...")
                .process { exchange ->
                    if (emailCount >= maxEmails) {
                        log.info("🎯 Reached limit: ${emailCount}/${maxEmails}")
                        exchange.setProperty("completed", true)
                        return
                    }

                    // Generate mock email
                    def emailData = generateMockEmail()
                    emailCount++

                    log.info("📧 Processing email ${emailCount}/${maxEmails}: ${emailData.subject}")

                    // Process with AI
                    def response = processWithOllama(emailData)

                    // Set exchange data
                    exchange.in.setHeader("originalFrom", emailData.from)
                    exchange.in.setHeader("originalSubject", emailData.subject)
                    exchange.in.setHeader("to", emailData.from)
                    exchange.in.setHeader("subject", "Re: ${emailData.subject}")
                    exchange.in.body = response
                }
                .choice()
                    .when(exchangeProperty("completed").isEqualTo(true))
                        .log("✅ Processing completed!")
                    .otherwise()
                        .to("direct:sendEmail")
                .end()

            // Email sending (mock)
            from("direct:sendEmail")
                .routeId("email-sender")
                .log("📤 Sending email to: \${header.originalFrom}")
                .log("📧 === MOCK EMAIL SENT ===")
                .log("📬 To: \${header.to}")
                .log("📝 Subject: \${header.subject}")
                .log("📄 Body: \${bodyAs(String).substring(0, java.lang.Math.min(120, bodyAs(String).length()))}...")
                .log("✅ Email sent successfully!")

            // Progress monitor
            from("timer://progress?period=30000&delay=10000")
                .routeId("progress-monitor")
                .process { exchange ->
                    def progress = maxEmails > 0 ? (emailCount * 100 / maxEmails) : 0
                    log.info("📊 Progress: ${emailCount}/${maxEmails} emails (${progress}%)")
                }
        }
    })

    println "✅ Routes configured successfully"
    println "🔄 Starting email processing..."
    println "   Expected: 1 email every ${config['CHECK_INTERVAL_SECONDS']} seconds"
    println "   Total: ${config['EMAIL_LIMIT']} emails"
    println ""

    // Start the application
    main.run()

} catch (Exception e) {
    println "❌ CRITICAL ERROR: ${e.message}"
    println "📋 Stack trace:"
    e.printStackTrace()

    println """

🆘 FALLBACK SOLUTION:
====================
If Camel continues to fail, try the pure Groovy version:
"""

    // FALLBACK - Pure Groovy version
    runPureGroovyVersion()
}

// === PURE GROOVY FALLBACK ===
def runPureGroovyVersion() {
    println """
🔄 PURE GROOVY EMAIL PROCESSOR
==============================
Running without Apache Camel...
"""

    def count = Integer.parseInt(config['EMAIL_LIMIT'])
    def interval = Integer.parseInt(config['CHECK_INTERVAL_SECONDS']) * 1000

    for (int i = 1; i <= count; i++) {
        def emailData = generateMockEmail()
        def timestamp = new Date().format("HH:mm:ss")

        println """
📧 EMAIL #${i}/${count} [${timestamp}]
══════════════════════════════════════
From: ${emailData.from}
Subject: ${emailData.subject}
Body: ${emailData.body}

🤖 Processing with ${ollamaAvailable ? 'Ollama AI' : 'Standard Response'}...
"""

        def response = processWithOllama(emailData)

        println """📤 RESPONSE GENERATED:
${response}

✅ Email #${i} processed successfully!
══════════════════════════════════════
"""

        if (i < count) {
            println "⏱️ Waiting ${config['CHECK_INTERVAL_SECONDS']} seconds...\n"
            Thread.sleep(interval)
        }
    }

    println """
🎯 PROCESSING COMPLETED!
========================
📊 Processed: ${count} emails
🤖 AI: ${ollamaAvailable ? 'Ollama' : 'Standard responses'}
⏰ Total time: ${count * Integer.parseInt(config['CHECK_INTERVAL_SECONDS'])} seconds
"""
}