package com.gergert.authservice.kafka;

import com.gergert.common.dto.kafka.CourierCreatedEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourierCreatedEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.courier-created-events}")
    private String topic;

    public void send(CourierCreatedEventDto event) {
        kafkaTemplate.send(
                topic,
                event.userId().toString(),
                event
        );

        log.info(
                "CourierCreatedEvent sent: userId={}, email={}",
                event.userId(),
                event.email()
        );
    }
}
