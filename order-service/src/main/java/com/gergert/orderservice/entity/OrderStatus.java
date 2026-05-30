package com.gergert.orderservice.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    DELIVERY_ASSIGNED,
    DELIVERED
}
