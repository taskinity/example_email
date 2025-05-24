@GrabResolver(name='apache.snapshots', root='https://repository.apache.org/snapshots/')
@Grab('org.apache.camel:camel-core:4.4.0')
@Grab('org.apache.camel:camel-main:4.4.0')
@Grab('org.apache.camel:camel-mail:4.4.0')
@Grab('org.apache.camel:camel-http:4.4.0')
@Grab('org.apache.camel:camel-jackson:4.4.0')
@Grab('org.slf4j:slf4j-simple:2.0.9')
@Grab('com.sun.mail:jakarta.mail:2.0.1')
@Grab('org.apache.httpcomponents.client5:httpclient5:5.2.1')

import org.apache.camel.main.Main
import org.apache.camel.builder.RouteBuilder
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Initialize logger
Logger log = LoggerFactory.getLogger('EmailProcessor')

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
    config.putIfAbsent('CHECK_INTERVAL_SECONDS', '15')
    config.putIfAbsent('EMAIL_LIMIT', '10')
    config.putIfAbsent('OLLAMA_HOST', 'localhost')
    config.putIfAbsent('OLLAMA_PORT', '11434')
    config.putIfAbsent('OLLAMA_MODEL', 'qwen2.5:1.5b')
    config.putIfAbsent('TEST_EMAIL', 'info@softreck.com')
    config.putIfAbsent('HAWTIO_PORT', '8080')
    config.putIfAbsent('JMX_PORT', '1099')

    return config
}

def config = loadConfig()

println """
🚀 EMAIL AUTOMATION SYSTEM
========================
📧 Email Processing: Mock=${config['MOCK_EMAILS']}, Limit=${config['EMAIL_LIMIT']}
🤖 Ollama: http://${config['OLLAMA_HOST']}:${config['OLLAMA_PORT']}
📦 Model: ${config['OLLAMA_MODEL']}
🔄 Check Interval: ${config['CHECK_INTERVAL_SECONDS']}s

Starting services...
"""

// === METRICS CLASS ===
class EmailMetrics {
    int emailCount = 0
    int successCount = 0
    int errorCount = 0
    int maxEmails = 0
    long startTime = System.currentTimeMillis()
    
    def getMetrics() {
        def uptime = System.currentTimeMillis() - startTime
        def rate = emailCount > 0 ? (emailCount / (uptime / 1000.0)) : 0

        return [
            totalEmails: emailCount,
            successEmails: successCount,
            errorEmails: errorCount,
            successRate: emailCount > 0 ? (successCount * 100 / emailCount) : 0,
            emailsPerSecond: rate,
            uptimeSeconds: uptime / 1000,
            remainingEmails: Math.max(0, maxEmails - emailCount)
        ]
    }
    
    def incrementEmailCount() { emailCount++ }
    def incrementSuccessCount() { successCount++ }
    def incrementErrorCount() { errorCount++ }
}

// Initialize metrics
def metrics = new EmailMetrics()
metrics.maxEmails = Integer.parseInt(config['EMAIL_LIMIT'])

// === HELPER FUNCTIONS ===
def testOllama() {
    try {
        def url = "http://${config['OLLAMA_HOST']}:${config['OLLAMA_PORT']}/api/tags"
        def connection = new URL(url).openConnection()
        connection.setConnectTimeout(5000)
        def response = connection.inputStream.text
        println "✅ Ollama: Connected and ready"
        return true
    } catch (Exception e) {
        println "❌ Ollama: Failed (${e.message})"
        return false
    }
}

