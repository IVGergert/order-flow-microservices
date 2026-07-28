package com.gergert.deliveryservice.service.impl;

import com.gergert.common.dto.kafka.DeliveryAssignedEventDto;
import com.gergert.common.dto.kafka.OrderDeliveredEventDto;
import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.common.dto.kafka.OrderPickedUpEventDto;
import com.gergert.deliveryservice.entity.*;
import com.gergert.deliveryservice.exception.NoCourierAvailableException;
import com.gergert.deliveryservice.repository.CourierRepository;
import com.gergert.deliveryservice.repository.DeliveryRepository;
import com.gergert.deliveryservice.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final CourierRepository courierRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.delivery-assigned-events}")
    private String deliveryEventsTopic;

    @Value("${kafka.topics.order-picked-up-events}")
    private String orderPickedUpTopic;

    @Value("${kafka.topics.delivery-completed-events}")
    private String deliveryCompletedTopic;

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

        courier.setCourierStatus(CourierStatus.ON_THE_WAY_TO_RESTAURANT);
        courierRepository.save(courier);

        Delivery delivery = Delivery.builder()
                .orderId(eventDto.orderId())
                .courier(courier)
                .deliveryStatus(DeliveryStatus.COURIER_ASSIGNED)
                .etaMinutes(ThreadLocalRandom.current().nextInt(20,60))
                .build();

        var savedDelivery = deliveryRepository.save(delivery);

        DeliveryAssignedEventDto kafkaEvent = DeliveryAssignedEventDto.builder()
                .orderId(eventDto.orderId())
                .courierId(courier.getId())
                .courierName(courier.getName())
                .address(eventDto.address())
                .etaMinutes(savedDelivery.getEtaMinutes())
                .build();


        kafkaTemplate.send(
                deliveryEventsTopic,
                savedDelivery.getOrderId().toString(),
                kafkaEvent
        );

        log.info("Delivery assigned: orderId={}, courier={}, eta={}min",
                eventDto.orderId(),
                courier.getName(),
                savedDelivery.getEtaMinutes());
    }

    @Transactional
    @Override
    public void pickUpOrder(Long orderId, Long courierUserId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Delivery not found for orderId=" + orderId)
                );

        if (!delivery.getCourier().getId().equals(courierUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot pick up someone else's order!");
        }

        Courier courier = delivery.getCourier();

        delivery.setDeliveryStatus(DeliveryStatus.PICKED_UP);
        courier.setCourierStatus(CourierStatus.ON_THE_WAY_TO_CUSTOMER);

        deliveryRepository.save(delivery);
        courierRepository.save(courier);

        log.info("Order {} picked up by courier {}. On the way to customer!", orderId, courier.getName());

        kafkaTemplate.send(orderPickedUpTopic, orderId.toString(), new OrderPickedUpEventDto(orderId));
    }

    @Transactional
    @Override
    public void completeDelivery(Long orderId, Long courierUserId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Delivery not found for orderId=" + orderId)
                );

        if (!delivery.getCourier().getId().equals(courierUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot complete someone else's delivery!");
        }

        Courier courier = delivery.getCourier();

        delivery.setDeliveryStatus(DeliveryStatus.DELIVERED);
        courier.setCourierStatus(CourierStatus.AVAILABLE);

        deliveryRepository.save(delivery);
        courierRepository.save(courier);

        log.info("Order {} delivered successfully by courier {}.", orderId, courier.getName());

        kafkaTemplate.send(deliveryCompletedTopic, orderId.toString(), new OrderDeliveredEventDto(orderId));
    }
}
