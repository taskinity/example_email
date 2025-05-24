@Grab('org.slf4j:slf4j-simple:2.0.9')

import java.net.Socket
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

// === DIAGNOSTYKA SYSTEMU EMAIL ===

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
    return config
}

def config = loadEnvConfig()

println """
🔍 EMAIL SYSTEM DIAGNOSTICS
============================
⏰ Time: ${new Date()}
📁 Working directory: ${System.getProperty("user.dir")}
📄 Config file: ${new File('.env').exists() ? '✅ Found' : '❌ Missing'}
"""

// === 1. SPRAWDZENIE KONFIGURACJI ===
println "\n1️⃣ CONFIGURATION CHECK"
println "━━━━━━━━━━━━━━━━━━━━━━━━"

def requiredVars = [
    'SMTP_SERVER', 'SMTP_PORT', 'SMTP_USERNAME', 'SMTP_PASSWORD',
    'IMAP_SERVER', 'IMAP_PORT', 'IMAP_USERNAME', 'IMAP_PASSWORD',
    'MOCK_EMAILS', 'EMAIL_LIMIT', 'CHECK_INTERVAL_SECONDS'
]

requiredVars.each { var ->
    def value = config[var]
    if (value) {
        if (var.contains('PASSWORD')) {
            println "✅ ${var}: ${'*' * value.length()}"
        } else {
            println "✅ ${var}: ${value}"
        }
    } else {
        println "❌ ${var}: MISSING"
    }
}

// === 2. SPRAWDZENIE POŁĄCZENIA SIECIOWEGO ===
println "\n2️⃣ NETWORK CONNECTIVITY"
println "━━━━━━━━━━━━━━━━━━━━━━━━━"

def testConnection(host, port, service) {
    try {
        Socket socket = new Socket()
        socket.connect(new InetSocketAddress(host, port as Integer), 5000)
        socket.close()
        println "✅ ${service}: ${host}:${port} - Connected"
        return true
    } catch (Exception e) {
        println "❌ ${service}: ${host}:${port} - Failed: ${e.message}"
        return false
    }
}

// Test Ollama
def ollamaOk = testConnection('localhost', '11434', 'Ollama')

// Test SMTP
def smtpOk = false
if (config['SMTP_SERVER']) {
    smtpOk = testConnection(config['SMTP_SERVER'], config['SMTP_PORT'], 'SMTP')
}

// Test IMAP
def imapOk = false
if (config['IMAP_SERVER']) {
    imapOk = testConnection(config['IMAP_SERVER'], config['IMAP_PORT'], 'IMAP')
}

// === 3. TEST OLLAMA API ===
println "\n3️⃣ OLLAMA API TEST"
println "━━━━━━━━━━━━━━━━━━━"

try {
    def ollamaUrl = "http://localhost:11434/api/tags"
    def connection = new URL(ollamaUrl).openConnection()
    connection.setConnectTimeout(5000)
    connection.setReadTimeout(10000)

    def response = connection.inputStream.text
    println "✅ Ollama API: Responding"

    // Test konkretnego modelu
    def modelUrl = "http://localhost:11434/api/generate"
    def testConnection = new URL(modelUrl).openConnection()
    testConnection.setRequestMethod("POST")
    testConnection.setRequestProperty("Content-Type", "application/json")
    testConnection.setDoOutput(true)

    def model = config['OLLAMA_MODEL'] ?: 'qwen2.5:1.5b'
    def testPayload = """{"model":"${model}","prompt":"test","stream":false}"""

    testConnection.outputStream.write(testPayload.bytes)
    def testResponse = testConnection.inputStream.text

    if (testResponse.contains('"response"')) {
        println "✅ Model ${model}: Available and responding"
    } else {
        println "❌ Model ${model}: Not responding properly"
        println "   Response: ${testResponse.take(100)}..."
    }

} catch (Exception e) {
    println "❌ Ollama API: Failed - ${e.message}"
    println "💡 Make sure Ollama is running: ollama serve"
}

