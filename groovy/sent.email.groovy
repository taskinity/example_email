@Grab('org.apache.camel:camel-core:4.4.0')
@Grab('org.apache.camel:camel-mail:4.4.0')
@Grab('org.apache.camel:camel-main:4.4.0')
@Grab('org.slf4j:slf4j-simple:2.0.9')

import org.apache.camel.main.Main
import org.apache.camel.builder.RouteBuilder
import groovy.json.JsonBuilder
import java.text.SimpleDateFormat
import java.util.concurrent.ThreadLocalRandom

// Ładowanie konfiguracji z .env
def loadEnvConfig() {
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

    // Domyślne wartości dla symulatora
    config.putIfAbsent('SMTP_SERVER', 'localhost')
    config.putIfAbsent('SMTP_PORT', '1025')
    config.putIfAbsent('SMTP_USERNAME', 'simulator@test.com')
    config.putIfAbsent('SMTP_PASSWORD', 'password')
    config.putIfAbsent('TARGET_EMAIL', 'user@taskinity.org')
    config.putIfAbsent('SIMULATION_INTERVAL', '30')
    config.putIfAbsent('SIMULATION_COUNT', '5')
    config.putIfAbsent('REALISTIC_MODE', 'true')

    return config
}

def config = loadEnvConfig()

println """
📧 EMAIL EVENT SIMULATOR
========================
🎯 Target: ${config['TARGET_EMAIL']}
📤 SMTP: ${config['SMTP_SERVER']}:${config['SMTP_PORT']}
⏱️ Interval: ${config['SIMULATION_INTERVAL']}s
🔢 Count: ${config['SIMULATION_COUNT']} emails
🎭 Realistic: ${config['REALISTIC_MODE']}

Generowanie różnych typów emaili dla systemu automatyzacji...
Naciśnij Ctrl+C aby zatrzymać
"""

