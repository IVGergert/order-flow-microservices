package com.gergert.deliveryservice.service;

import com.gergert.common.dto.kafka.DeliveryAssignedEventDto;
import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.deliveryservice.entity.Courier;
import com.gergert.deliveryservice.entity.CourierStatus;
import com.gergert.deliveryservice.entity.Delivery;
import com.gergert.deliveryservice.repository.CourierRepository;
import com.gergert.deliveryservice.repository.DeliveryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@AllArgsConstructor
public class DeliveryService {
    private static final String DELIVERY_EVENTS_TOPIC = "delivery.events";

    private final DeliveryRepository deliveryRepository;
    private final CourierRepository courierRepository;
    private final KafkaTemplate<String, DeliveryAssignedEventDto> kafkaTemplate;

    public void createDelivery(OrderPaidEventDto eventDto){
        Courier courier = courierRepository
                .findFirstByCourierStatus(CourierStatus.AVAILABLE)
                        .orElse(null);

        if (courier == null){
            log.warn("No available couriers for order {}", eventDto.orderId());
            return;
        }

        courier.setCourierStatus(CourierStatus.BUSY);
        courierRepository.save(courier);

        var etaMinutes = ThreadLocalRandom.current().nextInt(20,60);

        Delivery delivery = Delivery.builder()
                .orderId(eventDto.orderId())
                .courier(courier)
                .etaMinutes(etaMinutes)
                .build();

        var savedDelivery = deliveryRepository.save(delivery);

        DeliveryAssignedEventDto kafkaEvent = DeliveryAssignedEventDto.builder()
                .orderId(eventDto.orderId())
                .courierId(courier.getId())
                .courierName(courier.getName())
                .etaMinutes(savedDelivery.getEtaMinutes())
                .build();


        kafkaTemplate.send(
                DELIVERY_EVENTS_TOPIC,
                savedDelivery.getOrderId().toString(),
                kafkaEvent
        );

        log.info("Delivery assigned for order: {}", eventDto.orderId());
    }
}
