# Stage 1: Build the application
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# Copy maven wrapper and pom.xml to leverage Docker layer caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the application JAR
RUN ./mvnw package -DskipTests

# Stage 2: Create the final, smaller image
FROM openjdk:17-jre-slim

WORKDIR /app

# Create a non-root user for security
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy the executable JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Run the application
ENTRYPOINT ["java","-jar","app.jar"]
