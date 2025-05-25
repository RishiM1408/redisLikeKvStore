# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app
COPY --from=builder /app/target/redis-like-kvstore-1.0-SNAPSHOT.jar ./app.jar

# Create logs directory
RUN mkdir -p /app/logs

# Expose Redis default port
EXPOSE 6379

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]