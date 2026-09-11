package com.gergert.deliveryservice.service.impl;

import com.gergert.common.dto.kafka.DeliveryAssignedEventDto;
import com.gergert.common.dto.kafka.OrderDeliveredEventDto;
import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.common.dto.kafka.OrderPickedUpEventDto;
import com.gergert.deliveryservice.dto.CourierStatisticsResponseDto;
import com.gergert.deliveryservice.dto.DeliveryMapper;
import com.gergert.deliveryservice.dto.DeliveryResponseDto;
import com.gergert.deliveryservice.entity.*;
import com.gergert.deliveryservice.exception.*;
import com.gergert.deliveryservice.repository.CourierRepository;
import com.gergert.deliveryservice.repository.DeliveryRepository;
import com.gergert.deliveryservice.service.DeliveryService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final CourierRepository courierRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final DeliveryMapper deliveryMapper;

    @Value("${kafka.topics.delivery-assigned-events}")
    private String deliveryEventsTopic;

    @Value("${kafka.topics.order-picked-up-events}")
    private String orderPickedUpTopic;

    @Value("${kafka.topics.delivery-completed-events}")
    private String deliveryCompletedTopic;

    @Override
    @Transactional
    public void createDelivery(OrderPaidEventDto eventDto) {

        if (deliveryRepository.findByOrderId(eventDto.orderId()).isPresent()) {
            log.info("Delivery already assigned for order {}", eventDto.orderId());
            return;
        }

        Delivery delivery = Delivery.builder()
                .orderId(eventDto.orderId())
                .deliveryStatus(DeliveryStatus.WAITING_FOR_COURIER)
                .address(eventDto.address())
                .etaMinutes(ThreadLocalRandom.current().nextInt(20, 60))
                .build();

        Delivery saved = deliveryRepository.save(delivery);

        log.info("Delivery created for order {}", saved.getOrderId());
    }

    @Override
    @Transactional
    public DeliveryResponseDto acceptDelivery(Long orderId, Long courierUserId) {
        Delivery delivery = findDeliveryByOrderId(orderId);

        if (delivery.getDeliveryStatus() != DeliveryStatus.WAITING_FOR_COURIER) {
            throw new InvalidDeliveryStatusException("Delivery has already been accepted.");
        }

        Courier courier = getCourierByUserId(courierUserId);

        if (courier.getCourierStatus() != CourierStatus.AVAILABLE) {
            throw new CourierNotAvailableException("Courier is not available.");
        }

        delivery.setCourier(courier);
        delivery.setDeliveryStatus(DeliveryStatus.COURIER_ASSIGNED);

        courier.setCourierStatus(CourierStatus.ON_THE_WAY_TO_RESTAURANT);

        deliveryRepository.save(delivery);
        courierRepository.save(courier);

        DeliveryAssignedEventDto kafkaEvent = DeliveryAssignedEventDto.builder()
                .orderId(delivery.getOrderId())
                .courierId(courier.getId())
                .courierName(courier.getName())
                .address(delivery.getAddress())
                .etaMinutes(delivery.getEtaMinutes())
                .build();

        kafkaTemplate.send(
                deliveryEventsTopic,
                delivery.getOrderId().toString(),
                kafkaEvent
        );

        log.info("Courier {} accepted order {}", courier.getName(), orderId);

        return deliveryMapper.toDeliveryDto(delivery);
    }

    @Override
    @Transactional
    public DeliveryResponseDto pickUpOrder(Long orderId, Long courierUserId) {
        Delivery delivery = findDeliveryByOrderId(orderId);

        checkCourierAccess(delivery, courierUserId);

        if (delivery.getDeliveryStatus() != DeliveryStatus.COURIER_ASSIGNED) {
            throw new InvalidDeliveryStatusException("Cannot pick up order with status: " + delivery.getDeliveryStatus());
        }

        Courier courier = delivery.getCourier();

        delivery.setDeliveryStatus(DeliveryStatus.PICKED_UP);
        courier.setCourierStatus(CourierStatus.ON_THE_WAY_TO_CUSTOMER);

        deliveryRepository.save(delivery);
        courierRepository.save(courier);

        log.info("Order {} picked up by courier {}. On the way to customer!", orderId, courier.getName());

        kafkaTemplate.send(
                orderPickedUpTopic,
                orderId.toString(),
                new OrderPickedUpEventDto(orderId));

        return deliveryMapper.toDeliveryDto(delivery);
    }

    @Override
    @Transactional
    public DeliveryResponseDto completeDelivery(Long orderId, Long courierUserId) {
        Delivery delivery = findDeliveryByOrderId(orderId);

        checkCourierAccess(delivery, courierUserId);

        Courier courier = delivery.getCourier();

        if (delivery.getDeliveryStatus() != DeliveryStatus.PICKED_UP) {
            throw new InvalidDeliveryStatusException(
                    "Cannot complete delivery before picking up order from restaurant!");
        }

        delivery.setDeliveryStatus(DeliveryStatus.DELIVERED);
        delivery.setCompletedAt(LocalDateTime.now());

        courier.setCourierStatus(CourierStatus.AVAILABLE);

        deliveryRepository.save(delivery);
        courierRepository.save(courier);

        log.info("Order {} delivered successfully by courier {}.", orderId, courier.getName());

        kafkaTemplate.send(
                deliveryCompletedTopic,
                orderId.toString(),
                new OrderDeliveredEventDto(orderId));

        return deliveryMapper.toDeliveryDto(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryResponseDto> getCurrentDeliveryByCourierUserId(Long courierUserId) {
        Set<DeliveryStatus> activeStatuses = Set.of(
                DeliveryStatus.COURIER_ASSIGNED,
                DeliveryStatus.PICKED_UP
        );

        return deliveryRepository
                .findFirstByCourier_UserIdAndDeliveryStatusIn(courierUserId, activeStatuses)
                .map(deliveryMapper::toDeliveryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResponseDto> getDeliveriesByCourierUserId(Long courierUserId) {
        log.info("Fetching deliveries for courier with userId={}", courierUserId);

        List<Delivery> deliveries = deliveryRepository.findAllByCourier_UserId(courierUserId);

        return deliveries.stream()
                .map(deliveryMapper::toDeliveryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryResponseDto getDeliveryByOrderId(Long orderId, Long courierUserId) {
        Delivery delivery = findDeliveryByOrderId(orderId);

        checkCourierAccess(delivery, courierUserId);

        return deliveryMapper.toDeliveryDto(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResponseDto> getWaitingDeliveries(Long courierUserId) {

        Courier courier = getCourierByUserId(courierUserId);

        if (courier.getCourierStatus() != CourierStatus.AVAILABLE) {
            throw new CourierNotAvailableException("Courier is not available.");
        }

        log.info("Fetching waiting deliveries for courier {}", courierUserId);

        return deliveryRepository.findAllByDeliveryStatus(DeliveryStatus.WAITING_FOR_COURIER)
                .stream()
                .map(deliveryMapper::toDeliveryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourierStatisticsResponseDto getCompletedDeliveriesToday(Long courierUserId) {
        getCourierByUserId(courierUserId);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        var completedToday = deliveryRepository
                .countByCourier_UserIdAndCompletedAtBetween(
                        courierUserId,
                        startOfDay,
                        endOfDay
                );

        return new CourierStatisticsResponseDto(completedToday);
    }

    private Delivery findDeliveryByOrderId(Long orderId) {
        return deliveryRepository
                .findByOrderId(orderId)
                .orElseThrow(() -> new DeliveryNotFoundException("Delivery not found for orderId=" + orderId));
    }

    private Courier getCourierByUserId(Long courierUserId) {
        return courierRepository
                .findByUserId(courierUserId)
                .orElseThrow(() -> new CourierNotFoundException("Courier not found."));
    }

    private void checkCourierAccess(Delivery delivery, Long courierUserId) {

        if (delivery.getCourier() == null
                || !delivery.getCourier()
                .getUserId()
                .equals(courierUserId)) {

            throw new DeliveryAccessDeniedException("You cannot access someone else's delivery!");
        }
    }
}
