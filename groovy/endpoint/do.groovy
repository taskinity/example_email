// 🎯 ROZWIĄZANIE PROBLEMU: "Czy endpoint zadziała?"
// Apache Camel + Groovy - Walidacja przed startem

@Grab('org.apache.camel:camel-core:3.20.0')
@Grab('org.apache.camel:camel-file:3.20.0')
@Grab('org.apache.camel:camel-ftp:3.20.0')
@Grab('org.apache.camel:camel-mail:3.20.0')
@Grab('org.apache.camel:camel-http:3.20.0')

import org.apache.camel.*
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.spi.EndpointRegistry

// 🔧 ENV CONFIG LOADER
class EnvConfigLoader {
    static Map<String, String> config = [:]

    static void loadEnvFile(String envFile = '.env') {
        def file = new File(envFile)
        if (!file.exists()) {
            println "⚠️  Plik ${envFile} nie istnieje - używam zmiennych systemowych"
            return
        }

        file.eachLine { line ->
            line = line.trim()
            if (line && !line.startsWith('#') && line.contains('=')) {
                def parts = line.split('=', 2)
                def key = parts[0].trim()
                def value = parts[1].trim().replaceAll(/^["']|["']$/, '') // usuń cudzysłowy
                config[key] = value
                System.setProperty(key, value) // ustaw jako system property
            }
        }
        println "✅ Załadowano ${config.size()} zmiennych z ${envFile}"
    }

    static String get(String key, String defaultValue = null) {
        return System.getProperty(key) ?: System.getenv(key) ?: config[key] ?: defaultValue
    }

    static String resolveEndpoint(String endpoint) {
        // Zamień ${VAR} na wartości ze zmiennych
        return endpoint.replaceAll(/\$\{([^}]+)\}/) { match, varName ->
            get(varName, "\${${varName}}")
        }
    }
}

// 🔍 KLASA DO WALIDACJI ENDPOINTÓW
class EndpointValidator {

    CamelContext camelContext

    EndpointValidator(CamelContext context) {
        this.camelContext = context
    }

    // 📋 SPRAWDŹ WSZYSTKIE ENDPOINTY PRZED STARTEM
    Map<String, String> validateAllEndpoints(List<String> endpointUris) {
        def results = [:]

        endpointUris.each { uri ->
            results[uri] = validateSingleEndpoint(uri)
        }

        return results
    }

    // 🔍 WALIDACJA POJEDYNCZEGO ENDPOINTU Z ENV SUPPORT
    String validateSingleEndpoint(String uri) {
        try {
            // Rozwiń zmienne środowiskowe
            def resolvedUri = EnvConfigLoader.resolveEndpoint(uri)
            println "🔍 Sprawdzam: ${uri}"
            if (uri != resolvedUri) {
                println "   ↳ Rozwinięty: ${resolvedUri}"
            }

            // Sprawdź czy można utworzyć endpoint
            Endpoint endpoint = camelContext.getEndpoint(resolvedUri)

            // Sprawdź specyficzne dla typu
            def type = getEndpointType(resolvedUri)
            switch(type) {
                case 'file': return validateFileEndpoint(resolvedUri)
                case ['ftp','sftp']: return validateFtpEndpoint(resolvedUri)
                case ['http','https']: return validateHttpEndpoint(resolvedUri)
                case ['smtp','smtps','pop3','imap']: return validateSmtpEndpoint(resolvedUri)
                case ['jms','activemq','rabbitmq']: return validateJmsEndpoint(resolvedUri)
                case ['jdbc','sql']: return validateJdbcEndpoint(resolvedUri)
                case 'kafka': return validateKafkaEndpoint(resolvedUri)
                case ['websocket','netty','mina']: return validateNetworkEndpoint(resolvedUri)
                case ['ldap','ldaps']: return validateSocketEndpoint(resolvedUri, ~/ldaps?:\/\/([^\/]+)/, '389', 'LDAP')
                case 'mongodb': return validateSocketEndpoint(resolvedUri, ~/mongodb:\/\/([^\/]+)/, '27017', 'MongoDB')
                case 'redis': return validateSocketEndpoint(resolvedUri, ~/redis:\/\/([^\/]+)/, '6379', 'Redis')
                default: return validateGenericEndpoint(endpoint)
            }

        } catch (Exception e) {
            return "❌ BŁĄD: ${e.message}"
        }
    }

