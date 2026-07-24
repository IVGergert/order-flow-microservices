package com.gergert.paymentservice.dto;

import com.gergert.common.dto.CreatePaymentRequestDto;
import com.gergert.common.dto.CreatePaymentResponseDto;
import com.gergert.paymentservice.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)

public interface PaymentMapper {
    Payment toEntity(CreatePaymentRequestDto requestDto);

    @Mapping(source = "id", target = "paymentId")
    @Mapping(source = "paymentStatus", target = "paymentStatus")
    CreatePaymentResponseDto toResponseDto(Payment payment);
}
