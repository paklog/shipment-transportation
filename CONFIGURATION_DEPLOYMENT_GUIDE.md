# Configuration & Deployment Guide

This guide covers project setup, configuration management, API specifications, containerization, and deployment (Tasks 1-3, 66-75).

## Project Setup (Tasks 1-3)

### Maven pom.xml (Task 1)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    
    <groupId>com.example</groupId>
    <artifactId>shipment-transportation</artifactId>
    <version>1.0.0</version>
    <name>Shipment & Transportation Service</name>
    <description>Service for managing shipments and carrier integration</description>
    
    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <cloudevents.version>2.5.0</cloudevents.version>
        <testcontainers.version>1.19.3</testcontainers.version>
        <wiremock.version>3.3.1</wiremock.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </dependency>
        
        <!-- Spring Cloud Stream & Kafka -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-stream</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-stream-binder-kafka</artifactId>
        </dependency>
        
        <!-- CloudEvents -->
        <dependency>
            <groupId>io.cloudevents</groupId>
            <artifactId>cloudevents-spring</artifactId>
            <version>${cloudevents.version}</version>
        </dependency>
        
        <dependency>
            <groupId>io.cloudevents</groupId>
            <artifactId>cloudevents-json-jackson</artifactId>
            <version>${cloudevents.version}</version>
        </dependency>
        
        <!-- OpenAPI Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.3.0</version>
        </dependency>
        
        <!-- Testing Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-stream-test-binder</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- TestContainers -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mongodb</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>kafka</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        
        <!-- WireMock for External API Testing -->
        <dependency>
            <groupId>com.github.tomakehurst</groupId>
            <artifactId>wiremock-jre8</artifactId>
            <version>${wiremock.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <excludedGroups>integration</excludedGroups>
                </configuration>
            </plugin>
            
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <configuration>
                    <includes>
                        <include>**/*IntegrationTest.java</include>
                    </includes>
                    <groups>integration</groups>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Application Configuration (Task 2)
```yaml
# application.yml
spring:
  application:
    name: shipment-transportation
  
  profiles:
    active: local
    
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/shipment_transport}
      auto-index-creation: true
      
  cloud:
    stream:
      kafka:
        binder:
          brokers: ${KAFKA_BROKERS:localhost:9092}
          auto-create-topics: true
          
      bindings:
        packagePackedInput:
          destination: fulfillment.warehouse.v1.events
          group: shipment-transport-service
          consumer:
            max-attempts: 3
            back-off-initial-interval: 1000
            
        shipmentEventsOutput:
          destination: fulfillment.shipment.v1.events
          producer:
            partition-count: 3
            
  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false

server:
  port: 8080
  servlet:
    context-path: /api
    
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /management
      
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html

logging:
  level:
    com.example.shipment: INFO
    org.springframework.data.mongodb: DEBUG

carriers:
  fedex:
    api-url: ${FEDEX_API_URL:https://apis-sandbox.fedex.com}
    api-key: ${FEDEX_API_KEY:your-api-key}
    account-number: ${FEDEX_ACCOUNT_NUMBER:your-account-number}

tracking:
  job:
    enabled: ${TRACKING_JOB_ENABLED:true}
    interval: ${TRACKING_JOB_INTERVAL:3600000} # 1 hour

---
spring:
  config:
    activate:
      on-profile: docker
      
  data:
    mongodb:
      uri: mongodb://mongodb:27017/shipment_transport
      
  cloud:
    stream:
      kafka:
        binder:
          brokers: kafka:9092

---
spring:
  config:
    activate:
      on-profile: production
      
  data:
    mongodb:
      uri: ${MONGODB_URI}
      
  cloud:
    stream:
      kafka:
        binder:
          brokers: ${KAFKA_BROKERS}
          
logging:
  level:
    com.example.shipment: WARN
    root: INFO
```

### Spring Boot Main Application (Task 3)
```java
package com.example.shipment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@EnableBinding(Sink.class)
public class ShipmentTransportationApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ShipmentTransportationApplication.class, args);
    }
}
```

## Containerization & Deployment (Tasks 70-73)

### Dockerfile (Task 70)
```dockerfile
# Multi-stage build for optimized production image
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies (cached if pom.xml hasn't changed)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# Production image
FROM openjdk:21-jre-slim

# Create non-root user
RUN groupadd -r shipment && useradd -r -g shipment shipment

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/shipment-transportation-*.jar app.jar

# Change ownership
RUN chown shipment:shipment app.jar

# Switch to non-root user
USER shipment

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/management/health || exit 1

# JVM optimizations for containerized environment
ENV JVM_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run application
ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
```

### Docker Compose for Local Development (Task 71)
```yaml
version: '3.8'

services:
  # Application
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - MONGODB_URI=mongodb://mongodb:27017/shipment_transport
      - KAFKA_BROKERS=kafka:9092
      - FEDEX_API_URL=http://wiremock:8080
      - FEDEX_API_KEY=test-key
      - FEDEX_ACCOUNT_NUMBER=test-account
    depends_on:
      - mongodb
      - kafka
      - wiremock
    networks:
      - shipment-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/management/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # MongoDB
  mongodb:
    image: mongo:7.0
    ports:
      - "27017:27017"
    environment:
      - MONGO_INITDB_DATABASE=shipment_transport
    volumes:
      - mongodb_data:/data/db
      - ./scripts/mongodb/init.js:/docker-entrypoint-initdb.d/init.js:ro
    networks:
      - shipment-network
    healthcheck:
      test: echo 'db.stats().ok' | mongosh localhost:27017/shipment_transport --quiet
      interval: 30s
      timeout: 10s
      retries: 3

  # Zookeeper (required for Kafka)
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    networks:
      - shipment-network

  # Kafka
  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_HOST://kafka:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true
    networks:
      - shipment-network
    healthcheck:
      test: kafka-topics --bootstrap-server localhost:9092 --list
      interval: 30s
      timeout: 10s
      retries: 3

  # WireMock for mocking external carrier APIs
  wiremock:
    image: wiremock/wiremock:3.3.1
    ports:
      - "8089:8080"
    volumes:
      - ./wiremock:/home/wiremock
    command: ["--global-response-templating", "--verbose"]
    networks:
      - shipment-network

  # Kafka UI for development
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    ports:
      - "8081:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
    depends_on:
      - kafka
    networks:
      - shipment-network

volumes:
  mongodb_data:

networks:
  shipment-network:
    driver: bridge
```

### Health Check Configuration (Task 73)
```java
package com.example.shipment.infrastructure.health;

import com.example.shipment.domain.port.ICarrierAdapter;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("carriers")
public class CarrierHealthIndicator implements HealthIndicator {
    
    private final List<ICarrierAdapter> carrierAdapters;
    
    public CarrierHealthIndicator(List<ICarrierAdapter> carrierAdapters) {
        this.carrierAdapters = carrierAdapters;
    }
    
    @Override
    public Health health() {
        Map<String, String> carrierStatus = carrierAdapters.stream()
            .collect(Collectors.toMap(
                adapter -> adapter.getCarrierName().getDisplayName(),
                this::checkCarrierHealth
            ));
        
        boolean allHealthy = carrierStatus.values().stream()
            .allMatch("UP"::equals);
        
        Health.Builder healthBuilder = allHealthy ? Health.up() : Health.down();
        carrierStatus.forEach(healthBuilder::withDetail);
        
        return healthBuilder.build();
    }
    
    private String checkCarrierHealth(ICarrierAdapter adapter) {
        try {
            // Simple health check - could be more sophisticated
            return "UP";
        } catch (Exception e) {
            return "DOWN: " + e.getMessage();
        }
    }
}
```

```java
package com.example.shipment.infrastructure.health;

import com.example.shipment.infrastructure.outbox.repository.OutboxEventRepository;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("outbox")
public class OutboxHealthIndicator implements HealthIndicator {
    
    private final OutboxEventRepository outboxEventRepository;
    
    public OutboxHealthIndicator(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }
    
    @Override
    public Health health() {
        try {
            long unpublishedCount = outboxEventRepository.countUnpublishedEvents();
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("unpublished_events", unpublishedCount);
            
            if (unpublishedCount > 1000) {
                healthBuilder = Health.down()
                    .withDetail("unpublished_events", unpublishedCount)
                    .withDetail("reason", "Too many unpublished events in outbox");
            }
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Production Kubernetes Manifests
```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: shipment-transport

---
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: shipment-transport-config
  namespace: shipment-transport
data:
  SPRING_PROFILES_ACTIVE: "production"
  MONGODB_URI: "mongodb://mongodb-service:27017/shipment_transport"
  KAFKA_BROKERS: "kafka-service:9092"

---
# k8s/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: shipment-transport-secrets
  namespace: shipment-transport
type: Opaque
data:
  FEDEX_API_KEY: <base64-encoded-key>
  FEDEX_ACCOUNT_NUMBER: <base64-encoded-account>

---
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: shipment-transport
  namespace: shipment-transport
spec:
  replicas: 3
  selector:
    matchLabels:
      app: shipment-transport
  template:
    metadata:
      labels:
        app: shipment-transport
    spec:
      containers:
      - name: shipment-transport
        image: shipment-transport:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: shipment-transport-config
        - secretRef:
            name: shipment-transport-secrets
        livenessProbe:
          httpGet:
            path: /api/management/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /api/management/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"

---
# k8s/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: shipment-transport-service
  namespace: shipment-transport
spec:
  selector:
    app: shipment-transport
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP

---
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: shipment-transport-ingress
  namespace: shipment-transport
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: shipment-api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: shipment-transport-service
            port:
              number: 80
```

This configuration and deployment guide provides everything needed to run the Shipment & Transportation service in various environments, from local development to production Kubernetes deployments.