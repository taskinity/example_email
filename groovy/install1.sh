#!/bin/bash

set -e

echo "=== Instalacja Apache Camel, Hawtio i Groovy na Fedorze ==="

# Katalog docelowy
TOOLS_DIR="$HOME/tools"
mkdir -p "$TOOLS_DIR"
cd "$TOOLS_DIR"

echo "Katalog roboczy: $(pwd)"

# === APACHE CAMEL ===
echo ""
echo "=== APACHE CAMEL ==="
CAMEL_VERSION="4.11.0"
CAMEL_FILE="apache-camel-$CAMEL_VERSION-src.zip"
CAMEL_URL="https://archive.apache.org/dist/camel/apache-camel/$CAMEL_VERSION/$CAMEL_FILE"

if [ -f "$CAMEL_FILE" ]; then
    echo "Plik $CAMEL_FILE już istnieje, pomijam pobieranie"
else
    echo "Pobieranie Apache Camel $CAMEL_VERSION (wersja binarna)..."
    echo "URL: $CAMEL_URL"
    wget --progress=bar:force "$CAMEL_URL" -O "$CAMEL_FILE"
    echo "✓ Pobrano Apache Camel"
fi

if [ -d "apache-camel-$CAMEL_VERSION" ]; then
    echo "Apache Camel już rozpakowany"
else
    echo "Rozpakowywanie Apache Camel..."
    case "$CAMEL_FILE" in
        *.zip)
            if ! command -v unzip &> /dev/null; then
                echo "❌ Brak narzędzia unzip"
                exit 1
            fi
            unzip -q "$CAMEL_FILE" 2> >(grep -v "lchmod" >&2)
            echo "✓ Rozpakowano Apache Camel (ZIP)"
            ;;
        *.tar.gz|*.tgz)
            tar -xzf "$CAMEL_FILE"
            echo "✓ Rozpakowano Apache Camel (tar.gz)"
            ;;
        *)
            echo "❌ Nieznany format archiwum: $CAMEL_FILE"
            exit 1
            ;;
    esac
fi

ln -sf "apache-camel-$CAMEL_VERSION" apache-camel
echo "✓ Utworzono symlink dla Apache Camel"

# === GROOVY ===
echo ""
echo "=== GROOVY ==="
GROOVY_VERSION="4.0.24"
GROOVY_FILE="apache-groovy-binary-$GROOVY_VERSION.zip"
GROOVY_URL="https://archive.apache.org/dist/groovy/$GROOVY_VERSION/distribution/$GROOVY_FILE"

if [ -f "$GROOVY_FILE" ]; then
    echo "Plik $GROOVY_FILE już istnieje, pomijam pobieranie"
else
    echo "Pobieranie Groovy $GROOVY_VERSION..."
    wget --progress=bar:force "$GROOVY_URL" -O "$GROOVY_FILE"
    echo "✓ Pobrano Groovy"
fi

if [ -d "groovy-$GROOVY_VERSION" ]; then
    echo "Groovy już rozpakowany"
else
    echo "Rozpakowywanie Groovy..."
    unzip -q "$GROOVY_FILE"
    echo "✓ Rozpakowano Groovy"
fi

ln -sf "groovy-$GROOVY_VERSION" groovy
echo "✓ Utworzono symlink dla Groovy"

# === HAWTIO ===
echo ""
echo "=== HAWTIO ==="
HAWTIO_VERSION="2.17.5"
HAWTIO_FILE="hawtio-app-$HAWTIO_VERSION.jar"
HAWTIO_URL="https://repo1.maven.org/maven2/io/hawt/hawtio-app/$HAWTIO_VERSION/$HAWTIO_FILE"

mkdir -p hawtio

if [ -f "hawtio/hawtio-app.jar" ]; then
    echo "Hawtio już zainstalowany"
else
    echo "Pobieranie Hawtio $HAWTIO_VERSION..."
    wget --progress=bar:force "$HAWTIO_URL" -O "$HAWTIO_FILE"
    mv "$HAWTIO_FILE" hawtio/hawtio-app.jar
    echo "✓ Pobrano Hawtio"
fi

# Skrypt startowy dla Hawtio
cat > hawtio/start-hawtio.sh << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"
echo "Uruchamianie Hawtio na porcie 8080..."
echo "Otwórz przeglądarkę: http://localhost:8080/hawtio"
java -Dhawtio.authenticationEnabled=false -Dhawtio.offline=true -jar hawtio-app.jar --port 8080
EOF

