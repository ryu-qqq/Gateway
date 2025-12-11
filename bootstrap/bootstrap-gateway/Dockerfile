# ============================================================================
# Connectly Gateway - Production Dockerfile
# Pre-built JAR packaging (JAR is built by CI before docker build)
# ============================================================================

FROM eclipse-temurin:21-jre-alpine

# Security: Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Install required packages
RUN apk add --no-cache \
    curl \
    tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# Copy the pre-built JAR (built by Gradle in CI)
COPY bootstrap/bootstrap-gateway/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod"

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
