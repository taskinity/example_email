@Grab('javax.mail:javax.mail-api:1.6.2')
@Grab('com.sun.mail:javax.mail:1.6.2')

import javax.mail.*
import javax.mail.internet.*
import java.util.Properties

// === KONFIGURACJA Z .env ===
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
        'SMTP_PASSWORD': config['SMTP_PASSWORD'] ?: 'your-password',
        'FROM_EMAIL': config['FROM_EMAIL'] ?: 'tomasz@sapletta.de',
        'EMAIL_COUNT': '5',
        'EMAIL_INTERVAL': '30'
    ]
}

def config = loadConfig()

println """
📧 REAL EMAIL SENDER
====================
🎯 Target: ${config['TARGET_EMAIL']}
📤 SMTP: ${config['SMTP_SERVER']}:${config['SMTP_PORT']}
👤 From: ${config['FROM_EMAIL']}
🔐 User: ${config['SMTP_USERNAME']}
🔢 Count: ${config['EMAIL_COUNT']} emails
⏱️ Interval: ${config['EMAIL_INTERVAL']}s

⚠️  UWAGA: To wysyła PRAWDZIWE emaile!
Sprawdź konfigurację przed uruchomieniem.
"""

// Potwierdzenie przed wysłaniem
print "❓ Czy kontynuować wysyłanie prawdziwych emaili? (y/N): "
def confirmation = System.in.newReader().readLine()
if (!(confirmation?.toLowerCase() in ['y', 'yes', 'tak', 't'])) {
    println "❌ Anulowano przez użytkownika"
    System.exit(0)
}

// === SZABLONY EMAILI ===
def emailTemplates = [
    [
        from_name: "Jan Kowalski",
        from_email: "jan.kowalski@gmail.com",
        subject: "Pytanie o hosting VPS",
        body: """Dzień dobry,

Interesuje mnie Państwa oferta hostingu VPS. Potrzebuję:
- 4 CPU cores
- 8GB RAM
- 100GB SSD
- Unlimited transfer
- Backup daily

Czy mogliby Państwo przesłać szczegółową ofertę?

Pozdrawienia,
Jan Kowalski
IT Manager"""
    ],
    [
        from_name: "Anna Nowak",
        from_email: "anna.nowak@firma.pl",
        subject: "Problem z certyfikatem SSL",
        body: """Witam,

Mamy problem z certyfikatem SSL na domenie www.firma.pl:
- Certyfikat wygasł wczoraj
- Strona pokazuje ostrzeżenie bezpieczeństwa
- Klienci nie mogą składać zamówień

Prosimy o pilną interwencję!

Z poważaniem,
Anna Nowak
Webmaster"""
    ],
    [
        from_name: "Piotr Wiśniewski",
        from_email: "ceo@urgent-company.com",
        subject: "PILNE - Awaria serwera produkcyjnego",
        body: """PILNE!

Serwer produkcyjny nie odpowiada od 30 minut:
- IP: 185.xxx.xxx.xxx
- Usługa: E-commerce platform
- Błąd: Connection timeout
- Straty: ~5000 PLN/godzinę

Potrzebujemy natychmiastowej pomocy!

Piotr Wiśniewski
CEO, Urgent Company
Tel: +48 123 456 789"""
    ],
    [
        from_name: "Tomasz Admin",
        from_email: "admin@startup.tech",
        subject: "Backup bazy danych - konfiguracja",
        body: """Dzień dobry,

Potrzebujemy skonfigurować automatyczny backup:
- Baza: MongoDB 6.0
- Rozmiar: ~50GB
- Częstotliwość: Codziennie o 2:00
- Retencja: 30 dni
- Lokalizacja: S3 bucket

Czy mogą Państwo to skonfigurować?

Best regards,
Tomasz Admin
DevOps Engineer"""
    ],
    [
        from_name: "Sklep Online",
        from_email: "tech@sklep-online.pl",
        subject: "Upgrade serwera - Black Friday",
        body: """Witam,

Zbliża się Black Friday i potrzebujemy upgrade serwera:
- Aktualne: VPS Standard (4CPU, 8GB)
- Potrzebne: VPS Premium (8CPU, 16GB)
- Load balancer: TAK
- CDN: TAK
- Termin: do 20 listopada

Proszę o wycenę i harmonogram.

Pozdrawienia,
Zespół E-commerce"""
    ]
]

