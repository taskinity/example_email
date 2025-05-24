@Grab('org.apache.camel:camel-core:4.4.0')
@Grab('org.apache.camel:camel-mail:4.4.0')
@Grab('org.apache.camel:camel-main:4.4.0')
@Grab('org.slf4j:slf4j-simple:2.0.9')

import org.apache.camel.main.Main
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.CamelContext
import java.util.concurrent.ThreadLocalRandom

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

    return [
        'TARGET_EMAIL': config['TEST_EMAIL'] ?: 'info@softreck.com',
        'SMTP_SERVER': config['SMTP_SERVER'] ?: 'sapletta.com',
        'SMTP_PORT': config['SMTP_PORT'] ?: '465',
        'SMTP_USERNAME': config['SMTP_USERNAME'] ?: 'tomasz@sapletta.de',
        'SMTP_PASSWORD': config['SMTP_PASSWORD'] ?: 'password',
        'FROM_EMAIL': config['FROM_EMAIL'] ?: 'tomasz@sapletta.de',
        'SIMULATION_COUNT': '5',
        'SIMULATION_INTERVAL': '15'
    ]
}

def config = loadConfig()

println """
📧 ULTIMATE EMAIL SIMULATOR
============================
🎯 Target: ${config['TARGET_EMAIL']}
📤 SMTP: ${config['SMTP_SERVER']}:${config['SMTP_PORT']}
👤 From: ${config['FROM_EMAIL']}
⏱️ Interval: ${config['SIMULATION_INTERVAL']}s
🔢 Count: ${config['SIMULATION_COUNT']} emails

UWAGA: To jest SYMULATOR - generuje emaile!
Naciśnij Ctrl+C aby zatrzymać...
"""

// === SZABLONY EMAILI ===
def emailTemplates = [
    [
        from: "jan.kowalski@gmail.com",
        subject: "Pytanie o hosting i domeny",
        body: """Dzień dobry,

Szukam providera hostingu dla mojej firmy. Interesują mnie:
- Hosting VPS z SSD
- Rejestracja domeny .com i .pl
- SSL certificate
- Email hosting dla 10 osób

Czy mogliby Państwo przesłać ofertę?

Pozdrawienia,
Jan Kowalski
Dyrektor IT
ABC Company"""
    ],
    [
        from: "support@firma-klient.pl",
        subject: "Problem z serwerem - błąd 503",
        body: """Witam,

Od wczoraj wieczorem nasz serwer zwraca błąd 503:
- Domena: www.firma-klient.pl
- Hosting: VPS Basic
- Błąd: Service Temporarily Unavailable

Strona jest niedostępna dla klientów. Prosimy o pilną interwencję!

Z poważaniem,
Dział IT
Firma Klient Sp. z o.o."""
    ],
    [
        from: "ceo@wazna-firma.com",
        subject: "PILNE - Migracja serwera przed weekendem",
        body: """PILNE!

Potrzebujemy pilnej migracji naszego serwera produkcyjnego:
- Aktualne IP: 185.xxx.xxx.xxx
- Docelowy serwer: VPS Premium
- Deadline: Piątek 17:00
- Aplikacja: e-commerce (24/7 uptime wymagany)

Czy jest możliwość wykonania migracji dzisiaj?

CEO
Ważna Firma Sp. z o.o.
tel: +48 123 456 789"""
    ],
    [
        from: "admin@startup.tech",
        subject: "Konfiguracja SSL dla nowej aplikacji",
        body: """Dzień dobry,

Uruchamiamy nową aplikację i potrzebujemy konfiguracji SSL:
- Domena: api.startup.tech
- Subdomena: www.startup.tech
- Typ: Wildcard SSL (*.startup.tech)
- Framework: Node.js + MongoDB

Czy mogą Państwo pomóc z konfiguracją?

Best regards,
DevOps Team
StartupTech"""
    ],
    [
        from: "sklep@ecommerce.pl",
        subject: "Zwiększenie zasobów - Black Friday",
        body: """Witam,

Zbliża się Black Friday i spodziewamy się 10x więcej ruchu:
- Aktualne zasoby: VPS Standard (4 CPU, 8GB RAM)
- Potrzebne: VPS Premium (8 CPU, 16GB RAM)
- Termin: do 25 listopada
- Load balancer: wymagany

Proszę o wycenę upgrade'u infrastruktury.

Pozdrawienia,
E-commerce Team"""
    ]
]

// === GLOBALNY LICZNIK ===
def emailCount = 0
def maxEmails = Integer.parseInt(config['SIMULATION_COUNT'])

