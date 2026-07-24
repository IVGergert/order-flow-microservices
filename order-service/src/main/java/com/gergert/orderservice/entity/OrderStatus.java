package com.gergert.orderservice.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_FAILED,
    PAID,
    DELIVERY_ASSIGNED,
    IN_DELIVERY,
    DELIVERED,
    CANCELLED
}
