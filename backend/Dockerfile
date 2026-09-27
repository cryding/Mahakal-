# Multi-stage production container for MAHAKAL Enterprise Cloud API
FROM python:3.11-slim

LABEL maintainer="MAHAKAL Infrastructure Engineering"
LABEL description="MAHAKAL Enterprise Virtual Coin Backend API Engine"

WORKDIR /app

# Ensure unbuffered logs and security hygiene
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PORT=8080 \
    APP_ENV=production

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy backend files into container
COPY . /app

# Create non-root application user
RUN groupadd -r mahakal && useradd -r -g mahakal mahakal \
    && chown -R mahakal:mahakal /app

USER mahakal

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/health || exit 1

# Launch server dynamically whether built from repo root or backend/ directory
CMD ["sh", "-c", "if [ -f backend/server.py ]; then exec python3 backend/server.py; else exec python3 server.py; fi"]