// Szablony różnych typów emaili
class EmailTemplates {
    static def customerInquiries = [
        [
            category: "PRODUCT_INQUIRY",
            senders: ["jan.kowalski@gmail.com", "anna.nowak@onet.pl", "piotr.wisniewski@wp.pl"],
            subjects: [
                "Pytanie o produkt XYZ",
                "Dostępność towaru",
                "Zapytanie o cenę",
                "Informacje o produkcie",
                "Czy jest w magazynie?"
            ],
            bodies: [
                "Dzień dobry,\n\nCzy produkt XYZ-123 jest dostępny? Proszę o informację o cenie i terminie dostawy.\n\nPozdrawienia,\nJan",
                "Witam,\n\nInteresuje mnie Państwa oferta. Czy mogliby Państwo przesłać szczegółowe informacje o produkcie ABC?\n\nDziękuję",
                "Dzień dobry,\n\nSzukam produktu do zastosowań przemysłowych. Czy mają Państwo coś odpowiedniego?\n\nZ poważaniem",
                "Proszę o wycenę 50 sztuk produktu Model-456. Potrzebne na przyszły tydzień.\n\nPozdrawienia"
            ]
        ],
        [
            category: "COMPLAINT",
            senders: ["reklamacja@firma.pl", "niezadowolony.klient@gmail.com", "serwis@company.com"],
            subjects: [
                "REKLAMACJA - wadliwy produkt",
                "Problem z zamówieniem #12345",
                "Zwrot towaru",
                "Niesprawny sprzęt",
                "Reklamacja gwarancyjna"
            ],
            bodies: [
                "Dzień dobry,\n\nOtrzymałem wadliwy produkt w zamówieniu nr 12345. Urządzenie nie włącza się mimo prawidłowego podłączenia.\n\nProszę o kontakt w sprawie zwrotu.\n\nKlient niezadowolony",
                "Witam,\n\nProdukt zamówiony 15.12.2024 jest uszkodzony. Opakowanie było zniszczone podczas transportu.\n\nOczekuję wymiany lub zwrotu pieniędzy.\n\nPozdrawienia",
                "PILNE!\n\nSprzęt nie działa zgodnie ze specyfikacją. To już druga sztuka z tym samym problemem.\n\nProszę o natychmiastowy kontakt!",
                "Reklamacja gwarancyjna produktu zakupionego 3 miesiące temu. Usterka po miesiącu użytkowania.\n\nNr seryjny: ABC123456"
            ]
        ],
        [
            category: "SUPPORT_REQUEST",
            senders: ["help@client.com", "admin@company.org", "it@business.pl"],
            subjects: [
                "Pomoc techniczna - instalacja",
                "Jak skonfigurować system?",
                "Problem z oprogramowaniem",
                "Instrukcja obsługi",
                "Wsparcie IT"
            ],
            bodies: [
                "Dzień dobry,\n\nMam problem z instalacją oprogramowania. System wyświetla błąd podczas uruchamiania.\n\nCzy mogliby Państwo pomóc?\n\nIT Admin",
                "Witam,\n\nPotrzebuję instrukcji konfiguracji systemu dla 20 użytkowników. Jakie są wymagania?\n\nZ góry dziękuję",
                "PILNE - Problem z licencją\n\nOprogramowanie przestało działać po aktualizacji. Błąd: License_Invalid\n\nProszę o szybką pomoc!",
                "Czy jest możliwość zdalnej konfiguracji systemu? Mamy problemy z dostępem do panelu administracyjnego.\n\nPozdrawienia"
            ]
        ],
        [
            category: "ORDER_STATUS",
            senders: ["zamowienia@sklep.pl", "klient@email.com", "biuro@firma.com"],
            subjects: [
                "Status zamówienia #ORDER-789",
                "Kiedy wysyłka?",
                "Sprawdzenie dostawy",
                "Śledzenie przesyłki",
                "Termin realizacji zamówienia"
            ],
            bodies: [
                "Dzień dobry,\n\nZłożyłem zamówienie tydzień temu (nr ORDER-789). Czy mogę sprawdzić status realizacji?\n\nPozdrawienia",
                "Witam,\n\nCzy zamówienie zostało już wysłane? Potrzebuję numer śledzenia przesyłki.\n\nDziękuję",
                "Zamówienie z 10.12.2024 nadal w trakcie realizacji. Kiedy mogę spodziewać się wysyłki?\n\nPilne!",
                "Proszę o aktualizację statusu zamówienia. Klient pyta o termin dostawy.\n\nBiuro obsługi"
            ]
        ],
        [
            category: "PARTNERSHIP",
            senders: ["partner@business.com", "cooperation@company.pl", "b2b@enterprise.org"],
            subjects: [
                "Propozycja współpracy B2B",
                "Partnerstwo strategiczne",
                "Oferta dla firm",
                "Współpraca handlowa",
                "Dystrybucja produktów"
            ],
            bodies: [
                "Szanowni Państwo,\n\nJestem zainteresowany nawiązaniem współpracy B2B. Nasza firma zajmuje się dystrybucją w regionie.\n\nCzy moglibyśmy umówić się na spotkanie?\n\nPartner Biznesowy",
                "Dzień dobry,\n\nChcielibyśmy zostać autoryzowanym dystrybutorem Państwa produktów. Działamy na rynku od 15 lat.\n\nProszę o kontakt",
                "Propozycja partnerstwa strategicznego w obszarze IT. Mamy doświadczenie w integracji systemów.\n\nCzy są Państwo zainteresowani?\n\nCEO TechCompany",
                "Oferujemy współpracę w zakresie obsługi klientów zagranicznych. Mówimy w 8 językach.\n\nSprawdźcie naszą ofertę!"
            ]
        ]
    ]

