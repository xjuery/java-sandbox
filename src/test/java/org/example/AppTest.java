package org.example;

import com.github.dockerjava.api.model.Bind;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Test;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test for simple App.
 */
@Slf4j
public class AppTest 
    extends TestCase
{

    @Test
    public void testApp()
    {
        try(KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))) {
            kafka.setPortBindings(Arrays.asList("9092:9092"));
            kafka.start();
            createTopics(kafka.getBootstrapServers(), "mymetrics");

            log.error("Test");

            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                    ImmutableMap.of(
                            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                            kafka.getBootstrapServers(),
                            ConsumerConfig.GROUP_ID_CONFIG,
                            "tc-" + UUID.randomUUID(),
                            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                            "earliest"
                    ),
                    new StringDeserializer(),
                    new StringDeserializer()
            );
            consumer.subscribe(Collections.singletonList("mymetrics"));
            Unreliables.retryUntilTrue(
                    10,
                    TimeUnit.SECONDS,
                    () -> {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                        if (records.isEmpty()) {
                            return false;
                        }

                        assertEquals("Test", records);

                        return true;
                    }
            );

            assertTrue(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void createTopics(String bootstrapServers, String topicName) throws Exception {
        try (
                AdminClient adminClient = AdminClient.create(
                        ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                );
        ) {
            Collection<NewTopic> topics = Collections.singletonList(new NewTopic(topicName, 1, (short) 1));
            adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
        }
    }
}