chmod +x hawtio/start-hawtio.sh
echo "✓ Utworzono skrypt startowy dla Hawtio"

# === ŚRODOWISKO ===
echo ""
echo "=== KONFIGURACJA ŚRODOWISKA ==="

if ! grep -q "CAMEL_HOME" ~/.bashrc; then
    echo "Dodawanie zmiennych środowiskowych do ~/.bashrc..."
    cat >> ~/.bashrc << EOF

# Apache Camel, Groovy, Hawtio - dodane $(date)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export CAMEL_HOME=$TOOLS_DIR/apache-camel
export GROOVY_HOME=$TOOLS_DIR/groovy
export PATH=\$PATH:\$CAMEL_HOME/bin:\$GROOVY_HOME/bin
alias hawtio='$TOOLS_DIR/hawtio/start-hawtio.sh'
EOF
    echo "✓ Dodano zmienne środowiskowe"
else
    echo "Zmienne środowiskowe już skonfigurowane"
fi

# === PRZYKŁADY ===
echo ""
echo "=== TWORZENIE PRZYKŁADÓW ==="
mkdir -p apache-camel/examples

cat > apache-camel/examples/hello-route.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<routes xmlns="http://camel.apache.org/schema/spring">
    <route id="hello-timer">
        <from uri="timer:hello?period=5000"/>
        <transform>
            <constant>Hello World from Apache Camel! Time: ${date:now:yyyy-MM-dd HH:mm:ss}</constant>
        </transform>
        <to uri="log:hello"/>
    </route>
</routes>
EOF

cat > apache-camel/examples/hello-route.groovy << 'EOF'
from('timer:hello?period=3000')
    .setBody(constant('Hello from Camel + Groovy!'))
    .process { exchange ->
        def body = exchange.in.body
        def time = new Date().format('yyyy-MM-dd HH:mm:ss')
        exchange.in.body = "${body} - ${time}"
    }
    .log('${body}')
    .to('mock:result')

from('direct:json-transform')
    .setBody(constant('{"name": "Camel", "language": "Groovy"}'))
    .log('Original: ${body}')
    .process { exchange ->
        def json = new groovy.json.JsonSlurper().parseText(exchange.in.body as String)
        json.timestamp = new Date().time
        json.processed = true
        exchange.in.body = new groovy.json.JsonBuilder(json).toString()
    }
    .log('Transformed: ${body}')
EOF

cat > groovy/hello.groovy << 'EOF'
println "="*50
println "Hello World from Groovy!"
println "Java version: ${System.getProperty('java.version')}"
println "Groovy version: ${GroovySystem.version}"
println "Current time: ${new Date()}"
println "="*50

def numbers = [1, 2, 3, 4, 5]
def squares = numbers.collect { it * it }
println "Numbers: $numbers"
println "Squares: $squares"

@Grab('org.apache.camel:camel-core:4.11.0')
import groovy.json.*

def json = new JsonBuilder()
json {
    name "Apache Camel"
    version "4.11.0"
    languages(["Java", "Groovy", "XML"])
    features {
        integration true
        routing true
        transformation true
    }
}

println "\nJSON Example:"
println json.toPrettyString()
EOF

echo "✓ Utworzono przykładowe pliki"

# === PODSUMOWANIE ===
echo ""
echo "🎉 INSTALACJA ZAKOŃCZONA POMYŚLNIE! 🎉"
echo ""
echo "Zainstalowane wersje:"
echo "- Apache Camel: $CAMEL_VERSION"
echo "- Groovy: $GROOVY_VERSION"
echo "- Hawtio: $HAWTIO_VERSION"
echo ""
echo "Lokalizacje:"
echo "- Apache Camel: $TOOLS_DIR/apache-camel"
echo "- Groovy: $TOOLS_DIR/groovy"
echo "- Hawtio: $TOOLS_DIR/hawtio"
echo ""
echo "NASTĘPNE KROKI:"
echo "1. Przeładuj środowisko: source ~/.bashrc"
echo "2. Sprawdź instalację:"
echo "   - groovy --version"
echo "   - java -version"
echo "   - camel --version"
echo "3. Uruchom przykłady:"
echo "   - groovy ~/tools/groovy/hello.groovy"
echo "   - hawtio"
echo "   - camel run ~/tools/apache-camel/examples/hello-route.xml"
echo "   - camel run ~/tools/apache-camel/examples/hello-route.groovy"
