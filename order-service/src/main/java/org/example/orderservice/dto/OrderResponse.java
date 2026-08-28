package org.example.orderservice.dto;

import lombok.Builder;
import org.example.orderservice.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public class OrderResponse {

    private Long id;

    private Long customerId;

    private Long productId;

    private Integer quantity;

    private BigDecimal totalPrice;

    private OrderStatus status;

    private LocalDate createdAt;
}