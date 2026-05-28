package com.gergert.orderservice.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    DELIVERY_ASSIGNED,
    PAYMENT_FAILED,
    DELIVERED
}
