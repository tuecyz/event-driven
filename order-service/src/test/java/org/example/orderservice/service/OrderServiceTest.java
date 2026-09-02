package org.example.orderservice.service;

import org.example.orderservice.dto.CreateOrderRequestDTO;
import org.example.orderservice.dto.OrderResponseDTO;
import org.example.orderservice.entity.OrderEntity;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.exception.OrderNotFoundException;
import org.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequestDTO request;
    private OrderEntity savedOrder;

    @BeforeEach
    void setUp() {
        request = new CreateOrderRequestDTO();
        request.setCustomerId(1L);
        request.setProductId(100L);
        request.setQuantity(2);
        request.setTotalPrice(new BigDecimal("250.00"));

        savedOrder = OrderEntity.builder()
                .id(10L)
                .customerId(1L)
                .productId(100L)
                .quantity(2)
                .totalPrice(new BigDecimal("250.00"))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDate.now().atStartOfDay())
                .build();
    }

    @Test
    @DisplayName("Sipariş oluşturulduğunda DB'ye PENDING statüsüyle kaydedilmeli ve Response dönmeli")
    void shouldCreateOrderSuccessfully_AndReturnOrderResponse() {
        // Given
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);

        // When
        OrderResponseDTO response = orderService.createOrder(request);

        // Then
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(request.getCustomerId(), response.getCustomerId());
        assertEquals(request.getTotalPrice(), response.getTotalPrice());

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());

        OrderEntity capturedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.PENDING, capturedOrder.getStatus());
    }

    @Test
    @DisplayName("ID ile sipariş arandığında kayıt bulunursa Response dönmeli")
    void shouldFindOrderById_WhenOrderExists() {
        // Given
        Long orderId = 10L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(savedOrder));

        // When
        OrderResponseDTO response = orderService.getOrderById(orderId);

        // Then
        assertNotNull(response);
        assertEquals(orderId, response.getId());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("ID ile sipariş arandığında kayıt bulunamazsa OrderNotFoundException fırlatmalı")
    void shouldThrowOrderNotFoundException_WhenOrderDoesNotExist() {
        // Given
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderById(orderId);
        });

        assertTrue(exception.getMessage().contains("Sipariş bulunamadı"));
        verify(orderRepository, times(1)).findById(orderId);
    }
}