    static def getRandomEmail() {
        def category = customerInquiries[ThreadLocalRandom.current().nextInt(customerInquiries.size())]
        def sender = category.senders[ThreadLocalRandom.current().nextInt(category.senders.size())]
        def subject = category.subjects[ThreadLocalRandom.current().nextInt(category.subjects.size())]
        def body = category.bodies[ThreadLocalRandom.current().nextInt(category.bodies.size())]

        return [
            category: category.category,
            from: sender,
            subject: subject,
            body: body,
            timestamp: new Date()
        ]
    }

    static def getUrgentEmail() {
        return [
            category: "URGENT",
            from: "ceo@importantclient.com",
            subject: "PILNE - Problem z systemem produkcyjnym!",
            body: """PILNE!

System produkcyjny przestał działać o 14:30.
Linia produkcyjna stoi, straty 50,000 PLN/godzinę.

Błęd: SYSTEM_CRITICAL_FAILURE_001

Potrzebujemy natychmiastowej pomocy!

CEO
Ważny Klient Sp. z o.o.
tel: +48 123 456 789""",
            timestamp: new Date()
        ]
    }

    static def getSpamEmail() {
        def spamSubjects = [
            "🎉 PROMOCJA! Kup teraz -90%!",
            "💰 Zarobisz 10,000 PLN dziennie!",
            "🎁 DARMOWA nagroda czeka!",
            "⚡ Ostatnie 24 godziny wyprzedaży!",
            "🏆 Wygrałeś 1,000,000 EUR!"
        ]

        def spamBodies = [
            "Kliknij tutaj aby odebrać nagrodę! www.suspicious-link.com\n\nTo nie jest spam!",
            "Zarabiaj w domu! Bez doświadczenia! Tylko dziś!\n\nWysyłka gratis przy zamówieniu powyżej 0 PLN!",
            "UWAGA! Twoje konto zostanie zamknięte! Kliknij: fake-bank.com\n\nTo ostatnie ostrzeżenie!",
            "Powiększ swój... portfel! Inwestuj w kryptowaluty!\n\nGwarancja 1000% zysku!"
        ]

        return [
            category: "SPAM",
            from: "noreply@spam${ThreadLocalRandom.current().nextInt(1000)}.com",
            subject: spamSubjects[ThreadLocalRandom.current().nextInt(spamSubjects.size())],
            body: spamBodies[ThreadLocalRandom.current().nextInt(spamBodies.size())],
            timestamp: new Date()
        ]
    }
}

// Symulator wydarzeń
class EventSimulator extends RouteBuilder {
    def config
    def emailsSent = 0
    def maxEmails

    EventSimulator(config) {
        this.config = config
        this.maxEmails = Integer.parseInt(config['SIMULATION_COUNT'])
    }

