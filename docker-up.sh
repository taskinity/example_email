#!/bin/bash
# Script to start Docker environments for email processing

# Get the directory containing this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Function to display usage
usage() {
    echo "Usage: $0 [basic|mock|full]"
    echo "  basic - Start basic Docker environment with mock data"
    echo "  mock  - Start mock Docker environment with MailHog"
    echo "  full  - Start full Docker environment with real mail server"
    exit 1
}

# Check if an environment is specified
if [ $# -eq 0 ]; then
    usage
fi

# Start the specified Docker environment
case "$1" in
    basic)
        echo "Starting basic Docker environment..."
        cd "$SCRIPT_DIR" && docker-compose -f docker/basic/docker-compose.yml up -d
        echo "Basic Docker environment started"
        ;;
    mock)
        echo "Starting mock Docker environment with MailHog..."
        cd "$SCRIPT_DIR" && docker-compose -f docker/mock/docker-compose.yml up -d
        echo "Mock Docker environment started. Access MailHog UI at http://localhost:8025"
        ;;
    full)
        echo "Starting full Docker environment..."
        cd "$SCRIPT_DIR" && docker-compose -f docker/full/docker-compose.yml up -d
        echo "Full Docker environment started"
        ;;
    *)
        usage
        ;;
esac