// === MAIN CAMEL APPLICATION ===
try {
    println "🚀 Initializing Camel Main..."

    // Utwórz Main context
    Main main = new Main()

    // KLUCZOWA ZMIANA: Użyj configure() callback zamiast addRouteBuilder()
    main.configure().addRoutesBuilder(new RouteBuilder() {
        @Override
        void configure() throws Exception {

            // Error handling
            onException(Exception.class)
                .log("❌ ERROR: \${exception.message}")
                .handled(true)

            // Main email generator timer
            from("timer://emailGen?period=${config['SIMULATION_INTERVAL']}000&delay=3000")
                .routeId("email-generator")
                .log("🔄 Timer tick...")
                .process { exchange ->
                    // Sprawdź limit
                    if (emailCount >= maxEmails) {
                        log.info("🎯 Simulation completed! Generated ${emailCount} emails")
                        exchange.setProperty("completed", true)
                        return
                    }

                    // Wybierz losowy szablon
                    def template = emailTemplates[ThreadLocalRandom.current().nextInt(emailTemplates.size())]

                    // Ustaw dane emaila
                    exchange.in.setHeader("To", config['TARGET_EMAIL'])
                    exchange.in.setHeader("From", template.from)
                    exchange.in.setHeader("Subject", template.subject)
                    exchange.in.body = template.body

                    emailCount++
                    log.info("📧 Generated email ${emailCount}/${maxEmails}: ${template.subject}")
                }
                .choice()
                    .when(exchangeProperty("completed").isEqualTo(true))
                        .log("✅ Simulation finished - stopping")
                        .process { exchange ->
                            // Opcjonalnie zatrzymaj context po zakończeniu
                            exchange.context.createProducerTemplate().asyncSendBody("timer://shutdown?repeatCount=1&delay=5000", "stop")
                        }
                    .otherwise()
                        .to("direct:sendEmail")
                .end()

            // Email sending
            from("direct:sendEmail")
                .routeId("email-sender")
                .log("📤 Sending: \${header.Subject}")
                .log("   From: \${header.From} → To: \${header.To}")
                .choice()
                    .when(simple("${config['SMTP_SERVER']} == 'mock'"))
                        .to("direct:mockSend")
                    .otherwise()
                        .to("direct:realSend")
                .end()

            // Mock sending
            from("direct:mockSend")
                .routeId("mock-sender")
                .log("📧 === MOCK EMAIL ===")
                .log("📬 Subject: \${header.Subject}")
                .log("👤 From: \${header.From}")
                .log("📄 Preview: \${bodyAs(String).substring(0, java.lang.Math.min(150, bodyAs(String).length()))}...")
                .log("✅ Mock email sent!")

            // Real SMTP sending
            from("direct:realSend")
                .routeId("smtp-sender")
                .doTry()
                    .process { exchange ->
                        // Build SMTP URL
                        def port = config['SMTP_PORT']
                        def server = config['SMTP_SERVER']
                        def smtpUrl

                        if (port == '465') {
                            smtpUrl = "smtps://${server}:${port}?mail.smtps.auth=true&mail.smtps.ssl.enable=true"
                        } else if (port == '587') {
                            smtpUrl = "smtp://${server}:${port}?mail.smtp.starttls.enable=true&mail.smtp.auth=true"
                        } else {
                            smtpUrl = "smtp://${server}:${port}?mail.smtp.auth=true"
                        }

                        exchange.setProperty("smtpUrl", smtpUrl)
                        log.debug("📡 SMTP URL: ${smtpUrl}")
                    }
                    .setHeader("From", simple("${config['FROM_EMAIL']}"))
                    .recipientList(simple("\${exchangeProperty.smtpUrl}" +
                        "?username=${config['SMTP_USERNAME']}" +
                        "&password=${config['SMTP_PASSWORD']}"))
                    .log("✅ Real email sent via SMTP!")
                .doCatch(Exception.class)
                    .log("❌ SMTP failed: \${exception.message}")
                    .log("🔄 Falling back to mock mode...")
                    .to("direct:mockSend")
                .end()

            // Shutdown timer (opcjonalne)
            from("timer://shutdown?repeatCount=1")
                .routeId("shutdown-timer")
                .log("🛑 Shutting down simulator...")
                .process { exchange ->
                    exchange.context.stop()
                }
        }
    })

    println "✅ Routes configured successfully!"
    println "🔄 Starting email simulation..."

    // Uruchom aplikację
    main.run()

} catch (Exception e) {
    println "❌ CRITICAL ERROR: ${e.message}"
    println "📋 Stack trace:"
    e.printStackTrace()

    println """

🔧 ALTERNATIVE - Simple Generator:
==================================
Jeśli Camel nadal nie działa, użyj prostego generatora:
"""

    // FALLBACK - prosty generator bez Camel
    runSimpleGenerator(config)
}

// === PROSTY GENERATOR BEZ CAMEL ===
def runSimpleGenerator(config) {
    println """
📧 FALLBACK: SIMPLE EMAIL GENERATOR
===================================
🎯 Target: ${config['TARGET_EMAIL']}
🔢 Count: ${config['SIMULATION_COUNT']} emails
"""

    def emails = [
        [from: "jan.kowalski@gmail.com", subject: "Pytanie o hosting",
         body: "Interesuje mnie hosting VPS dla mojej firmy."],
        [from: "support@klient.pl", subject: "Problem z serwerem",
         body: "Serwer zwraca błąd 503 od wczoraj."],
        [from: "ceo@firma.com", subject: "PILNE - Migracja serwera",
         body: "Potrzebujemy pilnej migracji przed weekendem."],
        [from: "admin@startup.tech", subject: "Konfiguracja SSL",
         body: "Potrzebujemy SSL dla nowej aplikacji."],
        [from: "sklep@ecommerce.pl", subject: "Upgrade infrastruktury",
         body: "Black Friday - potrzebujemy więcej zasobów."]
    ]

    def count = Integer.parseInt(config['SIMULATION_COUNT'])
    def interval = Integer.parseInt(config['SIMULATION_INTERVAL']) * 1000

    for (int i = 0; i < count; i++) {
        def email = emails[i % emails.size()]
        def timestamp = new Date().format("HH:mm:ss")

        println """
📧 EMAIL #${i+1}/${count} [${timestamp}]
════════════════════════════════════════
From: ${email.from}
To: ${config['TARGET_EMAIL']}
Subject: ${email.subject}

${email.body}

✅ Email event generated!
────────────────────────────────────────
"""

        if (i < count - 1) {
            println "⏱️ Waiting ${config['SIMULATION_INTERVAL']} seconds...\n"
            Thread.sleep(interval)
        }
    }

    println """
🎯 SIMULATION COMPLETED!
========================
📊 Generated ${count} email events
🎯 Target: ${config['TARGET_EMAIL']}
💡 Your email automation system should now have work to do!
"""
}