package com.gergert.deliveryservice.controller;

import com.gergert.deliveryservice.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService deliveryService;

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
