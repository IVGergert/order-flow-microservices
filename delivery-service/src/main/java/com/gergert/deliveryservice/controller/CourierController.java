package com.gergert.deliveryservice.controller;

import com.gergert.common.dto.jwt.JwtClaimsDto;
import com.gergert.deliveryservice.dto.CourierStatusResponseDto;
import com.gergert.deliveryservice.service.CourierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class CourierController {

    private final CourierService courierService;

    @PostMapping("/courier/go-online")
    public ResponseEntity<CourierStatusResponseDto> goOnline(@AuthenticationPrincipal JwtClaimsDto claims) {
        return ResponseEntity.ok(courierService.goOnline(claims.userId()));
    }

    @PostMapping("/courier/go-offline")
    public ResponseEntity<CourierStatusResponseDto> goOffline(@AuthenticationPrincipal JwtClaimsDto claims) {
        return ResponseEntity.ok(courierService.goOffline(claims.userId()));
    }

    @GetMapping("/courier/status")
    public ResponseEntity<CourierStatusResponseDto> getStatus(@AuthenticationPrincipal JwtClaimsDto claims) {
        return ResponseEntity.ok(courierService.getStatus(claims.userId()));
    }
}
