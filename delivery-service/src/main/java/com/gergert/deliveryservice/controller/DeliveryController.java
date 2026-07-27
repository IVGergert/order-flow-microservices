package com.gergert.deliveryservice.controller;

import com.gergert.deliveryservice.entity.Delivery;
import com.gergert.deliveryservice.repository.DeliveryRepository;
import com.gergert.deliveryservice.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService deliveryService;
    private final DeliveryRepository deliveryRepository;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Delivery> getDeliveryByOrderId(@PathVariable Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Delivery not found for orderId=" + orderId));
        return ResponseEntity.ok(delivery);
    }

    @PostMapping("/{orderId}/pickup")
    public ResponseEntity<String> pickUpOrder(@PathVariable Long orderId) {
        deliveryService.pickUpOrder(orderId);
        return ResponseEntity.ok("Order " + orderId + " picked up. On the way to customer!");
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<String> completeDelivery(@PathVariable Long orderId) {
        deliveryService.completeDelivery(orderId);
        return ResponseEntity.ok("Order " + orderId + " delivered successfully!");
    }
}
