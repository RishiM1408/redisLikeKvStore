FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Create the runtime image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=builder /app/target/redis-like-kvstore-1.0-SNAPSHOT.jar ./app.jar

# Create logs directory
RUN mkdir -p /app/logs

# Expose Redis default port
EXPOSE 6379

# Set environment variables
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC"

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 