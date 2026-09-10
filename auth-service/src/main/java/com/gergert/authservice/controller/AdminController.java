package com.gergert.authservice.controller;

import com.gergert.authservice.dto.CreateCourierRequestDto;
import com.gergert.authservice.dto.UserResponseDto;
import com.gergert.authservice.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @PostMapping("/couriers")
    public ResponseEntity<UserResponseDto> createCourier(@Valid @RequestBody CreateCourierRequestDto request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(adminService.createCourier(request));
    }

}
