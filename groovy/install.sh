#!/bin/bash

echo "🚀 Instalacja Apache Camel Groovy Email Automation"

# Sprawdzenie Java
if ! command -v java &> /dev/null; then
    echo "Instalacja OpenJDK 17..."
    sudo apt-get update
    sudo apt-get install -y openjdk-17-jdk
fi

# Sprawdzenie Groovy
if ! command -v groovy &> /dev/null; then
    echo "Instalacja Groovy..."

    # Pobieranie i instalacja Groovy
    GROOVY_VERSION="4.0.21"
    cd /tmp
    wget "https://archive.apache.org/dist/groovy/${GROOVY_VERSION}/distribution/apache-groovy-binary-${GROOVY_VERSION}.zip"
    sudo unzip "apache-groovy-binary-${GROOVY_VERSION}.zip" -d /opt/
    sudo ln -sf "/opt/groovy-${GROOVY_VERSION}/bin/groovy" /usr/local/bin/groovy
    sudo ln -sf "/opt/groovy-${GROOVY_VERSION}/bin/groovyc" /usr/local/bin/groovyc

    # Cleanup
    rm "apache-groovy-binary-${GROOVY_VERSION}.zip"
    cd -
fi

# Sprawdzenie .env
if [ ! -f .env ]; then
    echo "❌ Brak pliku .env!"
    echo "Skopiuj podany plik .env do katalogu projektu"
    exit 1
fi

echo "✅ Instalacja zakończona!"
echo ""
echo "Uruchomienie:"
echo "groovy email-automation.groovy"
echo ""
echo "Z logowaniem:"
echo "groovy email-automation.groovy 2>&1 | tee automation.log"
echo ""
echo "Konfiguracja w .env:"
echo "- MOCK_EMAILS=true - dla testów"
echo "- SEND_TEST_EMAILS=true - wyślij email testowy"
echo "- CONTINUOUS_MODE=true - tryb ciągły"