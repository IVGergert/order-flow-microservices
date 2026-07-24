package com.gergert.deliveryservice.exception;

public class NoCourierAvailableException extends RuntimeException {
    public NoCourierAvailableException(Long orderId) {
        super("No available couriers for order: " + orderId);
    }
}
