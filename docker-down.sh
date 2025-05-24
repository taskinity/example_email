#!/bin/bash
# Script to stop Docker environments for email processing

# Get the directory containing this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Function to display usage
usage() {
    echo "Usage: $0 [basic|mock|full|all]"
    echo "  basic - Stop basic Docker environment"
    echo "  mock  - Stop mock Docker environment with MailHog"
    echo "  full  - Stop full Docker environment with real mail server"
    echo "  all   - Stop all Docker environments"
    exit 1
}

# Check if an environment is specified
if [ $# -eq 0 ]; then
    usage
fi

# Stop the specified Docker environment
case "$1" in
    basic)
        echo "Stopping basic Docker environment..."
        cd "$SCRIPT_DIR" && docker-compose -f docker/basic/docker-compose.yml down
        echo "Basic Docker environment stopped"
        ;;
    mock)
        echo "Stopping mock Docker environment..."
        cd "$SCRIPT_DIR" && docker-compose -f docker/mock/docker-compose.yml down
        echo "Mock Docker environment stopped"
        ;;
    full)
        echo "Stopping full Docker environment..."
        cd "$SCRIPT_DIR" && docker-compose -f docker/full/docker-compose.yml down
        echo "Full Docker environment stopped"
        ;;
    all)
        echo "Stopping all Docker environments..."
        cd "$SCRIPT_DIR" && docker-compose -f docker/basic/docker-compose.yml down
        cd "$SCRIPT_DIR" && docker-compose -f docker/mock/docker-compose.yml down
        cd "$SCRIPT_DIR" && docker-compose -f docker/full/docker-compose.yml down
        echo "All Docker environments stopped"
        ;;
    *)
        usage
        ;;
esac
