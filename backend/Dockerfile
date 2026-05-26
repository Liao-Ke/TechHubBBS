# ===============================================================
# TechHub Backend — Multi-stage Docker Build
# ===============================================================
# Stage 1: Build with Maven
# ===============================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Cache Maven dependencies (layer stays cached if pom.xml unchanged)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# ===============================================================
# Stage 2: Runtime with JRE
# ===============================================================
FROM eclipse-temurin:17-jre AS runtime

# Health check dependency
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Non-root user for security
RUN groupadd -r techhub -g 1000 && \
    useradd -r -g techhub -u 1000 -m -d /app techhub

WORKDIR /app

# Copy the built artifact from builder stage
COPY --from=builder /build/target/techhub-backend-0.0.1-SNAPSHOT.jar app.jar

USER techhub

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=40s \
  CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
