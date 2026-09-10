package com.gergert.deliveryservice.kafka;

import com.gergert.common.dto.kafka.CourierCreatedEventDto;
import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.deliveryservice.entity.Courier;
import com.gergert.deliveryservice.entity.CourierStatus;
import com.gergert.deliveryservice.entity.TransportType;
import com.gergert.deliveryservice.repository.CourierRepository;
import com.gergert.deliveryservice.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CourierCreatedListener {
    private final CourierRepository courierRepository;

    @KafkaListener(
            topics = "${kafka.topics.courier-created-events}",
            groupId = "delivery-courier-group"
    )
    public void handle(CourierCreatedEventDto eventDto) {
        log.info(
                "Received CourierCreatedEvent: userId={}, email={}",
                eventDto.userId(),
                eventDto.email()
        );

        if (courierRepository.findByUserId(eventDto.userId()).isPresent()) {
            log.warn("Courier already exists for userId={}", eventDto.userId());
            return;
        }

        Courier courier = Courier.builder()
                .userId(eventDto.userId())
                .name(eventDto.name())
                .transportType(TransportType.valueOf(eventDto.transportType()))
                .courierStatus(CourierStatus.OFFLINE)
                .build();

        courierRepository.save(courier);

        log.info("Courier created successfully: userId={}, name={}",
                eventDto.userId(),
                eventDto.name());
    }
}
