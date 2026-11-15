# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Render has no native Java/JVM runtime, so a Dockerfile is required.
# Multi-stage build: Maven + JDK 21 to compile, JRE 21 to run.
# ---------------------------------------------------------------------------

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
# Tests need a live MySQL (and Docker for Testcontainers), neither available at
# image-build time; they run separately in CI. Build the runnable jar only.
RUN mvn -B -ntp clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

# Production profile supplies the Aiven TLS/JDBC overrides (application-prod.properties).
ENV SPRING_PROFILES_ACTIVE=prod

# Render injects PORT (default 10000) and routes traffic to it.
EXPOSE 8082
ENTRYPOINT ["sh", "-c", "exec java -jar app.jar --server.port=${PORT:-8082}"]
