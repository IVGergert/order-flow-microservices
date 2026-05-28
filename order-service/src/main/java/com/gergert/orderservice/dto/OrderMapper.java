package com.gergert.orderservice.dto;

import com.gergert.orderservice.entity.Order;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {
    Order toEntity(CreateOrderRequestDto requestDto);

    @AfterMapping
    default void linkItems(@MappingTarget Order order) {
        order.getItems().forEach(item -> item.setOrder(order));
    }

    OrderDto toOrderDto(Order order);
}