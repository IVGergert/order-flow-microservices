package com.gergert.deliveryservice.controller;

import com.gergert.common.dto.jwt.JwtClaimsDto;
import com.gergert.deliveryservice.dto.CourierStatisticsResponseDto;
import com.gergert.deliveryservice.dto.DeliveryResponseDto;
import com.gergert.deliveryservice.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService deliveryService;

    @GetMapping("/current")
    public ResponseEntity<DeliveryResponseDto> getCurrentDelivery(@AuthenticationPrincipal JwtClaimsDto claims) {
        Optional<DeliveryResponseDto> delivery = deliveryService.getCurrentDeliveryByCourierUserId(claims.userId());

        return delivery.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());

    }

    @GetMapping("/statistics/today")
    public ResponseEntity<CourierStatisticsResponseDto> getCompletedToday(@AuthenticationPrincipal JwtClaimsDto claims) {
        return ResponseEntity.ok(deliveryService.getCompletedDeliveriesToday(claims.userId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<DeliveryResponseDto>> getMyDeliveries(@AuthenticationPrincipal JwtClaimsDto claims) {
        return ResponseEntity.ok(deliveryService.getDeliveriesByCourierUserId(claims.userId()));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryResponseDto> getDeliveryByOrderId(@PathVariable Long orderId,
                                                         @AuthenticationPrincipal JwtClaimsDto claims) {

        return ResponseEntity.ok(deliveryService.getDeliveryByOrderId(orderId, claims.userId()));
    }

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<DeliveryResponseDto> acceptDelivery(@PathVariable Long orderId,
                                                              @AuthenticationPrincipal JwtClaimsDto claims) {
        return ResponseEntity.ok(deliveryService.acceptDelivery(orderId, claims.userId()));
    }

    @PostMapping("/{orderId}/pickup")
    public ResponseEntity<DeliveryResponseDto> pickUpOrder(@PathVariable Long orderId,
                                              @AuthenticationPrincipal JwtClaimsDto claims) {
        return ResponseEntity.ok(deliveryService.pickUpOrder(orderId, claims.userId()));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<DeliveryResponseDto> completeDelivery(@PathVariable Long orderId,
                                                   @AuthenticationPrincipal JwtClaimsDto claims) {
        return ResponseEntity.ok(deliveryService.completeDelivery(orderId, claims.userId()));
    }

    @GetMapping("/waiting")
    public ResponseEntity<List<DeliveryResponseDto>> getWaitingDeliveries(@AuthenticationPrincipal JwtClaimsDto claims) {
        return ResponseEntity.ok(deliveryService.getWaitingDeliveries(claims.userId()));
    }
}
