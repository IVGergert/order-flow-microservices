package com.gergert.deliveryservice.service;

import com.gergert.common.dto.kafka.DeliveryAssignedEventDto;
import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.deliveryservice.entity.Courier;
import com.gergert.deliveryservice.entity.CourierStatus;
import com.gergert.deliveryservice.entity.Delivery;
import com.gergert.deliveryservice.exception.NoCourierAvailableException;
import com.gergert.deliveryservice.repository.CourierRepository;
import com.gergert.deliveryservice.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final CourierRepository courierRepository;
    private final KafkaTemplate<String, DeliveryAssignedEventDto> kafkaTemplate;

    @Value("${kafka.topics.delivery-events}")
    private String deliveryEventsTopic;

    @Transactional
    public void createDelivery(OrderPaidEventDto eventDto) {

        if (deliveryRepository.findByOrderId(eventDto.orderId()).isPresent()){
            log.info("Delivery already assigned for orderId={}, skipping", eventDto.orderId());
            return;
        }

        Courier courier = courierRepository
                .findFirstByCourierStatus(CourierStatus.AVAILABLE)
                .orElseThrow(() -> {
                    log.warn("No available couriers for orderId={}. Retrying...", eventDto.orderId());
                    return new NoCourierAvailableException(eventDto.orderId());
                });

        courier.setCourierStatus(CourierStatus.BUSY);
        courierRepository.save(courier);

        Delivery delivery = Delivery.builder()
                .orderId(eventDto.orderId())
                .courier(courier)
                .etaMinutes(ThreadLocalRandom.current().nextInt(20,60))
                .build();

        var savedDelivery = deliveryRepository.save(delivery);

        DeliveryAssignedEventDto kafkaEvent = DeliveryAssignedEventDto.builder()
                .orderId(eventDto.orderId())
                .courierId(courier.getId())
                .courierName(courier.getName())
                .etaMinutes(savedDelivery.getEtaMinutes())
                .build();


        kafkaTemplate.send(
                deliveryEventsTopic,
                savedDelivery.getOrderId().toString(),
                kafkaEvent
        ).thenAccept(result ->
                log.info("DeliveryAssignedEvent sent for orderId={}", eventDto.orderId()));

        log.info("Delivery assigned: orderId={}, courier={}, eta={}min",
                eventDto.orderId(),
                courier.getName(),
                savedDelivery.getEtaMinutes());
    }
}