    void configure() {

        // Obsługa błędów
        onException(Exception.class)
            .log("❌ SYMULATOR ERROR: \${exception.message}")
            .handled(true)

        // Timer główny - normalne emaile
        from("timer://normalEmails?period=${config['SIMULATION_INTERVAL']}000&delay=2000")
            .routeId("normal-email-generator")
            .filter { exchange -> emailsSent < maxEmails }
            .process { exchange ->
                def emailData = EmailTemplates.getRandomEmail()

                exchange.in.setHeader("emailCategory", emailData.category)
                exchange.in.setHeader("To", config['TARGET_EMAIL'])
                exchange.in.setHeader("From", emailData.from)
                exchange.in.setHeader("Subject", emailData.subject)
                exchange.in.body = emailData.body

                emailsSent++
                log.info("📧 Generating email ${emailsSent}/${maxEmails}: ${emailData.category}")
            }
            .to("direct:sendEmail")

        // Timer pilne emaile (rzadziej)
        from("timer://urgentEmails?period=120000&delay=30000")  // Co 2 minuty
            .routeId("urgent-email-generator")
            .filter { exchange -> emailsSent < maxEmails && ThreadLocalRandom.current().nextBoolean() }
            .process { exchange ->
                def emailData = EmailTemplates.getUrgentEmail()

                exchange.in.setHeader("emailCategory", emailData.category)
                exchange.in.setHeader("To", config['TARGET_EMAIL'])
                exchange.in.setHeader("From", emailData.from)
                exchange.in.setHeader("Subject", emailData.subject)
                exchange.in.body = emailData.body

                emailsSent++
                log.info("🚨 Generating URGENT email ${emailsSent}/${maxEmails}")
            }
            .to("direct:sendEmail")

        // Timer spam (jeśli realistic mode)
        if (config['REALISTIC_MODE'] == 'true') {
            from("timer://spamEmails?period=90000&delay=60000")  // Co 1.5 minuty
                .routeId("spam-email-generator")
                .filter { exchange -> ThreadLocalRandom.current().nextInt(100) < 30 }  // 30% szans
                .process { exchange ->
                    def emailData = EmailTemplates.getSpamEmail()

                    exchange.in.setHeader("emailCategory", emailData.category)
                    exchange.in.setHeader("To", config['TARGET_EMAIL'])
                    exchange.in.setHeader("From", emailData.from)
                    exchange.in.setHeader("Subject", emailData.subject)
                    exchange.in.body = emailData.body

                    log.info("💀 Generating SPAM email (realistic mode)")
                }
                .to("direct:sendEmail")
        }

        // Wysyłanie emaili
        from("direct:sendEmail")
            .routeId("email-sender")
            .log("📤 Sending: \${header.Subject} from \${header.From}")
            .choice()
                .when(simple("${config['SMTP_SERVER']} == 'mock'"))
                    .log("📧 MOCK EMAIL SENT:")
                    .log("   To: \${header.To}")
                    .log("   From: \${header.From}")
                    .log("   Subject: \${header.Subject}")
                    .log("   Category: \${header.emailCategory}")
                    .log("   Body: \${body}")
                .otherwise()
                    .process { exchange ->
                        // Budowanie URL SMTP
                        def smtpUrl = ""
                        if (config['SMTP_PORT'] == '465') {
                            smtpUrl = "smtps://${config['SMTP_SERVER']}:${config['SMTP_PORT']}"
                        } else if (config['SMTP_PORT'] == '587') {
                            smtpUrl = "smtp://${config['SMTP_SERVER']}:${config['SMTP_PORT']}?mail.smtp.starttls.enable=true"
                        } else {
                            smtpUrl = "smtp://${config['SMTP_SERVER']}:${config['SMTP_PORT']}"
                        }
                        exchange.in.setHeader("smtpUrl", smtpUrl)
                    }
                    .doTry()
                        .recipientList(simple("\${header.smtpUrl}?" +
                            "username=${config['SMTP_USERNAME']}&" +
                            "password=${config['SMTP_PASSWORD']}"))
                        .log("✅ Email sent successfully!")
                    .doCatch(Exception.class)
                        .log("❌ Failed to send email: \${exception.message}")
                        .log("📧 FALLBACK - would send: \${header.Subject}")
                    .end()
            .end()

        // Status monitor
        from("timer://statusMonitor?period=60000&delay=10000")  // Co minutę
            .routeId("status-monitor")
            .process { exchange ->
                def progress = emailsSent >= maxEmails ? 100 : (emailsSent * 100 / maxEmails)
                log.info("📊 Progress: ${emailsSent}/${maxEmails} emails sent (${progress}%)")

                if (emailsSent >= maxEmails) {
                    log.info("🎯 Simulation completed! Sent ${emailsSent} emails")
                    // Opcjonalnie zatrzymaj context
                    // exchange.context.stop()
                }
            }
    }
}

// Uruchomienie symulatora
Main main = new Main()
main.configure().routeBuilder(new EventSimulator(config))

try {
    main.run()
} catch (Exception e) {
    println "❌ Simulator error: ${e.message}"
    e.printStackTrace()
}