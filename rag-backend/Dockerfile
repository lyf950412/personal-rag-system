# Multi-stage build for Spring Boot application
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create uploads directory as root
RUN mkdir -p /app/uploads

# Add non-root user for security
RUN addgroup -S ragapp && adduser -S ragapp -G ragapp
RUN chown -R ragapp:ragapp /app/uploads

# Copy the built jar
COPY --from=builder /app/target/*.jar app.jar

USER ragapp

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set timezone
ENV TZ=Asia/Shanghai

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
