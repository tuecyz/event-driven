package org.example.orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.orderservice.dto.CreateOrderRequestDTO;
import org.example.orderservice.dto.OrderResponseDTO;
import org.example.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody CreateOrderRequestDTO request) {

        OrderResponseDTO response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {

        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long id) {

        return ResponseEntity.ok(orderService.getOrderById(id));
    }
}