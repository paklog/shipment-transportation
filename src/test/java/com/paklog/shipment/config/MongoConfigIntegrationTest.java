package com.paklog.shipment.config;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataMongoTest
@Testcontainers
class MongoConfigIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:latest"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void testMongoTemplateBeanCreation() {
        // Assert that MongoTemplate bean is created
        assertNotNull(mongoTemplate);
    }

    @Test
    void testMongoClientConnection() {
        // Assert that MongoClient bean is created and can connect
        MongoClient mongoClient = applicationContext.getBean(MongoClient.class);
        assertNotNull(mongoClient);
        // Attempt a simple operation to verify connection
        assertTrue(mongoClient.listDatabaseNames().into(new java.util.ArrayList<>()).size() >= 0);
    }
}