// === 4. SPRAWDZENIE MOCK MODE ===
println "\n4️⃣ EMAIL MODE CHECK"
println "━━━━━━━━━━━━━━━━━━━"

def mockMode = config['MOCK_EMAILS']?.toLowerCase() == 'true'
println "📧 Mock mode: ${mockMode ? '✅ ENABLED' : '❌ DISABLED'}"

if (mockMode) {
    println "💡 System będzie generować mock emaile"
    println "   Nie potrzeba prawdziwego IMAP/SMTP"
} else {
    println "📨 System szuka prawdziwych emaili w IMAP"
    if (!imapOk) {
        println "⚠️  PROBLEM: IMAP connection failed!"
        println "   Rozwiązanie: Ustaw MOCK_EMAILS=true w .env"
    }
}

// === 5. TEST PROSTEGO EMAILA ===
println "\n5️⃣ SIMPLE EMAIL GENERATION TEST"
println "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

def generateTestEmail() {
    def emails = [
        [from: "test@example.com", subject: "Test email", body: "This is a test"],
        [from: "customer@client.com", subject: "Pytanie o produkt", body: "Interesuje mnie oferta"],
        [from: "support@firma.pl", subject: "Problem techniczny", body: "Mam problem z systemem"]
    ]

    def email = emails[new Random().nextInt(emails.size())]
    println "📧 Generated test email:"
    println "   From: ${email.from}"
    println "   Subject: ${email.subject}"
    println "   Body: ${email.body}"
    return email
}

def testEmail = generateTestEmail()
println "✅ Email generation: Working"

// === 6. CAMEL ROUTES STATUS ===
println "\n6️⃣ CAMEL ROUTES DIAGNOSIS"
println "━━━━━━━━━━━━━━━━━━━━━━━━━━"

println "🔍 Checking why routes might not be processing..."

if (!mockMode && !imapOk) {
    println "❌ MAIN ISSUE: IMAP connection failed"
    println "   Route waiting for emails from IMAP server"
    println "   But cannot connect to: ${config['IMAP_SERVER']}:${config['IMAP_PORT']}"
}

if (mockMode) {
    println "✅ Mock mode enabled - should generate emails automatically"
    println "   Timer interval: ${config['CHECK_INTERVAL_SECONDS']}s"
    println "   Email limit: ${config['EMAIL_LIMIT']}"
}

// === 7. REKOMENDACJE ===
println "\n7️⃣ RECOMMENDATIONS"
println "━━━━━━━━━━━━━━━━━━━━"

if (!ollamaOk) {
    println "🔧 Fix Ollama:"
    println "   ollama serve"
    println "   ollama pull qwen2.5:1.5b"
}

if (!mockMode && !imapOk) {
    println "🔧 Fix IMAP or enable mock mode:"
    println "   echo 'MOCK_EMAILS=true' >> .env"
    println "   Restart the email automation system"
}

if (mockMode && ollamaOk) {
    println "✅ System should work in mock mode!"
    println "🔍 If still no processing, check:"
    println "   - Are Camel routes actually starting?"
    println "   - Check for Java/Groovy errors in logs"
    println "   - Verify timer is triggering"
}

// === 8. QUICK FIX GENERATOR ===
println "\n8️⃣ EMERGENCY EMAIL GENERATOR"
println "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

println "🚨 If main system not working, try quick generator:"
println """
# Quick mock email injection:
groovy -e '
println "📧 EMERGENCY EMAIL"
println "From: emergency@test.com"
println "To: ${config['TEST_EMAIL'] ?: 'info@softreck.com'}"
println "Subject: Emergency test email"
println "Body: This is an emergency test to trigger automation"
println "Time: " + new Date()
'
"""

println "\n" + "="*50
println "🎯 DIAGNOSIS COMPLETE"
println "📊 Summary:"
println "   Ollama: ${ollamaOk ? '✅' : '❌'}"
println "   SMTP: ${smtpOk ? '✅' : '❌'}"
println "   IMAP: ${imapOk ? '✅' : '❌'}"
println "   Mock mode: ${mockMode ? '✅' : '❌'}"
println "="*50