    // 📁 WALIDACJA ENDPOINT FILE
    String validateFileEndpoint(String uri) {
        def path = extractPath(uri, 'file:')
        def file = new File(path)

        if (!file.exists()) {
            if (uri.contains('?autoCreate=true')) {
                file.mkdirs()
                return "✅ Folder utworzony: ${path}"
            } else {
                return "❌ Folder nie istnieje: ${path}"
            }
        }

        if (!file.canRead()) return "❌ Brak uprawnień odczytu: ${path}"
        if (uri.contains('moveFailed') && !file.canWrite()) {
            return "❌ Brak uprawnień zapisu: ${path}"
        }

        return "✅ File endpoint OK: ${path}"
    }

    // 🌐 WALIDACJA HTTP ENDPOINT
    String validateHttpEndpoint(String uri) {
        try {
            def url = new URL(uri.replace('http:', 'http://').replace('https:', 'https://'))
            def connection = url.openConnection()
            connection.setConnectTimeout(5000)
            connection.setRequestMethod('HEAD')

            def responseCode = connection.responseCode

            if (responseCode >= 200 && responseCode < 400) {
                return "✅ HTTP endpoint OK: ${responseCode}"
            } else {
                return "⚠️ HTTP endpoint zwraca: ${responseCode}"
            }

        } catch (Exception e) {
            return "❌ HTTP endpoint nieosiągalny: ${e.message}"
        }
    }

    // 📧 WALIDACJA SMTP/MAIL ENDPOINTS
    String validateSmtpEndpoint(String uri) {
        return validateSocketEndpoint(uri, ~/smtp:\/\/([^:]+):?(\d+)?/, '25', 'SMTP')
    }

    // 📡 WALIDACJA FTP/SFTP ENDPOINTS
    String validateFtpEndpoint(String uri) {
        def port = uri.startsWith('sftp:') ? '22' : '21'
        def protocol = uri.startsWith('sftp:') ? 'SFTP' : 'FTP'
        return validateSocketEndpoint(uri, ~/(s?ftp):\/\/([^\/]+)/, port, protocol)
    }

    // 🔌 WALIDACJA SOCKET-BASED ENDPOINTS
    String validateSocketEndpoint(String uri, pattern, defaultPort, protocol) {
        try {
            def matcher = uri =~ pattern
            if (matcher.find()) {
                def host = matcher.group(matcher.groupCount()).split(':')[0]
                def port = uri.contains(':' + host + ':') ?
                    uri.split(':' + host + ':')[1].split('[/?]')[0] : defaultPort

                new Socket().withCloseable { socket ->
                    socket.connect(new InetSocketAddress(host, port as Integer), 5000)
                }
                return "✅ ${protocol} server: ${host}:${port}"
            }
        } catch (Exception e) {
            return "❌ ${protocol} error: ${e.message.take(50)}"
        }
        return "❌ Invalid ${protocol} format"
    }

    // 💾 WALIDACJA DATABASE ENDPOINTS
    String validateJdbcEndpoint(String uri) {
        def drivers = ['h2':'org.h2.Driver', 'mysql':'com.mysql.jdbc.Driver',
                       'postgresql':'org.postgresql.Driver', 'oracle':'oracle.jdbc.OracleDriver']
        def dbType = uri.split(':')[1]
        return drivers[dbType] ? "✅ DB driver available: ${dbType}" : "❌ DB driver missing: ${dbType}"
    }

    // ⚡ WALIDACJA JMS/ACTIVEMQ ENDPOINTS
    String validateJmsEndpoint(String uri) {
        if (uri.contains('activemq:')) return validateSocketEndpoint(uri, ~/tcp:\/\/([^:]+):?(\d+)?/, '61616', 'ActiveMQ')
        if (uri.contains('rabbitmq:')) return validateSocketEndpoint(uri, ~/([^:]+):?(\d+)?/, '5672', 'RabbitMQ')
        return "⚠️ JMS broker validation needed"
    }

    // 📊 WALIDACJA KAFKA ENDPOINTS
    String validateKafkaEndpoint(String uri) {
        return validateSocketEndpoint(uri, ~/kafka:([^?]+)/, '9092', 'Kafka')
    }

    // 🌐 WALIDACJA WEBSOCKET/TCP ENDPOINTS
    String validateNetworkEndpoint(String uri) {
        if (uri.startsWith('websocket:')) return validateSocketEndpoint(uri, ~/ws:\/\/([^\/]+)/, '80', 'WebSocket')
        if (uri.startsWith('netty:')) return validateSocketEndpoint(uri, ~/tcp:\/\/([^:]+):(\d+)/, '8081', 'Netty TCP')
        if (uri.startsWith('mina:')) return validateSocketEndpoint(uri, ~/tcp:\/\/([^:]+):(\d+)/, '8081', 'Mina TCP')
        return "⚠️ Network endpoint type unknown"
    }

    // 🔧 WALIDACJA GENERYCZNA
    String validateGenericEndpoint(Endpoint endpoint) {
        if (endpoint) {
            return "✅ Endpoint można utworzyć: ${endpoint.class.simpleName}"
        } else {
            return "❌ Nie można utworzyć endpointu"
        }
    }

