# ── Build Stage ──────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from source for faster rebuilds
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Runtime Stage ─────────────────────────────────────────────────────────────
# Use minimal JRE — no compiler, no tools
FROM eclipse-temurin:21-jre-alpine

# Run as non-root user
RUN addgroup -S vault && adduser -S vault -G vault

WORKDIR /app

# Copy only the jar — no source, no test classes, no credentials
COPY --from=build /app/target/knowledge-vault-backend.jar app.jar

# Harden file permissions
RUN chown vault:vault app.jar && chmod 440 app.jar

USER vault

EXPOSE 8080 9090

# JVM hardening flags:
#  -Djava.security.egd — faster entropy for SecureRandom
#  -XX:+UseContainerSupport — honour cgroup memory limits
#  -XX:MaxRAMPercentage — use 75% of container memory max
#  -Dfile.encoding=UTF-8 — consistent encoding
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dfile.encoding=UTF-8", \
  "-jar", "app.jar"]
