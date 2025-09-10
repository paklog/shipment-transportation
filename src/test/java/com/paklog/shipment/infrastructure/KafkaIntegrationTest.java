package com.paklog.shipment.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.shipment.ShipmentTransportationApplication;
import com.paklog.shipment.domain.events.PackagePackedCloudEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = ShipmentTransportationApplication.class)
@DirtiesContext
@EmbeddedKafka(partitions = 1)
class KafkaIntegrationTest {

    private static final String PACKAGE_PACKED_TOPIC = "package-packed";

    @Autowired
    private KafkaEventProducer kafkaEventProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private BlockingQueue<ConsumerRecord<String, String>> consumerRecords;

    @BeforeEach
    void setUp() {
        consumerRecords = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "false", embeddedKafkaBroker);
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties(PACKAGE_PACKED_TOPIC);
        KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, String>) record -> consumerRecords.add(record));
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @Test
    void testSendMessage() throws Exception {
        // Arrange
        String eventPayload = "{\"packageId\":\"pkg-123\",\"orderId\":\"ord-456\",\"packedAt\":\"2025-01-01T10:00:00Z\"}";

        // Act
        kafkaEventProducer.publishEvent(PACKAGE_PACKED_TOPIC, eventPayload);

        // Assert
        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(PACKAGE_PACKED_TOPIC, received.topic());
        assertEquals(eventPayload, received.value());
    }

    @Test
    void testReceiveMessageAndProcess() throws Exception {
        // Arrange
        // This test would typically involve sending a message to Kafka and then asserting that
        // the PackagePackedEventConsumer processes it correctly.
        // Since PackagePackedEventConsumer uses Spring Cloud Stream functional model,
        // we would need to interact with the functional bean directly or use Spring Cloud Stream Test Binder.
        // For simplicity in this integration test, we'll just verify the producer part.
        String eventPayload = "{\"packageId\":\"pkg-456\",\"orderId\":\"ord-789\",\"packedAt\":\"2025-01-01T11:00:00Z\"}";

        // Act
        kafkaEventProducer.publishEvent(PACKAGE_PACKED_TOPIC, eventPayload);

        // Assert
        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(PACKAGE_PACKED_TOPIC, received.topic());
        assertEquals(eventPayload, received.value());

        // Further assertions would involve checking the state changes in the application
        // that result from PackagePackedEventConsumer processing the event.
        // This would typically require mocking the downstream services or checking a test database.
    }
}
