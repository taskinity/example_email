# Makefile for Email Processing Examples
# This file contains all possible tests, examples, and run commands

# Variables
# Use the virtual environment's Python if it exists, otherwise fall back to python3
PYTHON = $(shell if [ -f ../venv/bin/python ]; then echo ../venv/bin/python; else echo python3; fi)
DOCKER_COMPOSE = docker-compose
PYTEST = pytest

# Default target
.PHONY: help
help:
	@echo "Available commands:"
	@echo "  make run-basic          - Run basic email processing example"
	@echo "  make run-pipeline       - Run full email processing pipeline"
	@echo "  make run-test           - Run email notification test"
	@echo "  make docker-up          - Start all Docker containers"
	@echo "  make docker-down        - Stop all Docker containers"
	@echo "  make docker-up-basic    - Start basic Docker setup"
	@echo "  make docker-up-full     - Start full Docker setup with IMAP/SMTP"
	@echo "  make docker-up-mock     - Start mock email server Docker setup"
	@echo "  make docker-down-basic  - Stop basic Docker setup"
	@echo "  make docker-down-full   - Stop full Docker setup"
	@echo "  make docker-down-mock   - Stop mock email server Docker setup"
	@echo "  make logs               - Show logs help"
	@echo "  make logs-basic         - View logs from basic environment"
	@echo "  make logs-full          - View logs from full environment"
	@echo "  make logs-mock          - View logs from mock environment"
	@echo "  make test               - Run all tests"
	@echo "  make test-unit          - Run unit tests"
	@echo "  make test-integration   - Run integration tests"
	@echo "  make clean              - Remove temporary files"
	@echo "  make lint               - Run linter on code"
	@echo "  make format             - Format code"

## Install dependencies
install:
	@echo "Installing dependencies..."
	$(PYTHON) -m pip install --upgrade pip
	if [ ! -d "venv" ]; then \
		echo "Creating virtual environment..."; \
		$(PYTHON) -m venv venv; \
	fi
	. venv/bin/activate && \
	pip install -r requirements.txt

# Run examples
.PHONY: run-basic
run-basic:
	$(PYTHON) tasks/fetch_emails.py

.PHONY: run-pipeline
run-pipeline:
	$(PYTHON) email_pipeline.py

.PHONY: run-test
run-test:
	$(PYTHON) test_email_notification.py

# Docker commands
.PHONY: docker-up
docker-up:
	$(DOCKER_COMPOSE) -f docker/full/docker-compose.yml up -d

.PHONY: docker-down
docker-down:
	$(DOCKER_COMPOSE) -f docker/full/docker-compose.yml down

.PHONY: docker-up-basic
docker-up-basic:
	$(DOCKER_COMPOSE) -f docker/basic/docker-compose.yml up -d

.PHONY: docker-up-full
docker-up-full:
	$(DOCKER_COMPOSE) -f docker/full/docker-compose.yml up -d

.PHONY: docker-up-mock
docker-up-mock:
	$(DOCKER_COMPOSE) -f docker/mock/docker-compose.yml up -d

.PHONY: docker-down-basic
docker-down-basic:
	$(DOCKER_COMPOSE) -f docker/basic/docker-compose.yml down

.PHONY: docker-down-full
docker-down-full:
	$(DOCKER_COMPOSE) -f docker/full/docker-compose.yml down

.PHONY: docker-down-mock
docker-down-mock:
	$(DOCKER_COMPOSE) -f docker/mock/docker-compose.yml down

.PHONY: logs
logs:
	@echo "Choose a Docker environment to view logs from:"
	@echo "  make logs-basic    - View logs from basic environment"
	@echo "  make logs-full     - View logs from full environment"
	@echo "  make logs-mock     - View logs from mock environment"

.PHONY: logs-basic
logs-basic:
	docker logs taskinity-email-basic

.PHONY: logs-full
logs-full:
	docker logs taskinity-email-processor

.PHONY: logs-mock
logs-mock:
	docker logs taskinity-email-mock

# Test commands
.PHONY: test
test:
	$(PYTEST) tests/

.PHONY: test-unit
test-unit:
	$(PYTEST) tests/unit/

.PHONY: test-integration
test-integration:
	$(PYTEST) tests/integration/

# Utility commands
.PHONY: clean
clean:
	rm -rf __pycache__
	rm -rf .pytest_cache
	rm -rf logs/*.log
	rm -rf emails/attachments/*
	find . -name "*.pyc" -delete

.PHONY: lint
lint:
	flake8 *.py tasks/ utils/

.PHONY: format
format:
	black *.py tasks/ utils/

# Example-specific commands
.PHONY: run-imap-fetch
run-imap-fetch:
	$(PYTHON) tasks/fetch_emails.py

.PHONY: run-smtp-send
run-smtp-send:
	$(PYTHON) tasks/send_emails.py

.PHONY: run-classify
run-classify:
	$(PYTHON) tasks/classify_emails.py

.PHONY: run-process
run-process:
	$(PYTHON) tasks/process_emails.py

.PHONY: run-flow
run-flow:
	$(PYTHON) flow.py
