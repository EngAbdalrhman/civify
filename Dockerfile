# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copy maven wrapper and pom.xml first to cache dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copy source code and build
COPY src ./src
ARG PROFILE=dev
RUN ./mvnw clean package -P${PROFILE} -DskipTests -Dgraphqlcodegen.skip=true

# Stage 2: Run the application
# Use JRE instead of JDK for smaller image and security (no source code)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8077

ENTRYPOINT ["java", "-jar", "app.jar"]
