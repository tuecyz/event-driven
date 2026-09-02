package org.example.orderservice.service;

import org.example.orderservice.dto.CreateOrderRequestDTO;
import org.example.orderservice.dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {

    OrderResponseDTO createOrder(CreateOrderRequestDTO request);

    List<OrderResponseDTO> getAllOrders();

    OrderResponseDTO getOrderById(Long id);
}
