package com.gergert.deliveryservice.exception;

public class DeliveryAccessDeniedException extends RuntimeException {
    public DeliveryAccessDeniedException(String message) {
        super(message);
    }
}