def generateMockEmail() {
    def emails = [
        [from: "jan.kowalski@example.com", subject: "Pytanie o hosting VPS",
         body: "Dzień dobry, interesuje mnie hosting VPS dla firmy. Proszę o ofertę.", priority: "Normal"],
        [from: "anna.nowak@firma.pl", subject: "Reklamacja - problem z SSL",
         body: "Certyfikat SSL wygasł i klienci nie mogą składać zamówień. Proszę o pilną pomoc.", priority: "High"],
        [from: "admin@urgent.com", subject: "PILNE - Serwer nie odpowiada",
         body: "Serwer produkcyjny nie działa od 30 minut. Straty wynoszą 5000 PLN/h!", priority: "Critical"],
        [from: "dev@startup.tech", subject: "Backup bazy danych",
         body: "Potrzebujemy skonfigurować automatyczny backup MongoDB. Kiedy można to zrobić?", priority: "Normal"],
        [from: "sklep@ecommerce.pl", subject: "Zwiększenie zasobów przed Black Friday",
         body: "Spodziewamy się 10x więcej ruchu. Potrzebujemy upgrade serwera do piątku.", priority: "High"],
        [from: config['TEST_EMAIL'], subject: "Test automatyzacji emaili",
         body: "To jest testowy email sprawdzający działanie systemu automatyzacji.", priority: "Low"]
    ]
    return emails[new Random().nextInt(emails.size())]
}

def generateResponse(emailData, useOllama) {
    if (useOllama) {
        return generateOllamaResponse(emailData)
    } else {
        return generateStandardResponse(emailData)
    }
}

def generateOllamaResponse(emailData) {
    try {
        def prompt = """Jesteś profesjonalnym asystentem technicznym. Otrzymałeś email:

Od: ${emailData.from}
Temat: ${emailData.subject}
Priorytet: ${emailData.priority}
Treść: ${emailData.body}

Napisz profesjonalną odpowiedź w języku polskim uwzględniając priorytet. Maksymalnie 150 słów."""

        def payload = new JsonBuilder([
            model: config['OLLAMA_MODEL'],
            prompt: prompt,
            stream: false,
            options: [
                temperature: 0.7,
                max_tokens: 200,
                top_p: 0.9
            ]
        ])

        def connection = new URL("http://${config['OLLAMA_HOST']}:${config['OLLAMA_PORT']}/api/generate").openConnection()
        connection.requestMethod = 'POST'
        connection.setRequestProperty('Content-Type', 'application/json')
        connection.setRequestProperty('User-Agent', 'Camel-Groovy-Email-Processor')
        connection.doOutput = true
        connection.outputStream.write(payload.toString().bytes)

        def response = new JsonSlurper().parseText(connection.inputStream.text)
        def aiResponse = response.response?.trim()

        if (aiResponse && aiResponse.length() > 20) {
            return aiResponse
        } else {
            throw new Exception("Empty or invalid response from Ollama")
        }

    } catch (Exception e) {
        println "⚠️ Ollama failed for ${emailData.from}: ${e.message}"
        return generateStandardResponse(emailData)
    }
}

def generateStandardResponse(emailData) {
    def name = emailData.from.split('@')[0].replace('.', ' ').split(' ').collect { it.capitalize() }.join(' ')
    def priority = emailData.priority

    def responseTime = ""
    switch (priority) {
        case "Critical":
            responseTime = "2 godzin"
            break
        case "High":
            responseTime = "4 godzin"
            break
        case "Normal":
            responseTime = "24 godzin"
            break
        default:
            responseTime = "48 godzin"
    }

    if (emailData.subject.toLowerCase().contains('pilne') || priority == 'Critical') {
        return """Dzień dobry ${name},

Otrzymaliśmy Państwa pilną wiadomość (priorytet: ${priority}).

Nasz zespół techniczny został natychmiast powiadomiony i skontaktuje się z Państwem w ciągu ${responseTime}.

Ticket ID: ${UUID.randomUUID().toString().take(8).toUpperCase()}

W razie dalszych pytań prosimy o kontakt pod numerem: +48 123 456 789

Pozdrawienia,
Zespół Obsługi Technicznej
24/7 Support Center"""
    } else {
        return """Dzień dobry ${name},

Dziękujemy za Państwa wiadomość dotyczącą: "${emailData.subject}"

Priorytet: ${priority}
Czas odpowiedzi: do ${responseTime}
Ticket ID: ${UUID.randomUUID().toString().take(8).toUpperCase()}

Państwa zapytanie zostało przekazane do odpowiedniego specjalisty, który skontaktuje się z Państwem w określonym czasie.

Pozdrawienia,
Zespół Obsługi Klienta"""
    }
}

