# Stage 1: Build Spring Boot
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copy Gradle configuration
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle.kts .
COPY backend/build.gradle.kts backend/

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Copy backend source code
COPY backend/src/ backend/src/

# Build executable JAR
RUN ./gradlew :backend:bootJar --no-daemon

# Stage 2: Run Spring Boot
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Run as a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /app/backend/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]