package org.example.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.orderservice.dto.CreateOrderRequestDTO;
import org.example.orderservice.dto.OrderResponseDTO;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.exception.OrderNotFoundException;
import org.example.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private OrderService orderService;

    private CreateOrderRequestDTO validRequest;
    private OrderResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = new CreateOrderRequestDTO();
        validRequest.setCustomerId(1L);
        validRequest.setProductId(100L);
        validRequest.setQuantity(3);
        validRequest.setTotalPrice(new BigDecimal("450.00"));

        sampleResponse = OrderResponseDTO.builder()
                .id(10L)
                .customerId(1L)
                .productId(100L)
                .quantity(3)
                .totalPrice(new BigDecimal("450.00"))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDate.now())
                .build();
    }

    // ==========================================
    // 1. POST /orders - Başarılı Senaryo
    // ==========================================
    @Test
    @DisplayName("POST /api/orders - Geçerli istek ile 201 Created ve sipariş bilgisi dönmeli")
    void shouldCreateOrder_WhenRequestIsValid() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    // ==========================================
    // 2. POST /orders - Validation Testleri
    // ==========================================
    @Test
    @DisplayName("POST /api/orders - Eksik veya geçersiz alanlar ile 400 Bad Request dönmeli")
    void shouldReturnBadRequest_WhenValidationFails() throws Exception {
        CreateOrderRequestDTO invalidRequest = new CreateOrderRequestDTO();
        invalidRequest.setCustomerId(null);
        invalidRequest.setProductId(100L);
        invalidRequest.setQuantity(0);
        invalidRequest.setTotalPrice(new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.customerId").exists())
                .andExpect(jsonPath("$.validationErrors.quantity").exists())
                .andExpect(jsonPath("$.validationErrors.totalPrice").exists());
    }

    // ==========================================
    // 3. GET /orders - Tüm Siparişleri Listeleme
    // ==========================================
    @Test
    @DisplayName("GET /api/orders - Tüm siparişleri 200 OK ile liste olarak dönmeli")
    void shouldGetAllOrders() throws Exception {
        List<OrderResponseDTO> ordersList = Collections.singletonList(sampleResponse);
        when(orderService.getAllOrders()).thenReturn(ordersList);

        mockMvc.perform(get("/api/orders")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    // ==========================================
    // 4. GET /orders/{id} - Başarılı Senaryo
    // ==========================================
    @Test
    @DisplayName("GET /api/orders/{id} - Kayıt mevcutsa 200 OK ve siparişi dönmeli")
    void shouldGetOrderById_WhenOrderExists() throws Exception {
        Long orderId = 10L;
        when(orderService.getOrderById(orderId)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ==========================================
    // 5. GET /orders/{id} - 404 Bulunamadı Testi
    // ==========================================
    @Test
    @DisplayName("GET /api/orders/{id} - Kayıt yoksa GlobalExceptionHandler üzerinden 404 Not Found dönmeli")
    void shouldReturn404_WhenOrderDoesNotExist() throws Exception {
        Long nonExistingId = 999L;
        when(orderService.getOrderById(nonExistingId))
                .thenThrow(new OrderNotFoundException("Sipariş bulunamadı. ID: " + nonExistingId));

        mockMvc.perform(get("/api/orders/{id}", nonExistingId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()) // 404 Durum Kodu doğrulaması
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Sipariş bulunamadı. ID: " + nonExistingId));
    }
}
