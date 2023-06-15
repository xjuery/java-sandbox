package org.example;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class KafkaLogbackAppender extends AppenderBase<ILoggingEvent> {
    private String bootstrapServers;
    private String topic;
    private Producer<String, String> producer;

    @Override
    public void start() {
        super.start();

        // Configure Kafka producer properties
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(props);
    }

    @Override
    protected void append(ILoggingEvent event) {
        // Create a Kafka record with log message
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getFormattedMessage());

        // Send the record to Kafka producer
        producer.send(record);
    }

    @Override
    public void stop() {
        super.stop();
        producer.close();
    }

    // Setters for configuration properties
    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}