// === MAIN APPLICATION ===
def ollamaAvailable = testOllama()

println """
🚀 STARTING EMAIL PROCESSOR
==========================
📧 Processing up to ${maxEmails} emails
🤖 AI Responses: ${ollamaAvailable ? 'Enabled' : 'Disabled'}
⏳ Check interval: ${config['CHECK_INTERVAL_SECONDS']} seconds

Starting routes...
"""

try {
    Main main = new Main()

    // Configure Camel context
    main.configure().withRoutesBuilder(new RouteBuilder() {
        @Override
        void configure() throws Exception {

            // Global error handler z retry
            errorHandler(deadLetterChannel("direct:errorHandler")
                .maximumRedeliveries(3)
                .redeliveryDelay(2000)
                .retryAttemptedLogLevel(org.apache.camel.LoggingLevel.WARN))

            // Main email processing timer
            def intervalMs = Integer.parseInt(config['CHECK_INTERVAL_SECONDS']) * 1000

            from("timer://emailProcessor?period=${intervalMs}&delay=5000")
                .routeId("email-processor")
                .log("🔄 Timer triggered (${intervalMs}ms interval)")
                .process { exchange ->
                    if (metrics.emailCount >= metrics.maxEmails) {
                        log.info("🎯 Reached email limit: ${metrics.emailCount}/${metrics.maxEmails}")
                        exchange.setProperty("completed", true)
                        return
                    }

                    def emailData = generateMockEmail()
                    metrics.incrementEmailCount()

                    log.info("📧 Processing email ${metrics.emailCount}/${metrics.maxEmails}: ${emailData.subject} [${emailData.priority}]")

                    // Store email data
                    exchange.setProperty("emailData", emailData)
                    exchange.in.setHeader("emailFrom", emailData.from)
                    exchange.in.setHeader("emailSubject", emailData.subject)
                    exchange.in.setHeader("emailPriority", emailData.priority)
                    exchange.in.body = emailData.body
                }
                .choice()
                    .when(exchangeProperty("completed").isEqualTo(true))
                        .log("✅ Email processing completed!")
                        .to("direct:showFinalStats")
                    .otherwise()
                        .to("direct:processWithAI")
                .end()

            // AI Processing
            from("direct:processWithAI")
                .routeId("ai-processor")
                .log("🤖 Processing with ${ollamaAvailable ? 'Ollama AI' : 'Standard Response'}")
                .process { exchange ->
                    def emailData = exchange.getProperty("emailData")

                    try {
                        def response = generateResponse(emailData, ollamaAvailable)
                        exchange.in.body = response
                        metrics.incrementSuccessCount()
                        log.info("✅ Response generated successfully")
                    } catch (Exception e) {
                        metrics.incrementErrorCount()
                        log.error("❌ Response generation failed: ${e.message}")
                        exchange.in.body = "Błąd przetwarzania. Skontaktujemy się wkrótce."
                    }
                }
                .to("direct:sendEmail")

            // Email sending (mock)
            from("direct:sendEmail")
                .routeId("email-sender")
                .log("📤 Sending response to: \${header.emailFrom}")
                .process { exchange ->
                    def priority = exchange.in.getHeader("emailPriority")
                    def subject = exchange.in.getHeader("emailSubject")
                    def from = exchange.in.getHeader("emailFrom")
                    def body = exchange.in.body

                    // Mock email sending
                    log.info("📧 === EMAIL SENT ===")
                    log.info("📬 To: ${from}")
                    log.info("📝 Re: ${subject}")
                    log.info("⚡ Priority: ${priority}")
                    log.info("📄 Body: ${body.toString().take(100)}...")
                    log.info("✅ Email delivered!")
                }
                .to("direct:updateMetrics")

            // Metrics update
            from("direct:updateMetrics")
                .routeId("metrics-updater")
                .process { exchange ->
                    def metrics = EmailMetrics.getMetrics()
                    log.info("📊 Metrics - Total: ${metrics.totalEmails}, Success: ${metrics.successEmails}, Errors: ${metrics.errorEmails}, Rate: ${metrics.successRate.round(1)}%")
                }

            // Error handler
            from("direct:errorHandler")
                .routeId("error-handler")
                .log("❌ Error handler triggered: \${exception.message}")
                .process { exchange ->
                    errorCount++
                    def error = exchange.getProperty("CamelExceptionCaught")
                    log.error("Error details: ${error?.message}")
                }

            // Progress monitor
            from("timer://progressMonitor?period=60000&delay=30000")
                .routeId("progress-monitor")
                .process { exchange ->
                    def metricsData = metrics.getMetrics()
                    def uptime = (metricsData.uptimeSeconds / 60).round(1)

                    log.info("""
📊 === EMAIL AUTOMATION STATS ===
⏰ Uptime: ${uptime} minutes
📧 Emails processed: ${metricsData.totalEmails}/${metrics.maxEmails}
✅ Success rate: ${metricsData.successRate.round(1)}%
🔄 Processing rate: ${metricsData.emailsPerSecond.round(2)} emails/sec
🤖 AI Engine: ${ollamaAvailable ? 'Ollama' : 'Standard'}
📊 Remaining: ${metricsData.remainingEmails} emails
=================================""")
                }

            // Final statistics
            from("direct:showFinalStats")
                .routeId("final-stats")
                .process { exchange ->
                    def metricsData = metrics.getMetrics()
                    def totalTime = (metricsData.uptimeSeconds / 60).round(1)

                    log.info("""
🎯 === FINAL STATISTICS ===
📧 Total emails processed: ${metricsData.totalEmails}
✅ Successful: ${metricsData.successEmails}
❌ Errors: ${metricsData.errorEmails}
📊 Success rate: ${metricsData.successRate.round(1)}%
⏰ Total processing time: ${totalTime} minutes
🤖 AI responses: ${ollamaAvailable ? metrics.successCount : 0}
📝 Standard responses: ${ollamaAvailable ? 0 : metrics.successCount}
🔄 Average rate: ${metricsData.emailsPerSecond.round(2)} emails/sec
==========================""")
                }

            // Simple health check endpoint
            from("timer://healthCheck?period=30000")
                .routeId("health-check")
                .process { exchange ->
                    def metricsData = metrics.getMetrics()
                    log.info("System health check - Processed: ${metricsData.totalEmails} emails, Success rate: ${metricsData.successRate.round(1)}%")
                }
        }
    })

    println """
✅ ROUTES CONFIGURED SUCCESSFULLY
=============================
📧 Processing: ${metrics.maxEmails} emails
⏱️  Interval: ${config['CHECK_INTERVAL_SECONDS']}s
🤖 AI: ${ollamaAvailable ? 'Ollama (' + config['OLLAMA_MODEL'] + ')' : 'Standard responses'}

🔄 Email processor is running...
Press Ctrl+C to stop
"""

    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread() {
        void run() {
            println "\n🛑 Shutting down Email Automation System..."
            def metricsData = metrics.getMetrics()
            println "📊 Final stats: ${metricsData.totalEmails} emails, ${metricsData.successRate.round(1)}% success rate"
            main.stop()
        }
    })

    // Start the application
    main.run()

} catch (Exception e) {
    println "❌ CRITICAL ERROR: ${e.message}"
    println "📋 Stack trace:"
    e.printStackTrace()
    System.exit(1)
}