package com.paklog.shipment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shipment.events")
public class ShipmentEventProperties {

    private EventProperties dispatched = new EventProperties();
    private EventProperties delivered = new EventProperties();

    public EventProperties getDispatched() {
        return dispatched;
    }

    public void setDispatched(EventProperties dispatched) {
        this.dispatched = dispatched;
    }

    public EventProperties getDelivered() {
        return delivered;
    }

    public void setDelivered(EventProperties delivered) {
        this.delivered = delivered;
    }

    public static class EventProperties {
        private String type;
        private String topic;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }
}
