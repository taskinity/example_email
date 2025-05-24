FROM groovy:4.0-jdk17

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy application files
COPY email.groovy .
COPY .env .

# Create logs directory
RUN mkdir -p /app/logs

# Set environment variables
ENV GROOVY_OPTS="-Dfile.encoding=UTF-8"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Default command
CMD ["groovy", "email.groovy"]