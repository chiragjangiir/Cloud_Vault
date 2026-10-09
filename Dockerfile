# syntax=docker/dockerfile:1
# ===========================================================================
# Cloud Vault — production image (§49)
# Multi-stage: Maven builds the fat JAR, a slim JRE runs it as a NON-ROOT user.
# The storage directory is NEVER baked into the image; it is provided at
# run time through STORAGE_ROOT (bind mount / volume).
# ===========================================================================

# ---- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
# Warm the dependency layer so source-only changes rebuild fast
RUN mvn -q -B dependency:go-offline || true
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

# curl is used only for the container HEALTHCHECK
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home cloudvault

WORKDIR /app
COPY --from=build /build/target/cloud-vault-*.jar /app/app.jar

# Defaults; every value can be overridden at run time (§59)
ENV STORAGE_ROOT=/data/storage \
    SERVER_PORT=8080 \
    JAVA_OPTS=""

RUN mkdir -p /data/storage && chown -R cloudvault:cloudvault /data/storage /app

# Never run as root (§51)
USER cloudvault

VOLUME ["/data/storage"]
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
