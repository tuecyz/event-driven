package org.example.orderservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull(message = "Müşteri ID boş olamaz.")
    private Long customerId;

    @NotNull(message = "Ürün ID boş olamaz.")
    private Long productId;

    @NotNull(message = "Ürün adedi boş olamaz.")
    @Min(value = 1, message = "Ürün adedi en az 1 olmalıdır.")
    private Integer quantity;

    @NotNull(message = "Toplam fiyat boş olamaz.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Toplam fiyat 0'dan büyük olmalıdır.")
    private BigDecimal totalPrice;
}
