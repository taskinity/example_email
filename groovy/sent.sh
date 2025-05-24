#!/bin/bash

echo "🎬 Quick Email Event Simulator"

# Konfiguracja
TARGET_EMAIL=${1:-"user@taskinity.org"}
SMTP_SERVER=${2:-"localhost"}
SMTP_PORT=${3:-"1025"}
COUNT=${4:-"10"}
INTERVAL=${5:-"15"}

cat > .env.simulator << EOF
TARGET_EMAIL=${TARGET_EMAIL}
SMTP_SERVER=${SMTP_SERVER}
SMTP_PORT=${SMTP_PORT}
SMTP_USERNAME=simulator@test.com
SMTP_PASSWORD=password
SIMULATION_COUNT=${COUNT}
SIMULATION_INTERVAL=${INTERVAL}
REALISTIC_MODE=true
EOF

echo "📧 Simulator config:"
echo "   Target: ${TARGET_EMAIL}"
echo "   SMTP: ${SMTP_SERVER}:${SMTP_PORT}"
echo "   Count: ${COUNT} emails"
echo "   Interval: ${INTERVAL}s"
echo ""

# Sprawdź czy MailHog działa (dla testów)
if [ "$SMTP_SERVER" = "localhost" ] && [ "$SMTP_PORT" = "1025" ]; then
    if ! curl -s http://localhost:8025 > /dev/null; then
        echo "⚠️ MailHog nie działa. Uruchom: docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog"
        echo "   Lub zmień SMTP_SERVER na prawdziwy serwer"
        read -p "Kontynuować z mock mode? (y/N): " choice
        if [[ ! $choice =~ ^[Yy]$ ]]; then
            exit 1
        fi
        # Przełącz na mock mode
        sed -i 's/SMTP_SERVER=localhost/SMTP_SERVER=mock/' .env.simulator
    fi
fi

echo "🚀 Starting simulator..."
#cp .env.simulator .env
groovy sent.email.groovy