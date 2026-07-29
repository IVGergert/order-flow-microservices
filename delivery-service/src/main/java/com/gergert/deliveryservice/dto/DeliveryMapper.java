package com.gergert.deliveryservice.dto;

import com.gergert.deliveryservice.entity.Delivery;
import org.mapstruct.*;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)

public interface DeliveryMapper {

    @Mapping(source = "courier.id", target = "courierId")
    @Mapping(source = "courier.name", target = "courierName")
    DeliveryResponseDto toDeliveryDto(Delivery delivery);
}