// === FUNKCJA WYSYŁANIA EMAILA ===
def sendEmail(fromEmail, fromName, toEmail, subject, body, smtpConfig) {
    try {
        // Konfiguracja SMTP
        Properties props = new Properties()

        if (smtpConfig.port == '465') {
            // SSL
            props.put("mail.smtp.host", smtpConfig.server)
            props.put("mail.smtp.port", smtpConfig.port)
            props.put("mail.smtp.auth", "true")
            props.put("mail.smtp.ssl.enable", "true")
            props.put("mail.smtp.ssl.trust", smtpConfig.server)
        } else if (smtpConfig.port == '587') {
            // TLS
            props.put("mail.smtp.host", smtpConfig.server)
            props.put("mail.smtp.port", smtpConfig.port)
            props.put("mail.smtp.auth", "true")
            props.put("mail.smtp.starttls.enable", "true")
            props.put("mail.smtp.ssl.trust", smtpConfig.server)
        } else {
            // Plain
            props.put("mail.smtp.host", smtpConfig.server)
            props.put("mail.smtp.port", smtpConfig.port)
            props.put("mail.smtp.auth", "true")
        }

        // Tworzenie sesji z autentykacją
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpConfig.username, smtpConfig.password)
            }
        })

        // Tworzenie wiadomości
        Message message = new MimeMessage(session)

        // Ustawienie From (prawdziwy nadawca w imieniu)
        message.setFrom(new InternetAddress(smtpConfig.fromEmail, fromName + " (via Simulator)"))

        // Ustawienie Reply-To na oryginalnego nadawcę (symulowanego)
        message.setReplyTo([new InternetAddress(fromEmail, fromName)] as Address[])

        // Ustawienie To
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))

        // Ustawienie tematu
        message.setSubject(subject)

        // Treść emaila z informacją o symulacji
        def fullBody = """[SYMULATOR EMAILI - TEST AUTOMATYZACJI]

Od: ${fromName} <${fromEmail}>
Do: ${toEmail}
Temat: ${subject}

--- TREŚĆ ORYGINALNEGO EMAILA ---

${body}

--- KONIEC TREŚCI ---

To jest email testowy wygenerowany przez symulator.
Oryginalny nadawca: ${fromEmail}
Czas wysłania: ${new Date()}
System: Email Automation Simulator v1.0
"""

        message.setText(fullBody)
        message.setSentDate(new Date())

        // Wysłanie emaila
        Transport.send(message)

        return [success: true, message: "Email wysłany pomyślnie"]

    } catch (Exception e) {
        return [success: false, message: "Błąd wysyłania: ${e.message}"]
    }
}

// === GŁÓWNA PĘTLA WYSYŁANIA ===
def smtpConfig = [
    server: config['SMTP_SERVER'],
    port: config['SMTP_PORT'],
    username: config['SMTP_USERNAME'],
    password: config['SMTP_PASSWORD'],
    fromEmail: config['FROM_EMAIL']
]

def emailCount = Integer.parseInt(config['EMAIL_COUNT'])
def interval = Integer.parseInt(config['EMAIL_INTERVAL']) * 1000

println "\n🚀 Rozpoczynanie wysyłania emaili...\n"

for (int i = 0; i < emailCount; i++) {
    def template = emailTemplates[i % emailTemplates.size()]
    def timestamp = new Date().format("yyyy-MM-dd HH:mm:ss")

    println "📧 EMAIL #${i+1}/${emailCount} [${timestamp}]"
    println "═══════════════════════════════════════════════"
    println "From: ${template.from_name} <${template.from_email}>"
    println "To: ${config['TARGET_EMAIL']}"
    println "Subject: ${template.subject}"
    println ""

    // Wysłanie emaila
    def result = sendEmail(
        template.from_email,
        template.from_name,
        config['TARGET_EMAIL'],
        template.subject,
        template.body,
        smtpConfig
    )

    if (result.success) {
        println "✅ EMAIL WYSŁANY POMYŚLNIE!"
        println "📡 Via SMTP: ${config['SMTP_SERVER']}:${config['SMTP_PORT']}"
    } else {
        println "❌ BŁĄD WYSYŁANIA: ${result.message}"
        println "🔧 Sprawdź konfigurację SMTP w .env"
    }

    println "───────────────────────────────────────────────"

    // Czekanie przed następnym emailem
    if (i < emailCount - 1) {
        println "⏱️ Czekanie ${config['EMAIL_INTERVAL']} sekund przed następnym emailem...\n"
        Thread.sleep(interval)
    }
}

println """

🎯 WYSYŁANIE ZAKOŃCZONE!
========================
📊 Wysłano ${emailCount} emaili
🎯 Do: ${config['TARGET_EMAIL']}
📤 Via: ${config['SMTP_SERVER']}:${config['SMTP_PORT']}
⏰ Czas: ${new Date()}

💡 Sprawdź skrzynkę ${config['TARGET_EMAIL']}
🤖 Twój system automatyzacji powinien teraz przetwarzać te emaile!
"""