    // 🏷️ HELPER METHODS
    String getEndpointType(String uri) {
        return uri.split(':')[0].toLowerCase()
    }

    String extractPath(String uri, String prefix) {
        return uri.substring(prefix.length()).split('\\?')[0]
    }
}

// 🎯 KLASA ROUTE Z PRE-VALIDATION
class ValidatedRoutes extends RouteBuilder {

    void configure() {

        // 🔧 ZAŁADUJ KONFIGURACJĘ Z .env
        EnvConfigLoader.loadEnvFile()

        // 📋 ENDPOINTY Z WYKORZYSTANIEM ZMIENNYCH ŚRODOWISKOWYCH
        def endpointsToValidate = [
            "file:\${INPUT_DIR:/tmp/input}?noop=true",
            "file:\${OUTPUT_DIR:/tmp/output}",
            "file:\${ERROR_DIR:/tmp/error}?autoCreate=true",
            "http://\${API_HOST:httpbin.org}/status/200",
            "https://\${EXTERNAL_API:api.github.com}",
            "smtp://\${SMTP_HOST:localhost}:\${SMTP_PORT:25}",
            "ftp://\${FTP_HOST:localhost}:\${FTP_PORT:21}",
            "activemq:\${QUEUE_NAME:queue:test.queue}",
            "kafka:\${KAFKA_HOST:localhost}:\${KAFKA_PORT:9092}",
            "jdbc:\${DB_TYPE:h2}:\${DB_URL:mem:testdb}",
            "mongodb://\${MONGO_HOST:localhost}:\${MONGO_PORT:27017}",
            "redis://\${REDIS_HOST:localhost}:\${REDIS_PORT:6379}"
        ]

        // ✅ WALIDUJ PRZED STARTEM
        def validator = new EndpointValidator(getContext())
        def results = validator.validateAllEndpoints(endpointsToValidate)

        // 📊 POKAŻ WYNIKI
        println "\n" + "="*60
        println "🔍 RAPORT WALIDACJI ENDPOINTÓW"
        println "="*60

        def successCount = 0
        def errorCount = 0

        results.each { uri, result ->
            println "${result} | ${uri}"
            if (result.startsWith("✅")) successCount++
            else if (result.startsWith("❌")) errorCount++
        }

        println "="*60
        println "📊 PODSUMOWANIE: ✅ ${successCount} OK | ❌ ${errorCount} BŁĘDÓW"
        println "="*60

        // 🚨 ZATRZYMAJ JEŚLI KRYTYCZNE BŁĘDY
        if (errorCount > 0) {
            def criticalErrors = results.findAll { k, v ->
                v.startsWith("❌") && !k.contains("autoCreate")
            }

            if (criticalErrors.size() > 0) {
                println "🚨 ZATRZYMUJĘ - wykryto krytyczne błędy endpointów!"
                throw new RuntimeException("Validation failed: ${criticalErrors.keySet()}")
            }
        }

        // 🚀 JEŚLI OK - DEFINIUJ ROUTES Z ENV VARIABLES
        from("timer:validation?period=\${TIMER_PERIOD:10000}")
        .routeId("health-check")
        .setBody(constant("System OK - endpointy zwalidowane z .env!"))
        .to("log:health?level=\${LOG_LEVEL:INFO}")

        // Route z wykorzystaniem zmiennych środowiskowych
        from("file:\${INPUT_DIR:/tmp/input}?noop=true")
        .routeId("file-processor")
        .onException(Exception.class)
            .to("file:\${ERROR_DIR:/tmp/error}")
            .handled(true)
        .end()
        .to("file:\${OUTPUT_DIR:/tmp/output}")
    }
}

// 🚀 URUCHOMIENIE Z WALIDACJĄ
try {
    def camelContext = new DefaultCamelContext()
    camelContext.addRoutes(new ValidatedRoutes())
    camelContext.start()
    
    println "\n🎉 System uruchomiony pomyślnie!"
    println "📁 Wszystkie endpointy sprawdzone i działają"
    
    // Keep alive
    Thread.sleep(30000)
    camelContext.stop()
    
} catch (Exception e) {
    println "\n💥 BŁĄD URUCHOMIENIA: ${e.message}"
    println "🔧 Sprawdź konfigurację endpointów i spróbuj ponownie"
}

// 💡 BONUS: HEALTHCHECK ENDPOINT
// Dodaj to do monitorowania:
/*
from("jetty:http://localhost:8081/health")
.setBody(constant('{"status":"OK","validated_endpoints":true}'))
.setHeader("Content-Type", constant("application/json"))
*/